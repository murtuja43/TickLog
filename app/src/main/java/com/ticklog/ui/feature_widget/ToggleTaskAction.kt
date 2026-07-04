package com.ticklog.ui.feature_widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll

/** Action parameter carrying the id of the tapped task. */
val TaskIdKey = ActionParameters.Key<Long>("ticklog.widget.taskId")

/** Action parameter carrying the target completion state to apply. */
val TargetStateKey = ActionParameters.Key<Boolean>("ticklog.widget.target")

/**
 * Toggles a task's completion directly from the widget — no app launch.
 *
 * The write goes through the same [ChecklistRepository][com.ticklog.domain.repository.ChecklistRepository]
 * the app uses, so history, streaks and every open screen stay consistent. After
 * the write we refresh **all** widget instances, because a single toggle also
 * changes the shared completion percentage and can start or break the streak.
 */
class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val taskId = parameters[TaskIdKey] ?: return
        val target = parameters[TargetStateKey] ?: return

        widgetRepository(context).setTaskCompleted(taskId, target)
        TickLogWidget().updateAll(context)
    }
}
