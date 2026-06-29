package com.ticklog.domain.calculator

import com.google.common.truth.Truth.assertThat
import com.ticklog.domain.model.AchievementType
import com.ticklog.domain.model.ChecklistStatistics
import com.ticklog.domain.model.CompletionRecord
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [AchievementEvaluator] unlock rules.
 */
class AchievementEvaluatorTest {

    private fun unlocked(
        records: List<CompletionRecord>,
        statistics: ChecklistStatistics,
    ): Map<AchievementType, Boolean> =
        AchievementEvaluator.evaluate(records, statistics).associate { it.type to it.unlocked }

    private fun perfect(date: LocalDate) =
        CompletionRecord(date = date, totalItems = 1, completedItems = 1)

    @Test
    fun `streak and task milestones follow the statistics thresholds`() {
        val stats = ChecklistStatistics.EMPTY.copy(
            longestStreak = 7,
            totalCompletedTasks = 100,
        )
        val result = unlocked(emptyList(), stats)

        assertThat(result[AchievementType.STREAK_7]).isTrue()
        assertThat(result[AchievementType.STREAK_30]).isFalse()
        assertThat(result[AchievementType.TASKS_100]).isTrue()
        assertThat(result[AchievementType.TASKS_365]).isFalse()
    }

    @Test
    fun `a full perfect ISO week unlocks PERFECT_WEEK`() {
        // Mon 5 Jan .. Sun 11 Jan 2026 is one ISO week.
        val week = (5..11).map { perfect(LocalDate.of(2026, 1, it)) }
        assertThat(unlocked(week, ChecklistStatistics.EMPTY)[AchievementType.PERFECT_WEEK]).isTrue()
    }

    @Test
    fun `an imperfect day in the week does not unlock PERFECT_WEEK`() {
        val week = (5..11).map { dayOfMonth ->
            val date = LocalDate.of(2026, 1, dayOfMonth)
            if (dayOfMonth == 8) CompletionRecord(date, totalItems = 2, completedItems = 1) else perfect(date)
        }
        assertThat(unlocked(week, ChecklistStatistics.EMPTY)[AchievementType.PERFECT_WEEK]).isFalse()
    }

    @Test
    fun `a fully perfect month unlocks PERFECT_MONTH`() {
        // February 2026 has 28 days.
        val month = (1..28).map { perfect(LocalDate.of(2026, 2, it)) }
        assertThat(unlocked(month, ChecklistStatistics.EMPTY)[AchievementType.PERFECT_MONTH]).isTrue()
    }
}
