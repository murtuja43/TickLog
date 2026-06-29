package com.ticklog.ui.feature_statistics

import androidx.compose.runtime.Immutable
import com.ticklog.domain.model.Achievement
import com.ticklog.domain.model.ChecklistStatistics
import com.ticklog.domain.model.TaskInsight
import com.ticklog.domain.model.TaskInsightSort

/**
 * Immutable UI state for the Statistics dashboard.
 *
 * Chart inputs are pre-normalised to [0f, 1f] lists so the Canvas charts stay
 * dumb. Everything is derived from the live data on each emission.
 *
 * @property isLoading true until the first load resolves.
 * @property statistics the headline metrics.
 * @property weeklyCompletion last 7 days' completion rates.
 * @property monthlyCompletion last 30 days' completion rates.
 * @property completionTrend the (possibly downsampled) trend over all history.
 * @property insights per-task insights in the chosen order.
 * @property insightSort the active insight ordering.
 * @property achievements the achievement catalogue with unlock state.
 */
@Immutable
data class StatisticsUiState(
    val isLoading: Boolean,
    val statistics: ChecklistStatistics = ChecklistStatistics.EMPTY,
    val weeklyCompletion: List<Float> = emptyList(),
    val monthlyCompletion: List<Float> = emptyList(),
    val completionTrend: List<Float> = emptyList(),
    val insights: List<TaskInsight> = emptyList(),
    val insightSort: TaskInsightSort = TaskInsightSort.BEST,
    val achievements: List<Achievement> = emptyList(),
) {
    /** True once there is at least one day to report on. */
    val hasData: Boolean get() = statistics.hasData
}
