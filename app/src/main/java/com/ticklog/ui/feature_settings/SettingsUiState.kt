package com.ticklog.ui.feature_settings

import androidx.compose.runtime.Immutable
import com.ticklog.domain.model.DateFormat
import com.ticklog.domain.model.ThemeMode
import com.ticklog.domain.model.WeekStart

/**
 * Immutable UI state for the Settings screen.
 *
 * Renders purely from this snapshot; every change is raised through a callback.
 *
 * @property themeMode selected theme.
 * @property dateFormat selected date format.
 * @property weekStart selected first weekday.
 * @property animationsEnabled whether non-essential motion is enabled.
 * @property appVersion human-readable version string for the About section.
 */
@Immutable
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dateFormat: DateFormat = DateFormat.SYSTEM,
    val weekStart: WeekStart = WeekStart.MONDAY,
    val animationsEnabled: Boolean = true,
    val appVersion: String = "",
)
