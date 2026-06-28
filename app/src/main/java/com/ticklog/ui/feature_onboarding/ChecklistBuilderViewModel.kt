package com.ticklog.ui.feature_onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticklog.core.navigation.ARG_END
import com.ticklog.core.navigation.ARG_START
import com.ticklog.domain.model.DateRange
import com.ticklog.domain.model.TaskDraft
import com.ticklog.domain.usecase.CreateChecklistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * Drives onboarding step 2 — assembling checklist items and creating everything.
 *
 * The chosen range arrives as epoch-day navigation arguments (read from
 * [SavedStateHandle]), so the step is fully restorable. Item editing is pure
 * in-memory list manipulation keyed by a builder-local id; only "Create My
 * Checklist" touches storage, via [CreateChecklistUseCase].
 */
@HiltViewModel
class ChecklistBuilderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val createChecklist: CreateChecklistUseCase,
) : ViewModel() {

    /** The range chosen in step 1; exposed so the screen can show it. */
    val dateRange: DateRange = run {
        val startEpochDay = savedStateHandle.get<Long>(ARG_START) ?: 0L
        val endEpochDay = savedStateHandle.get<Long>(ARG_END) ?: 0L
        DateRange(
            start = LocalDate.ofEpochDay(startEpochDay),
            end = LocalDate.ofEpochDay(endEpochDay),
        )
    }

    private var nextItemId = 1L

    private val _uiState = MutableStateFlow(ChecklistBuilderUiState())
    val uiState: StateFlow<ChecklistBuilderUiState> = _uiState.asStateFlow()

    private val createdChannel = Channel<Unit>(Channel.BUFFERED)

    /** Emits once when the checklist has been created and the app should advance. */
    val created: Flow<Unit> = createdChannel.receiveAsFlow()

    /** Appends a new item; ignored if the title is blank. */
    fun addItem(title: String, note: String?) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val item = BuilderItem(
            id = nextItemId++,
            title = cleanTitle,
            note = note?.trim()?.takeIf { it.isNotEmpty() },
        )
        _uiState.update { it.copy(items = it.items + item) }
    }

    /** Updates the title/note of the item with [id]. */
    fun updateItem(id: Long, title: String, note: String?) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        _uiState.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.id == id) {
                        item.copy(
                            title = cleanTitle,
                            note = note?.trim()?.takeIf { it.isNotEmpty() },
                        )
                    } else {
                        item
                    }
                },
            )
        }
    }

    /** Removes the item with [id]. */
    fun deleteItem(id: Long) {
        _uiState.update { state -> state.copy(items = state.items.filterNot { it.id == id }) }
    }

    /** Inserts a copy of the item with [id] directly after it. */
    fun duplicateItem(id: Long) {
        _uiState.update { state ->
            val index = state.items.indexOfFirst { it.id == id }
            if (index == -1) return@update state
            val copy = state.items[index].copy(id = nextItemId++)
            state.copy(items = state.items.toMutableList().apply { add(index + 1, copy) })
        }
    }

    /** Moves the item with [id] one position earlier, if possible. */
    fun moveUp(id: Long) = swap(id, -1)

    /** Moves the item with [id] one position later, if possible. */
    fun moveDown(id: Long) = swap(id, +1)

    /** Validates and creates the checklist, then signals completion on success. */
    fun onCreateClicked() {
        val drafts = _uiState.value.items.mapNotNull { TaskDraft.from(it.title, it.note) }
        if (drafts.isEmpty()) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = createChecklist(dateRange, drafts)
            if (result.isSuccess) {
                createdChannel.send(Unit)
            } else {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    /** Swaps the item with [id] with its neighbour [offset] positions away. */
    private fun swap(id: Long, offset: Int) {
        _uiState.update { state ->
            val index = state.items.indexOfFirst { it.id == id }
            val target = index + offset
            if (index == -1 || target !in state.items.indices) return@update state
            state.copy(
                items = state.items.toMutableList().apply {
                    val moved = removeAt(index)
                    add(target, moved)
                },
            )
        }
    }
}
