package com.ticklog.core.navigation

import java.time.LocalDate

/** Navigation argument key: the range start, as an epoch day. */
const val ARG_START: String = "startEpochDay"

/** Navigation argument key: the range end, as an epoch day. */
const val ARG_END: String = "endEpochDay"

/** Navigation argument key: an optional target day for Home, as an epoch day. */
const val ARG_DATE: String = "date"

/** Sentinel meaning "no explicit date — open today". */
const val HOME_NO_DATE: Long = Long.MIN_VALUE

/**
 * The complete set of navigation destinations in the app.
 *
 * Routes are declared in one sealed hierarchy so they are exhaustive,
 * refactor-safe and impossible to mistype at call sites. Every screen the
 * product will ever expose (current and planned) has a route reserved here,
 * which makes adding a destination a one-line change.
 *
 * @property route the unique route string used by Navigation Compose.
 */
sealed class TickDestination(val route: String) {

    /** Onboarding step 1: choosing the tracking date range. */
    data object Onboarding : TickDestination("onboarding")

    /**
     * Onboarding step 2: the checklist builder, parameterised by the chosen
     * range (passed as epoch-day arguments so the step is fully restorable).
     */
    data object OnboardingItems : TickDestination("onboarding_items/{$ARG_START}/{$ARG_END}") {
        /** Builds the concrete route for a specific [start]/[end] range. */
        fun routeFor(start: LocalDate, end: LocalDate): String =
            "onboarding_items/${start.toEpochDay()}/${end.toEpochDay()}"
    }

    /**
     * The primary daily screen. Accepts an optional [ARG_DATE] so other screens
     * (e.g. the calendar) can open a specific day; omitting it opens today.
     */
    data object Home : TickDestination("home?$ARG_DATE={$ARG_DATE}") {
        /** Navigate here for today. */
        const val DEFAULT: String = "home"

        /** Navigate here to open a specific [date]. */
        fun routeForDate(date: LocalDate): String = "home?$ARG_DATE=${date.toEpochDay()}"
    }

    /** Month/range calendar overview (Phase 2). */
    data object Calendar : TickDestination("calendar")

    /** Archived, completed days (Phase 2). */
    data object History : TickDestination("history")

    /** Streaks and completion trends (Phase 2). */
    data object Statistics : TickDestination("statistics")

    /** App settings, including theme. */
    data object Settings : TickDestination("settings")

    /** Export history to a shareable PDF. */
    data object PdfExport : TickDestination("pdf_export")

    /** Offline backup and restore. */
    data object Backup : TickDestination("backup")

    /** Open-source licences. */
    data object Licenses : TickDestination("licenses")
}
