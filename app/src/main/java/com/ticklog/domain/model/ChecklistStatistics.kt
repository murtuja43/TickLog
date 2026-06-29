package com.ticklog.domain.model

/**
 * The complete set of headline statistics for the user's checklist.
 *
 * Every value is derived from the per-day completion summaries, so it always
 * reflects the live data with no separate ledger to keep in sync.
 *
 * @property overallCompletionRate completed tasks / total tasks, in [0f, 1f].
 * @property totalCompletedTasks tasks completed across all days.
 * @property totalIncompleteTasks tasks not completed across all days.
 * @property totalChecklistDays number of generated days.
 * @property currentStreak consecutive perfect days ending now.
 * @property longestStreak the longest run of perfect days ever.
 * @property completedThisWeek tasks completed on days in the current week.
 * @property completedThisMonth tasks completed on days in the current month.
 * @property completedThisYear tasks completed on days in the current year.
 * @property averageDailyCompletion mean of per-day completion rates (days with tasks).
 */
data class ChecklistStatistics(
    val overallCompletionRate: Float,
    val totalCompletedTasks: Int,
    val totalIncompleteTasks: Int,
    val totalChecklistDays: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val completedThisWeek: Int,
    val completedThisMonth: Int,
    val completedThisYear: Int,
    val averageDailyCompletion: Float,
) {
    /** True once at least one day exists to report on. */
    val hasData: Boolean get() = totalChecklistDays > 0

    companion object {
        /** Zeroed statistics for the empty state. */
        val EMPTY = ChecklistStatistics(
            overallCompletionRate = 0f,
            totalCompletedTasks = 0,
            totalIncompleteTasks = 0,
            totalChecklistDays = 0,
            currentStreak = 0,
            longestStreak = 0,
            completedThisWeek = 0,
            completedThisMonth = 0,
            completedThisYear = 0,
            averageDailyCompletion = 0f,
        )
    }
}
