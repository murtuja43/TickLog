package com.ticklog.ui.feature_settings

import androidx.compose.runtime.Immutable
import com.ticklog.domain.model.ThemeMode

/**
 * Immutable UI state for the Settings screen.
 *
 * Marked [Immutable] so Compose can confidently skip recomposition when the
 * reference is unchanged. The screen renders purely from this snapshot and
 * raises intent through callbacks — no business logic lives in the composable.
 *
 * @property themeMode the user's currently selected theme.
 * @property appVersion human-readable version string for the About section.
 */
@Immutable
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appVersion: String = "",
)
