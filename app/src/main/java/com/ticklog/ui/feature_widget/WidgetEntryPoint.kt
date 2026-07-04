package com.ticklog.ui.feature_widget

import com.ticklog.domain.repository.ChecklistRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt bridge for the home-screen widget.
 *
 * A [androidx.glance.appwidget.GlanceAppWidget] and its [androidx.glance.appwidget.action.ActionCallback]s
 * are instantiated by the framework, not by Hilt, so they cannot use `@Inject`
 * constructors. This [EntryPoint] lets those classes pull the singleton
 * [ChecklistRepository] out of the application graph on demand — the same
 * instance the rest of the app uses, keeping the widget perfectly in sync.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun checklistRepository(): ChecklistRepository
}
