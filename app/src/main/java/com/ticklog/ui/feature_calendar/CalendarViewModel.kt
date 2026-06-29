package com.ticklog.ui.feature_calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticklog.domain.repository.ChecklistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

/**
 * Drives the Calendar screen.
 *
 * Observes the shared per-day completion summaries and indexes them by date so
 * the screen can paint any month instantly as the user swipes between months.
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    checklistRepository: ChecklistRepository,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()

    val uiState: StateFlow<CalendarUiState> =
        checklistRepository.observeDaySummaries()
            .map { records ->
                CalendarUiState(
                    isLoading = false,
                    today = today,
                    summariesByDate = records.associateBy { it.date },
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = CalendarUiState(isLoading = true, today = today),
            )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
