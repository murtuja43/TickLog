package com.ticklog.domain.calculator

import com.ticklog.domain.model.CompletionRecord
import java.time.LocalDate

/**
 * Pure streak calculations over per-day completion records.
 *
 * A day "counts" toward a streak only when it is **perfect** — it has at least
 * one task and every task is completed (see [CompletionRecord.isPerfectDay]).
 * Streaks are runs of consecutive calendar days; a missed or empty day, or a
 * gap in the dates, ends the run. All functions are deterministic and take the
 * reference [LocalDate] explicitly so they are trivially testable.
 */
object StreakCalculator {

    /**
     * The current streak: consecutive perfect days ending at the most recent day.
     *
     * Today is treated kindly — if today exists but is not yet perfect, it is
     * considered *pending* rather than a miss, so the streak is measured from
     * yesterday and an in-progress day never breaks it.
     */
    fun currentStreak(records: List<CompletionRecord>, today: LocalDate): Int {
        val past = records
            .filter { !it.date.isAfter(today) }
            .sortedByDescending { it.date }
        if (past.isEmpty()) return 0

        var streak = 0
        var expectedDate: LocalDate? = null
        for ((index, record) in past.withIndex()) {
            // Enforce that we are walking consecutive calendar days backward.
            if (expectedDate != null && record.date != expectedDate) break

            val isMostRecent = index == 0
            if (record.isPerfectDay) {
                streak++
                expectedDate = record.date.minusDays(1)
            } else if (isMostRecent && record.date == today) {
                // Today is still in progress: skip it without breaking the run.
                expectedDate = record.date.minusDays(1)
            } else {
                break
            }
        }
        return streak
    }

    /** The longest run of consecutive perfect days anywhere in the history. */
    fun longestStreak(records: List<CompletionRecord>): Int {
        val sorted = records.sortedBy { it.date }
        var best = 0
        var run = 0
        var previousDate: LocalDate? = null
        for (record in sorted) {
            run = if (record.isPerfectDay) {
                if (previousDate != null && record.date == previousDate.plusDays(1)) run + 1 else 1
            } else {
                0
            }
            previousDate = record.date
            if (run > best) best = run
        }
        return best
    }
}
