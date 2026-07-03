package com.ticklog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticklog.domain.usecase.ObserveUserPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Activity-scoped ViewModel that resolves the global app state (theme + start
 * destination) from persisted preferences.
 *
 * Exposes [MainUiState.Loading] until the first preferences value arrives, which
 * lets [MainActivity] hold a neutral frame and avoid a theme flash or briefly
 * showing the wrong start screen.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeUserPreferences: ObserveUserPreferencesUseCase,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> =
        observeUserPreferences()
            .map { prefs ->
                MainUiState.Ready(
                    themeMode = prefs.themeMode,
                    onboardingCompleted = prefs.onboardingCompleted,
                    animationsEnabled = prefs.animationsEnabled,
                    weekStart = prefs.weekStart,
                    dateFormat = prefs.dateFormat,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = MainUiState.Loading,
            )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
