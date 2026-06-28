package com.ticklog.ui.feature_onboarding

import androidx.lifecycle.ViewModel
import com.ticklog.domain.model.DateRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import javax.inject.Inject

/**
 * Drives the first onboarding step: choosing the tracking date range.
 *
 * In Phase 2 this step no longer finishes onboarding — it merely validates the
 * range and hands it to the second step (the checklist builder), which performs
 * the actual creation. The chosen range is emitted as a one-shot event so the
 * navigation fires exactly once.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // One-shot "proceed to builder" signal carrying the validated range.
    private val proceedChannel = Channel<DateRange>(Channel.BUFFERED)

    /** Emits once with the chosen range when the user advances to the next step. */
    val proceed: Flow<DateRange> = proceedChannel.receiveAsFlow()

    /** Records the chosen start date. */
    fun onStartDateSelected(date: LocalDate) {
        _uiState.update { it.copy(startDate = date) }
    }

    /** Records the chosen end date. */
    fun onEndDateSelected(date: LocalDate) {
        _uiState.update { it.copy(endDate = date) }
    }

    /**
     * Advances to the checklist builder if a valid range is selected. The button
     * is only enabled when [OnboardingUiState.canContinue] is true, so this is a
     * guarded, deterministic transition.
     */
    fun onContinueClicked() {
        val state = _uiState.value
        val start = state.startDate ?: return
        val end = state.endDate ?: return
        if (!state.canContinue) return
        proceedChannel.trySend(DateRange(start = start, end = end))
    }
}
