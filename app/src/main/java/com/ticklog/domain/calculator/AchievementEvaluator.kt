package com.ticklog.domain.calculator

import com.ticklog.domain.model.Achievement
import com.ticklog.domain.model.AchievementType
import com.ticklog.domain.model.ChecklistStatistics
import com.ticklog.domain.model.CompletionRecord
import java.time.YearMonth
import java.time.temporal.IsoFields

/**
 * Pure evaluation of which achievements the user has unlocked.
 *
 * Streak and total-task milestones read straight from [ChecklistStatistics];
 * the "perfect week/month" badges require a *full* calendar week or month in
 * which every day is perfect, computed from the records.
 */
object AchievementEvaluator {

    fun evaluate(
        records: List<CompletionRecord>,
        statistics: ChecklistStatistics,
    ): List<Achievement> {
        val perfectWeek = hasPerfectWeek(records)
        val perfectMonth = hasPerfectMonth(records)
        return AchievementType.entries.map { type ->
            val unlocked = when (type) {
                AchievementType.STREAK_7 -> statistics.longestStreak >= 7
                AchievementType.STREAK_30 -> statistics.longestStreak >= 30
                AchievementType.TASKS_100 -> statistics.totalCompletedTasks >= 100
                AchievementType.TASKS_365 -> statistics.totalCompletedTasks >= 365
                AchievementType.PERFECT_WEEK -> perfectWeek
                AchievementType.PERFECT_MONTH -> perfectMonth
            }
            Achievement(type = type, unlocked = unlocked)
        }
    }

    /** True if any ISO week is fully present (7 days) and entirely perfect. */
    private fun hasPerfectWeek(records: List<CompletionRecord>): Boolean =
        records
            .groupBy {
                it.date.get(IsoFields.WEEK_BASED_YEAR) to
                    it.date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
            }
            .values
            .any { week ->
                week.size == CalendarGenerator.DAYS_PER_WEEK && week.all { it.isPerfectDay }
            }

    /** True if any calendar month is fully present and entirely perfect. */
    private fun hasPerfectMonth(records: List<CompletionRecord>): Boolean =
        records
            .groupBy { YearMonth.from(it.date) }
            .any { (yearMonth, days) ->
                days.size == yearMonth.lengthOfMonth() && days.all { it.isPerfectDay }
            }
}
