package com.ticklog.ui.feature_statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ticklog.R
import com.ticklog.core.designsystem.component.BarChart
import com.ticklog.core.designsystem.component.EmptyState
import com.ticklog.core.designsystem.component.LineChart
import com.ticklog.core.designsystem.component.LoadingIndicator
import com.ticklog.core.designsystem.component.ProgressRing
import com.ticklog.core.designsystem.component.StatCard
import com.ticklog.core.designsystem.component.TickCard
import com.ticklog.core.designsystem.component.TickTopAppBar
import com.ticklog.core.designsystem.theme.TickLogTheme
import com.ticklog.domain.model.Achievement
import com.ticklog.domain.model.AchievementType
import com.ticklog.domain.model.ChecklistStatistics
import com.ticklog.domain.model.TaskInsight
import com.ticklog.domain.model.TaskInsightSort
import com.ticklog.util.DateTimeFormatters
import kotlin.math.roundToInt

/**
 * Statistics destination — a professional, monochrome dashboard.
 *
 * Headline metrics, Canvas charts (weekly/monthly bars and a trend line),
 * per-task insights and achievement badges are all derived from the live data.
 * The whole dashboard is one [LazyColumn] so the insight list scales and only
 * visible content is composed.
 *
 * @param onNavigateUp invoked when the user taps the back arrow.
 * @param modifier external layout modifier.
 */
@Composable
fun StatisticsScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TickTopAppBar(
                title = stringResource(R.string.destination_statistics),
                onNavigateUp = onNavigateUp,
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingIndicator(modifier = Modifier.padding(innerPadding))

            !uiState.hasData -> EmptyState(
                icon = Icons.Outlined.Insights,
                title = stringResource(R.string.statistics_empty_title),
                subtitle = stringResource(R.string.statistics_empty_subtitle),
                modifier = Modifier.padding(innerPadding),
            )

            else -> StatisticsContent(
                uiState = uiState,
                onInsightSortChange = viewModel::onInsightSortChange,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun StatisticsContent(
    uiState: StatisticsUiState,
    onInsightSortChange: (TaskInsightSort) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(TickLogTheme.spacing.large),
        verticalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.large),
    ) {
        item(key = "overall") { OverallCard(uiState.statistics) }
        item(key = "grid") { StatGrid(uiState.statistics) }
        item(key = "charts") { ChartsSection(uiState) }
        item(key = "achievements") { AchievementsSection(uiState.achievements) }
        item(key = "insights-header") {
            InsightsHeader(sort = uiState.insightSort, onSortChange = onInsightSortChange)
        }
        items(items = uiState.insights, key = { it.sourceItemId }) { insight ->
            InsightRow(insight)
        }
    }
}

/** Hero card: the overall completion ring plus the average daily rate. */
@Composable
private fun OverallCard(statistics: ChecklistStatistics) {
    TickCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = statistics.overallCompletionRate,
                strokeWidth = 8.dp,
                modifier = Modifier.size(96.dp),
            ) {
                Text(
                    text = formatPercent(statistics.overallCompletionRate),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = TickLogTheme.spacing.large),
            ) {
                Text(
                    text = stringResource(R.string.stat_overall_completion),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.stat_average_daily_value,
                        formatPercent(statistics.averageDailyCompletion),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The 2-column grid of headline metric cards. */
@Composable
private fun StatGrid(statistics: ChecklistStatistics) {
    val cells = listOf(
        stringResource(R.string.stat_current_streak) to statistics.currentStreak.toString(),
        stringResource(R.string.stat_longest_streak) to statistics.longestStreak.toString(),
        stringResource(R.string.stat_total_completed) to statistics.totalCompletedTasks.toString(),
        stringResource(R.string.stat_total_incomplete) to statistics.totalIncompleteTasks.toString(),
        stringResource(R.string.stat_total_days) to statistics.totalChecklistDays.toString(),
        stringResource(R.string.stat_this_week) to statistics.completedThisWeek.toString(),
        stringResource(R.string.stat_this_month) to statistics.completedThisMonth.toString(),
        stringResource(R.string.stat_this_year) to statistics.completedThisYear.toString(),
    )
    Column(verticalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.medium)) {
        cells.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.medium)) {
                row.forEach { (label, value) ->
                    StatCard(label = label, value = value, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** The three Canvas charts, each titled. */
@Composable
private fun ChartsSection(uiState: StatisticsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.large)) {
        ChartCard(title = stringResource(R.string.stat_chart_weekly)) {
            BarChart(values = uiState.weeklyCompletion, modifier = Modifier.fillMaxWidth().height(120.dp))
        }
        ChartCard(title = stringResource(R.string.stat_chart_monthly)) {
            BarChart(values = uiState.monthlyCompletion, modifier = Modifier.fillMaxWidth().height(120.dp))
        }
        ChartCard(title = stringResource(R.string.stat_chart_trend)) {
            LineChart(values = uiState.completionTrend, modifier = Modifier.fillMaxWidth().height(120.dp))
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    TickCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = TickLogTheme.spacing.medium),
        )
        content()
    }
}

/** Achievement badges, rendered three per row. */
@Composable
private fun AchievementsSection(achievements: List<Achievement>) {
    Column(verticalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.medium)) {
        Text(
            text = stringResource(R.string.stat_achievements),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        achievements.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.medium)) {
                row.forEach { achievement ->
                    AchievementBadge(achievement, modifier = Modifier.weight(1f))
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AchievementBadge(achievement: Achievement, modifier: Modifier = Modifier) {
    val unlocked = achievement.unlocked
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.small),
    ) {
        Surface(
            shape = CircleShape,
            color = if (unlocked) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = achievement.type.icon(),
                    contentDescription = null,
                    tint = if (unlocked) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Text(
            text = achievement.type.label(),
            style = MaterialTheme.typography.labelSmall,
            color = if (unlocked) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
        )
    }
}

/** Task-insight section header with sort chips. */
@Composable
private fun InsightsHeader(sort: TaskInsightSort, onSortChange: (TaskInsightSort) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.small)) {
        Text(
            text = stringResource(R.string.stat_task_insights),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.small)) {
            SortChip(stringResource(R.string.stat_sort_best), sort == TaskInsightSort.BEST) {
                onSortChange(TaskInsightSort.BEST)
            }
            SortChip(stringResource(R.string.stat_sort_worst), sort == TaskInsightSort.WORST) {
                onSortChange(TaskInsightSort.WORST)
            }
            SortChip(stringResource(R.string.stat_sort_alphabetical), sort == TaskInsightSort.ALPHABETICAL) {
                onSortChange(TaskInsightSort.ALPHABETICAL)
            }
        }
    }
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

/** A single task's insight row. */
@Composable
private fun InsightRow(insight: TaskInsight) {
    TickCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = insight.completionRate,
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp),
            ) {
                Text(
                    text = formatPercent(insight.completionRate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = TickLogTheme.spacing.medium),
            ) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.stat_insight_counts,
                        insight.timesCompleted,
                        insight.timesSkipped,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.stat_insight_streak_last,
                        insight.longestStreak,
                        insight.lastCompleted?.let(DateTimeFormatters::medium)
                            ?: stringResource(R.string.stat_insight_never),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatPercent(rate: Float): String = "${(rate * 100).roundToInt()}%"

/** The icon for an achievement type. */
private fun AchievementType.icon(): ImageVector = when (this) {
    AchievementType.STREAK_7 -> Icons.Outlined.Whatshot
    AchievementType.STREAK_30 -> Icons.Outlined.LocalFireDepartment
    AchievementType.TASKS_100 -> Icons.Outlined.CheckCircle
    AchievementType.TASKS_365 -> Icons.Outlined.WorkspacePremium
    AchievementType.PERFECT_WEEK -> Icons.Outlined.Star
    AchievementType.PERFECT_MONTH -> Icons.Outlined.EmojiEvents
}

/** The label for an achievement type. */
@Composable
private fun AchievementType.label(): String = stringResource(
    when (this) {
        AchievementType.STREAK_7 -> R.string.achievement_streak_7
        AchievementType.STREAK_30 -> R.string.achievement_streak_30
        AchievementType.TASKS_100 -> R.string.achievement_tasks_100
        AchievementType.TASKS_365 -> R.string.achievement_tasks_365
        AchievementType.PERFECT_WEEK -> R.string.achievement_perfect_week
        AchievementType.PERFECT_MONTH -> R.string.achievement_perfect_month
    },
)
