package com.ticklog.ui.feature_settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticklog.BuildConfig
import com.ticklog.domain.model.ThemeMode
import com.ticklog.domain.usecase.ObserveUserPreferencesUseCase
import com.ticklog.domain.usecase.SetThemeModeUseCase
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
 * It maps the user-preferences stream into [SettingsUiState] and exposes a
 * single intent — selecting a theme. All threading and persistence is delegated
 * to the injected use cases, keeping this class focused on state orchestration.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeUserPreferences: ObserveUserPreferencesUseCase,
    private val setThemeMode: SetThemeModeUseCase,
) : ViewModel() {

    /** Reactive UI state, started while the screen is subscribed. */
    val uiState: StateFlow<SettingsUiState> =
        observeUserPreferences()
            .map { prefs ->
                SettingsUiState(
                    themeMode = prefs.themeMode,
                    appVersion = BuildConfig.VERSION_NAME,
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = SettingsUiState(appVersion = BuildConfig.VERSION_NAME),
            )

    /** Persists the user's [themeMode] selection. */
    fun onThemeModeSelected(themeMode: ThemeMode) {
        viewModelScope.launch { setThemeMode(themeMode) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
