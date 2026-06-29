package com.ticklog.domain.calculator

import com.ticklog.domain.model.ChecklistStatistics
import com.ticklog.domain.model.CompletionRecord
import java.time.LocalDate

/**
 * Pure aggregation of per-day completion records into [ChecklistStatistics].
 *
 * Everything is derived from the records themselves (each carries a date and its
 * completed/total counts), so the dashboard always matches the live data with no
 * separate ledger. The reference [LocalDate] is passed in for deterministic,
 * testable "this week / month / year" windows.
 */
object StatisticsCalculator {

    fun calculate(records: List<CompletionRecord>, today: LocalDate): ChecklistStatistics {
        if (records.isEmpty()) return ChecklistStatistics.EMPTY

        val totalCompleted = records.sumOf { it.completedItems }
        val totalTasks = records.sumOf { it.totalItems }
        val totalIncomplete = totalTasks - totalCompleted

        val daysWithTasks = records.filter { it.totalItems > 0 }
        val averageDaily =
            if (daysWithTasks.isEmpty()) 0f
            else daysWithTasks.map { it.completionRate }.average().toFloat()

        // ISO week: Monday of the current week through the following Sunday.
        val weekStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val weekEnd = weekStart.plusDays(6)

        return ChecklistStatistics(
            overallCompletionRate = if (totalTasks == 0) 0f else totalCompleted.toFloat() / totalTasks,
            totalCompletedTasks = totalCompleted,
            totalIncompleteTasks = totalIncomplete,
            totalChecklistDays = records.size,
            currentStreak = StreakCalculator.currentStreak(records, today),
            longestStreak = StreakCalculator.longestStreak(records),
            completedThisWeek = records
                .filter { !it.date.isBefore(weekStart) && !it.date.isAfter(weekEnd) }
                .sumOf { it.completedItems },
            completedThisMonth = records
                .filter { it.date.year == today.year && it.date.month == today.month }
                .sumOf { it.completedItems },
            completedThisYear = records
                .filter { it.date.year == today.year }
                .sumOf { it.completedItems },
            averageDailyCompletion = averageDaily,
        )
    }
}
