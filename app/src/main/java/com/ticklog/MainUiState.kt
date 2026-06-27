package com.ticklog

import com.ticklog.domain.model.ThemeMode

/**
 * Top-level UI state that decides what the very first frame shows.
 *
 * The app must read the persisted theme and onboarding flag before it can render
 * anything correctly (to avoid a theme flash or showing the wrong start screen),
 * so the entry point models an explicit [Loading] phase distinct from [Ready].
 */
sealed interface MainUiState {

    /** Preferences are still being read; show a neutral loading frame. */
    data object Loading : MainUiState

    /**
     * Preferences resolved and the app can render.
     *
     * @property themeMode the user's theme choice.
     * @property onboardingCompleted whether to start at Home (vs. Onboarding).
     */
    data class Ready(
        val themeMode: ThemeMode,
        val onboardingCompleted: Boolean,
    ) : MainUiState
}
