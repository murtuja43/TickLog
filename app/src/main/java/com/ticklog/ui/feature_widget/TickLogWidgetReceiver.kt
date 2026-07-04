package com.ticklog.ui.feature_widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * The manifest-registered receiver that hosts [TickLogWidget].
 *
 * Beyond the standard app-widget lifecycle (which the Glance base class handles),
 * it also listens for the system's midnight/clock broadcasts so the widget rolls
 * over to the new day on its own — "today" must never go stale on the home
 * screen. Each of those broadcasts arrives as its own `onReceive` call, so the
 * refresh path and Glance's own handling never contend for the same async token.
 */
class TickLogWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = TickLogWidget()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action in DAY_ROLLOVER_ACTIONS) {
            val pending = goAsync()
            scope.launch {
                try {
                    glanceAppWidget.updateAll(context)
                } finally {
                    pending.finish()
                }
            }
        } else {
            super.onReceive(context, intent)
        }
    }

    private companion object {
        val DAY_ROLLOVER_ACTIONS = setOf(
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
