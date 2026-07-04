package com.ticklog.ui.feature_settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticklog.BuildConfig
import com.ticklog.domain.model.DateFormat
import com.ticklog.domain.model.ThemeMode
import com.ticklog.domain.model.WeekStart
import com.ticklog.domain.repository.PreferencesRepository
import com.ticklog.domain.usecase.ObserveUserPreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Settings screen.
 *
 * Maps the user-preferences stream into [SettingsUiState] and forwards every
 * change to the [PreferencesRepository]. Persistence is fire-and-forget; the UI
 * updates reactively from the preferences flow, so there is no local echo state.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeUserPreferences: ObserveUserPreferencesUseCase,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> =
        observeUserPreferences()
            .map { prefs ->
                SettingsUiState(
                    themeMode = prefs.themeMode,
                    dateFormat = prefs.dateFormat,
                    weekStart = prefs.weekStart,
                    animationsEnabled = prefs.animationsEnabled,
                    includeNotesInExport = prefs.includeNotesInExport,
                    appVersion = BuildConfig.VERSION_NAME,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = SettingsUiState(appVersion = BuildConfig.VERSION_NAME),
            )

    fun onThemeModeSelected(themeMode: ThemeMode) =
        launchPref { preferencesRepository.setThemeMode(themeMode) }

    fun onDateFormatSelected(dateFormat: DateFormat) =
        launchPref { preferencesRepository.setDateFormat(dateFormat) }

    fun onWeekStartSelected(weekStart: WeekStart) =
        launchPref { preferencesRepository.setWeekStart(weekStart) }

    fun onAnimationsToggled(enabled: Boolean) =
        launchPref { preferencesRepository.setAnimationsEnabled(enabled) }

    fun onIncludeNotesToggled(enabled: Boolean) =
        launchPref { preferencesRepository.setIncludeNotesInExport(enabled) }

    fun onResetOnboarding() =
        launchPref { preferencesRepository.resetOnboarding() }

    private fun launchPref(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
