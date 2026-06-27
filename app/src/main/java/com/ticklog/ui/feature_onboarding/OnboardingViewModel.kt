package com.ticklog.ui.feature_onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticklog.domain.model.DateRange
import com.ticklog.domain.usecase.CompleteOnboardingUseCase
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
 * Drives the onboarding screen.
 *
 * Holds the in-progress date selection as [OnboardingUiState] and persists it
 * through [CompleteOnboardingUseCase]. Successful completion is signalled as a
 * one-shot event (via a [Channel]) rather than a state flag, so the navigation
 * fires exactly once and never replays on configuration change.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val completeOnboarding: CompleteOnboardingUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // One-shot navigation signal; buffered so a value is never dropped.
    private val completionChannel = Channel<Unit>(Channel.BUFFERED)

    /** Emits once when onboarding has been saved and the app should advance. */
    val onboardingCompleted: Flow<Unit> = completionChannel.receiveAsFlow()

    /** Records the chosen start date. */
    fun onStartDateSelected(date: LocalDate) {
        _uiState.update { it.copy(startDate = date) }
    }

    /** Records the chosen end date. */
    fun onEndDateSelected(date: LocalDate) {
        _uiState.update { it.copy(endDate = date) }
    }

    /**
     * Validates and persists the selected range, then signals completion.
     *
     * Guarded by [OnboardingUiState.canContinue]; the use case performs the
     * authoritative validation, so an invalid range simply clears the saving
     * flag and leaves the inline error visible.
     */
    fun onContinueClicked() {
        val state = _uiState.value
        val start = state.startDate
        val end = state.endDate
        if (start == null || end == null || !state.canContinue) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = completeOnboarding(DateRange(start = start, end = end))
            _uiState.update { it.copy(isSaving = false) }
            if (result.isSuccess) {
                completionChannel.send(Unit)
            }
        }
    }
}
