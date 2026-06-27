package com.ticklog.domain.model

import java.time.LocalDate

/**
 * The user's selected app theme.
 *
 * [SYSTEM] defers to the device's light/dark setting; the others force a mode.
 * Modelled as a domain enum (rather than a raw string) so the rest of the app
 * reasons about a closed, type-safe set of options.
 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * Lightweight, UI-facing user preferences backed by DataStore.
 *
 * These are the few flags read on the app's hot path — the chosen theme, whether
 * onboarding is done, and the date range picked during onboarding. Heavier,
 * relational configuration lives in the Room `settings` table instead.
 *
 * @property onboardingCompleted whether the user has finished onboarding.
 * @property themeMode the user's theme choice.
 * @property scheduleStartDate first date of the tracking range, if chosen.
 * @property scheduleEndDate last date of the tracking range, if chosen.
 */
data class UserPreferences(
    val onboardingCompleted: Boolean,
    val themeMode: ThemeMode,
    val scheduleStartDate: LocalDate?,
    val scheduleEndDate: LocalDate?,
) {
    companion object {
        /** Sensible defaults for a brand-new install before anything is stored. */
        val DEFAULT = UserPreferences(
            onboardingCompleted = false,
            themeMode = ThemeMode.SYSTEM,
            scheduleStartDate = null,
            scheduleEndDate = null,
        )
    }
}

/**
 * An inclusive range of calendar dates.
 *
 * Encapsulating the range as a value object lets us attach validation and
 * derived values (like [lengthInDays]) in one place instead of passing loose
 * start/end pairs around.
 *
 * @property start the first day (inclusive).
 * @property end the last day (inclusive).
 */
data class DateRange(
    val start: LocalDate,
    val end: LocalDate,
) {
    /** True when [end] is on or after [start] — the only validity rule. */
    val isValid: Boolean get() = !end.isBefore(start)

    /** Number of days in the range, inclusive of both ends (>= 1 when valid). */
    val lengthInDays: Long
        get() = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1
}
