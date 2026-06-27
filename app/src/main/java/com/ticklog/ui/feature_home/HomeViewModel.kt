package com.ticklog.ui.feature_home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticklog.domain.repository.ChecklistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

/**
 * Drives the Home screen.
 *
 * The currently-focused [LocalDate] is the single input; the UI state is derived
 * reactively by observing that day's checklist through [ChecklistRepository].
 * This is what makes day navigation — arrows now, swipe via the pager — a matter
 * of simply changing [selectedDate], with the data following automatically.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val checklistRepository: ChecklistRepository,
) : ViewModel() {

    /** The day the app opens on. Exposed so the UI can anchor its day pager. */
    val today: LocalDate = LocalDate.now()

    private val selectedDate = MutableStateFlow(today)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> =
        selectedDate
            .flatMapLatest { date ->
                checklistRepository.observeChecklistForDate(date).map { checklist ->
                    HomeUiState(selectedDate = date, checklist = checklist, isLoading = false)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = HomeUiState(selectedDate = today, isLoading = true),
            )

    /** Focuses [date], triggering observation of that day's checklist. */
    fun onDateSelected(date: LocalDate) {
        selectedDate.value = date
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
