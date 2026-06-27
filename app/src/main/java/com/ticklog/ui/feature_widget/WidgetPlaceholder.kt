package com.ticklog.ui.feature_widget

/**
 * Reserved package for the home-screen widget.
 *
 * A glanceable "today's progress" widget is planned for a later phase, built
 * with Jetpack Glance (`androidx.glance:glance-appwidget`). It is intentionally
 * **not** implemented in Phase 1 — this marker exists only to establish the
 * package in the agreed architecture so the widget can be dropped in without
 * restructuring.
 *
 * When implemented, this package will contain:
 *  - a `GlanceAppWidget` rendering the day's completion ratio,
 *  - a `GlanceAppWidgetReceiver` registered in the manifest, and
 *  - a Hilt entry point to read the active day's [com.ticklog.domain.model.DailyChecklist].
 */
internal object WidgetPlaceholder
