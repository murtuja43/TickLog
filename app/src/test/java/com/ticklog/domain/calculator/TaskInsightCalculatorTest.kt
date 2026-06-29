package com.ticklog.domain.calculator

import com.google.common.truth.Truth.assertThat
import com.ticklog.domain.model.TaskInsightSort
import com.ticklog.domain.model.TaskOccurrence
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [TaskInsightCalculator] grouping, metrics and sorting.
 */
class TaskInsightCalculatorTest {

    private val base: LocalDate = LocalDate.of(2026, 1, 1)
    private fun day(offset: Int) = base.plusDays(offset.toLong())

    private fun occ(sourceId: Long, title: String, offset: Int, completed: Boolean) =
        TaskOccurrence(sourceItemId = sourceId, title = title, date = day(offset), isCompleted = completed)

    private val occurrences = listOf(
        // Task A: completed, completed, skipped -> 2/3, streak 2, last = day 1
        occ(1, "Apple", 0, true),
        occ(1, "Apple", 1, true),
        occ(1, "Apple", 2, false),
        // Task B: skipped, completed -> 1/2, streak 1, last = day 1
        occ(2, "Banana", 0, false),
        occ(2, "Banana", 1, true),
    )

    @Test
    fun `computes per-task metrics`() {
        val insights = TaskInsightCalculator.calculate(occurrences).associateBy { it.sourceItemId }

        val apple = insights.getValue(1)
        assertThat(apple.title).isEqualTo("Apple")
        assertThat(apple.totalOccurrences).isEqualTo(3)
        assertThat(apple.timesCompleted).isEqualTo(2)
        assertThat(apple.timesSkipped).isEqualTo(1)
        assertThat(apple.completionRate).isWithin(1e-4f).of(2f / 3f)
        assertThat(apple.lastCompleted).isEqualTo(day(1))
        assertThat(apple.longestStreak).isEqualTo(2)

        val banana = insights.getValue(2)
        assertThat(banana.timesCompleted).isEqualTo(1)
        assertThat(banana.timesSkipped).isEqualTo(1)
        assertThat(banana.longestStreak).isEqualTo(1)
        assertThat(banana.lastCompleted).isEqualTo(day(1))
    }

    @Test
    fun `a never-completed task reports null last-completed and zero rate`() {
        val never = TaskInsightCalculator.calculate(
            listOf(occ(9, "Ghost", 0, false), occ(9, "Ghost", 1, false)),
        ).single()

        assertThat(never.lastCompleted).isNull()
        assertThat(never.completionRate).isEqualTo(0f)
        assertThat(never.longestStreak).isEqualTo(0)
    }

    @Test
    fun `sorting orders by rate or title`() {
        val insights = TaskInsightCalculator.calculate(occurrences)

        assertThat(TaskInsightCalculator.sort(insights, TaskInsightSort.BEST).map { it.title })
            .containsExactly("Apple", "Banana").inOrder()
        assertThat(TaskInsightCalculator.sort(insights, TaskInsightSort.WORST).map { it.title })
            .containsExactly("Banana", "Apple").inOrder()
        assertThat(TaskInsightCalculator.sort(insights, TaskInsightSort.ALPHABETICAL).map { it.title })
            .containsExactly("Apple", "Banana").inOrder()
    }
}
