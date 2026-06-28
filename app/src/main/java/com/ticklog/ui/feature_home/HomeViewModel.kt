package com.ticklog.ui.feature_home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticklog.domain.model.DailyChecklist
import com.ticklog.domain.model.DailyTask
import com.ticklog.domain.model.DateRange
import com.ticklog.domain.model.DeletedTask
import com.ticklog.domain.model.TaskDraft
import com.ticklog.domain.model.TaskScope
import com.ticklog.domain.repository.ChecklistRepository
import com.ticklog.domain.usecase.ObserveUserPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Drives the Home screen and owns every checklist interaction.
 *
 * Days are observed independently: [checklistFlow] returns a cold stream per
 * date, which lets the pager render real data for each visible page. Completion
 * is optimistic — a tap is reflected instantly via an in-memory override that is
 * pruned once the database confirms it, so the checkbox never flickers and the
 * write happens in the background with no save button.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val checklistRepository: ChecklistRepository,
    observeUserPreferences: ObserveUserPreferencesUseCase,
) : ViewModel() {

    /** The day the app opens on; the pager anchors here. */
    val today: LocalDate = LocalDate.now()

    /**
     * The user's generated tracking range, or null until preferences load.
     * Exposed so the screen can decide whether "add task" is meaningful for the
     * day currently in view.
     */
    val scheduleRange: StateFlow<DateRange?> =
        observeUserPreferences()
            .map { prefs ->
                val start = prefs.scheduleStartDate
                val end = prefs.scheduleEndDate
                if (start != null && end != null) DateRange(start, end) else null
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    /** Pending optimistic completion values, keyed by task id. */
    private val optimisticCompletion = MutableStateFlow<Map<Long, Boolean>>(emptyMap())

    /** The most recent deletion, retained so it can be undone. */
    private var lastDeleted: List<DeletedTask> = emptyList()

    private val eventsChannel = Channel<HomeEvent>(Channel.BUFFERED)

    /** One-shot UI effects (e.g. the undo snackbar). */
    val events: Flow<HomeEvent> = eventsChannel.receiveAsFlow()

    /**
     * A reactive [DayUiState] stream for a single [date], combining the tracking
     * range, the day's checklist and the optimistic overlay.
     */
    fun checklistFlow(date: LocalDate): Flow<DayUiState> =
        combine(
            scheduleRange,
            checklistRepository.observeChecklistForDate(date),
            optimisticCompletion,
        ) { range, checklist, overrides ->
            Triple(range, checklist, overrides)
        }
            .onEach { (_, checklist, overrides) -> pruneConfirmedOverrides(checklist, overrides) }
            .map { (range, checklist, overrides) -> buildDayState(date, range, checklist, overrides) }

    /** Toggles a task's completion optimistically, persisting in the background. */
    fun onToggleTask(taskId: Long, currentlyCompleted: Boolean) {
        val desired = !currentlyCompleted
        optimisticCompletion.update { it + (taskId to desired) }
        viewModelScope.launch { checklistRepository.setTaskCompleted(taskId, desired) }
    }

    /** Adds a task on [date] with the chosen [scope]; no-op if the title is blank. */
    fun addTask(date: LocalDate, title: String, note: String?, scope: TaskScope) {
        val draft = TaskDraft.from(title, note) ?: return
        viewModelScope.launch { checklistRepository.addTask(date, draft, scope) }
    }

    /** Applies a title + note edit to a task with the chosen [scope]. */
    fun editTask(taskId: Long, title: String, note: String?, scope: TaskScope) {
        if (title.isBlank()) return
        viewModelScope.launch {
            checklistRepository.renameTask(taskId, title, scope)
            checklistRepository.updateTaskNote(taskId, note, scope)
        }
    }

    /** Duplicates a task on its own day. */
    fun duplicateTask(taskId: Long) {
        viewModelScope.launch { checklistRepository.duplicateTask(taskId) }
    }

    /** Deletes a task with the chosen [scope] and offers an undo. */
    fun deleteTask(taskId: Long, scope: TaskScope) {
        viewModelScope.launch {
            val removed = checklistRepository.deleteTask(taskId, scope)
            if (removed.isNotEmpty()) {
                lastDeleted = removed
                eventsChannel.send(HomeEvent.TasksDeleted(removed.size))
            }
        }
    }

    /** Restores the most recently deleted tasks. */
    fun undoDelete() {
        val toRestore = lastDeleted
        lastDeleted = emptyList()
        if (toRestore.isEmpty()) return
        viewModelScope.launch { checklistRepository.restoreTasks(toRestore) }
    }

    /** Drops overrides the database has now caught up to, keeping the overlay tidy. */
    private fun pruneConfirmedOverrides(
        checklist: DailyChecklist?,
        overrides: Map<Long, Boolean>,
    ) {
        if (checklist == null || overrides.isEmpty()) return
        val confirmed = checklist.tasks
            .filter { overrides[it.id] == it.isCompleted }
            .map { it.id }
            .toSet()
        if (confirmed.isNotEmpty()) {
            optimisticCompletion.update { it - confirmed }
        }
    }

    /** Resolves the [DayUiState] for [date] from its inputs. */
    private fun buildDayState(
        date: LocalDate,
        range: DateRange?,
        checklist: DailyChecklist?,
        overrides: Map<Long, Boolean>,
    ): DayUiState = when {
        range == null || date !in range -> DayUiState.OutOfRange(date)
        checklist == null || checklist.tasks.isEmpty() -> DayUiState.Empty(date)
        else -> DayUiState.Content(date, checklist.tasks.map { it.withOverride(overrides) })
    }

    /** Applies any pending optimistic completion value to a task. */
    private fun DailyTask.withOverride(overrides: Map<Long, Boolean>): DailyTask =
        overrides[id]?.let { copy(isCompleted = it) } ?: this

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
