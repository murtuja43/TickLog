package com.ticklog.domain.calculator

import com.ticklog.domain.model.TaskInsight
import com.ticklog.domain.model.TaskInsightSort
import com.ticklog.domain.model.TaskOccurrence
import java.time.LocalDate

/**
 * Pure per-task insight calculations.
 *
 * Occurrences are grouped by their template item; for each group the completion
 * rate, counts, last-completed date and longest completion streak are computed.
 */
object TaskInsightCalculator {

    /** Builds one [TaskInsight] per template-linked task. */
    fun calculate(occurrences: List<TaskOccurrence>): List<TaskInsight> =
        occurrences
            .groupBy { it.sourceItemId }
            .map { (sourceItemId, group) ->
                val ordered = group.sortedBy { it.date }
                val completed = ordered.count { it.isCompleted }
                TaskInsight(
                    sourceItemId = sourceItemId,
                    title = ordered.last().title, // most recent title is the current name
                    totalOccurrences = ordered.size,
                    timesCompleted = completed,
                    timesSkipped = ordered.size - completed,
                    lastCompleted = ordered.lastOrNull { it.isCompleted }?.date,
                    longestStreak = longestCompletionStreak(ordered),
                )
            }

    /** Applies the chosen [sort] to a list of insights. */
    fun sort(insights: List<TaskInsight>, sort: TaskInsightSort): List<TaskInsight> = when (sort) {
        TaskInsightSort.BEST ->
            insights.sortedWith(compareByDescending<TaskInsight> { it.completionRate }.thenBy { it.title })
        TaskInsightSort.WORST ->
            insights.sortedWith(compareBy<TaskInsight> { it.completionRate }.thenBy { it.title })
        TaskInsightSort.ALPHABETICAL ->
            insights.sortedBy { it.title.lowercase() }
    }

    /** Longest run of consecutive days (by date) on which a task was completed. */
    private fun longestCompletionStreak(orderedByDate: List<TaskOccurrence>): Int {
        var best = 0
        var run = 0
        var previousDate: LocalDate? = null
        for (occurrence in orderedByDate) {
            run = if (occurrence.isCompleted) {
                if (previousDate != null && occurrence.date == previousDate.plusDays(1)) run + 1 else 1
            } else {
                0
            }
            previousDate = occurrence.date
            if (run > best) best = run
        }
        return best
    }
}
