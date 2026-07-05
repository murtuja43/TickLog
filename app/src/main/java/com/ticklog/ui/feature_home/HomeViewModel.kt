package com.ticklog.ui.feature_home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticklog.core.navigation.ARG_DATE
import com.ticklog.core.navigation.HOME_NO_DATE
import com.ticklog.domain.model.DailyChecklist
import com.ticklog.domain.model.DailyTask
import com.ticklog.domain.model.DateRange
import com.ticklog.domain.model.DeletedTask
import com.ticklog.domain.model.TaskDraft
import com.ticklog.domain.model.TaskScope
import com.ticklog.domain.repository.ChecklistRepository
import com.ticklog.domain.usecase.ObserveUserPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** The day the pager is anchored on (the real "today"). */
    val today: LocalDate = LocalDate.now()

    /**
     * The day to open on first composition. Defaults to [today], but the calendar
     * can deep-link to any date via the [ARG_DATE] navigation argument.
     */
    val initialDate: LocalDate = savedStateHandle.get<Long>(ARG_DATE)
        ?.takeIf { it != HOME_NO_DATE }
        ?.let { LocalDate.ofEpochDay(it) }
        ?: today

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

    /**
     * Toggles a task's completion optimistically, persisting in the background.
     * If the write fails the optimistic overlay is removed — the checkbox snaps
     * back to the stored state — and the failure is surfaced.
     */
    fun onToggleTask(taskId: Long, currentlyCompleted: Boolean) {
        val desired = !currentlyCompleted
        optimisticCompletion.update { it + (taskId to desired) }
        viewModelScope.launch {
            try {
                checklistRepository.setTaskCompleted(taskId, desired)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Roll the optimistic UI back to whatever the database holds.
                optimisticCompletion.update { it - taskId }
                eventsChannel.send(HomeEvent.ActionFailed)
            }
        }
    }

    /** Adds a task on [date] with the chosen [scope]; no-op if the title is blank. */
    fun addTask(date: LocalDate, title: String, note: String?, scope: TaskScope) {
        val draft = TaskDraft.from(title, note) ?: return
        launchWrite { checklistRepository.addTask(date, draft, scope) }
    }

    /** Applies a title + note edit to a task with the chosen [scope]. */
    fun editTask(taskId: Long, title: String, note: String?, scope: TaskScope) {
        if (title.isBlank()) return
        launchWrite {
            checklistRepository.renameTask(taskId, title, scope)
            checklistRepository.updateTaskNote(taskId, note, scope)
        }
    }

    /** Duplicates a task on its own day. */
    fun duplicateTask(taskId: Long) {
        launchWrite { checklistRepository.duplicateTask(taskId) }
    }

    /** Deletes a task with the chosen [scope] and offers an undo. */
    fun deleteTask(taskId: Long, scope: TaskScope) {
        launchWrite {
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
        launchWrite { checklistRepository.restoreTasks(toRestore) }
    }

    /**
     * Runs a repository write, catching persistence failures (e.g. SQLite or IO
     * errors) so they surface a message instead of crashing the app. The UI is
     * driven by the database flow, so on failure it simply stays on the last
     * persisted state.
     */
    private fun launchWrite(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                eventsChannel.send(HomeEvent.ActionFailed)
            }
        }
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
