package com.ticklog.domain.calculator

import com.google.common.truth.Truth.assertThat
import com.ticklog.domain.model.CompletionRecord
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [StatisticsCalculator] aggregations.
 */
class StatisticsCalculatorTest {

    // Saturday; the ISO week runs Mon 5 Jan .. Sun 11 Jan 2026.
    private val today: LocalDate = LocalDate.of(2026, 1, 10)

    private fun record(date: LocalDate, total: Int, completed: Int) =
        CompletionRecord(date = date, totalItems = total, completedItems = completed)

    @Test
    fun `empty records yield EMPTY statistics`() {
        assertThat(StatisticsCalculator.calculate(emptyList(), today))
            .isEqualTo(com.ticklog.domain.model.ChecklistStatistics.EMPTY)
    }

    @Test
    fun `aggregates totals, windows, averages and streaks`() {
        val records = listOf(
            record(LocalDate.of(2025, 12, 31), total = 2, completed = 1), // last year
            record(LocalDate.of(2026, 1, 5), total = 2, completed = 2), // this week/month/year
            record(LocalDate.of(2026, 1, 8), total = 2, completed = 1), // this week/month/year
            record(LocalDate.of(2026, 1, 10), total = 2, completed = 2), // today
        )

        val stats = StatisticsCalculator.calculate(records, today)

        assertThat(stats.totalCompletedTasks).isEqualTo(6)
        assertThat(stats.totalIncompleteTasks).isEqualTo(2)
        assertThat(stats.totalChecklistDays).isEqualTo(4)
        assertThat(stats.overallCompletionRate).isWithin(1e-4f).of(0.75f)
        assertThat(stats.averageDailyCompletion).isWithin(1e-4f).of(0.75f)
        assertThat(stats.completedThisWeek).isEqualTo(5) // Jan 5 + Jan 8 + Jan 10
        assertThat(stats.completedThisMonth).isEqualTo(5)
        assertThat(stats.completedThisYear).isEqualTo(5)
        // Today is perfect but Jan 9 is missing, so the current streak is just today.
        assertThat(stats.currentStreak).isEqualTo(1)
        assertThat(stats.longestStreak).isEqualTo(1)
    }

    @Test
    fun `average daily completion ignores days with no tasks`() {
        val records = listOf(
            record(LocalDate.of(2026, 1, 8), total = 2, completed = 1), // 0.5
            record(LocalDate.of(2026, 1, 9), total = 0, completed = 0), // ignored
            record(LocalDate.of(2026, 1, 10), total = 2, completed = 2), // 1.0
        )

        val stats = StatisticsCalculator.calculate(records, today)

        assertThat(stats.averageDailyCompletion).isWithin(1e-4f).of(0.75f)
    }
}
