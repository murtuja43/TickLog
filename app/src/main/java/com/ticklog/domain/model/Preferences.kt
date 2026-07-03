package com.ticklog.domain.model

import java.time.DayOfWeek
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
 * How calendar dates are formatted across the app and in exports.
 *
 * [SYSTEM] uses the device's localized medium format; the others pin an explicit
 * pattern. Modelling this as an enum (with its [pattern]) keeps formatting
 * consistent everywhere from a single user choice.
 */
enum class DateFormat(val pattern: String?) {
    /** Device-localized medium date. */
    SYSTEM(null),

    /** e.g. "27 Jun 2026". */
    DAY_MONTH_YEAR("d MMM yyyy"),

    /** e.g. "Jun 27, 2026". */
    MONTH_DAY_YEAR("MMM d, yyyy"),

    /** e.g. "2026-06-27". */
    ISO("yyyy-MM-dd"),
}

/** The weekday a calendar week starts on. */
enum class WeekStart(val dayOfWeek: DayOfWeek) {
    SUNDAY(DayOfWeek.SUNDAY),
    MONDAY(DayOfWeek.MONDAY),
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
 * @property dateFormat how dates are displayed and exported.
 * @property weekStart the weekday calendars begin on.
 * @property animationsEnabled whether non-essential motion is enabled.
 * @property scheduleStartDate first date of the tracking range, if chosen.
 * @property scheduleEndDate last date of the tracking range, if chosen.
 */
data class UserPreferences(
    val onboardingCompleted: Boolean,
    val themeMode: ThemeMode,
    val dateFormat: DateFormat,
    val weekStart: WeekStart,
    val animationsEnabled: Boolean,
    val scheduleStartDate: LocalDate?,
    val scheduleEndDate: LocalDate?,
) {
    companion object {
        /** Sensible defaults for a brand-new install before anything is stored. */
        val DEFAULT = UserPreferences(
            onboardingCompleted = false,
            themeMode = ThemeMode.SYSTEM,
            dateFormat = DateFormat.SYSTEM,
            weekStart = WeekStart.MONDAY,
            animationsEnabled = true,
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

    /** True when [date] falls within the inclusive range. */
    operator fun contains(date: LocalDate): Boolean =
        !date.isBefore(start) && !date.isAfter(end)

    /**
     * Every calendar date in the range, in chronological order (inclusive of both
     * ends). Returns an empty list for an invalid range. This is the pure basis
     * of day generation and is exhaustively unit-tested.
     */
    fun datesInclusive(): List<LocalDate> {
        if (!isValid) return emptyList()
        return generateSequence(start) { current ->
            if (current.isBefore(end)) current.plusDays(1) else null
        }.toList()
    }
}
