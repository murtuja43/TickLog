package com.ticklog.domain.model

import java.time.LocalDate

/**
 * One occurrence of a template-linked task on a given day — the raw input to
 * insight calculations.
 */
data class TaskOccurrence(
    val sourceItemId: Long,
    val title: String,
    val date: LocalDate,
    val isCompleted: Boolean,
)

/**
 * Aggregated performance of a single task across every day it appears on.
 *
 * @property sourceItemId the template item this insight summarises.
 * @property title the task's current title.
 * @property totalOccurrences number of days the task appeared on.
 * @property timesCompleted days it was completed.
 * @property timesSkipped days it was not completed.
 * @property lastCompleted the most recent day it was completed, if ever.
 * @property longestStreak the longest run of consecutive days it was completed.
 */
data class TaskInsight(
    val sourceItemId: Long,
    val title: String,
    val totalOccurrences: Int,
    val timesCompleted: Int,
    val timesSkipped: Int,
    val lastCompleted: LocalDate?,
    val longestStreak: Int,
) {
    /** Completion ratio in [0f, 1f]; 0 when the task never occurred. */
    val completionRate: Float
        get() = if (totalOccurrences == 0) 0f else timesCompleted.toFloat() / totalOccurrences
}

/** Ordering options for the task-insight list. */
enum class TaskInsightSort {
    /** Highest completion rate first. */
    BEST,

    /** Lowest completion rate first. */
    WORST,

    /** A→Z by title. */
    ALPHABETICAL,
}
