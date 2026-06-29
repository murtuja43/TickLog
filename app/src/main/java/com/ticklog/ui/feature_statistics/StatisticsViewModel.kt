package com.ticklog.ui.feature_statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticklog.domain.calculator.AchievementEvaluator
import com.ticklog.domain.calculator.StatisticsCalculator
import com.ticklog.domain.calculator.TaskInsightCalculator
import com.ticklog.domain.model.CompletionRecord
import com.ticklog.domain.model.TaskInsightSort
import com.ticklog.domain.repository.ChecklistRepository
import com.ticklog.domain.usecase.ObserveTaskInsightsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.ceil

/**
 * Drives the Statistics dashboard.
 *
 * One observation of the per-day summaries feeds the headline metrics, the
 * achievements and the chart series (all via pure calculators), combined with
 * the per-task insight stream and the chosen insight ordering. Keeping a single
 * summaries flow avoids re-running the aggregate query per metric.
 */
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    checklistRepository: ChecklistRepository,
    observeTaskInsights: ObserveTaskInsightsUseCase,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()
    private val insightSort = MutableStateFlow(TaskInsightSort.BEST)

    val uiState: StateFlow<StatisticsUiState> =
        combine(
            checklistRepository.observeDaySummaries(),
            observeTaskInsights(),
            insightSort,
        ) { records, insights, sort ->
            val statistics = StatisticsCalculator.calculate(records, today)
            StatisticsUiState(
                isLoading = false,
                statistics = statistics,
                weeklyCompletion = trailingRates(records, days = 7),
                monthlyCompletion = trailingRates(records, days = 30),
                completionTrend = trend(records),
                insights = TaskInsightCalculator.sort(insights, sort),
                insightSort = sort,
                achievements = AchievementEvaluator.evaluate(records, statistics),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = StatisticsUiState(isLoading = true),
        )

    /** Sets the ordering of the task-insight list. */
    fun onInsightSortChange(sort: TaskInsightSort) {
        insightSort.value = sort
    }

    /** Completion rate for each of the last [days] days ending today (0 if absent). */
    private fun trailingRates(records: List<CompletionRecord>, days: Int): List<Float> {
        val byDate = records.associateBy { it.date }
        return (days - 1 downTo 0).map { offset ->
            byDate[today.minusDays(offset.toLong())]?.completionRate ?: 0f
        }
    }

    /** The completion trend across all days, downsampled to keep the path light. */
    private fun trend(records: List<CompletionRecord>): List<Float> {
        val ordered = records.sortedBy { it.date }.map { it.completionRate }
        if (ordered.size <= MAX_TREND_POINTS) return ordered
        val bucketSize = ceil(ordered.size / MAX_TREND_POINTS.toDouble()).toInt()
        return ordered.chunked(bucketSize) { bucket -> bucket.average().toFloat() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val MAX_TREND_POINTS = 60
    }
}
