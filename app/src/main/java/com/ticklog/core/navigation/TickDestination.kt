package com.ticklog.core.navigation

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

    /** First-run experience; shown until onboarding is completed. */
    data object Onboarding : TickDestination("onboarding")

    /** The primary daily screen. */
    data object Home : TickDestination("home")

    /** Month/range calendar overview (Phase 2). */
    data object Calendar : TickDestination("calendar")

    /** Archived, completed days (Phase 2). */
    data object History : TickDestination("history")

    /** Streaks and completion trends (Phase 2). */
    data object Statistics : TickDestination("statistics")

    /** App settings, including theme. */
    data object Settings : TickDestination("settings")

    /** Export history to a shareable PDF (Phase 2). */
    data object PdfExport : TickDestination("pdf_export")
}
