package com.ticklog.domain.calculator

import com.google.common.truth.Truth.assertThat
import com.ticklog.domain.model.CompletionRecord
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [StreakCalculator] — the rule that only fully-completed
 * ("perfect") days count, runs are consecutive, and today is treated as pending.
 */
class StreakCalculatorTest {

    private val today: LocalDate = LocalDate.of(2026, 1, 10)
    private fun day(offset: Int) = today.plusDays(offset.toLong())

    /** A perfect (2/2) or imperfect (1/2) day. */
    private fun record(offset: Int, perfect: Boolean) =
        CompletionRecord(date = day(offset), totalItems = 2, completedItems = if (perfect) 2 else 1)

    @Test
    fun `empty history has no streak`() {
        assertThat(StreakCalculator.currentStreak(emptyList(), today)).isEqualTo(0)
        assertThat(StreakCalculator.longestStreak(emptyList())).isEqualTo(0)
    }

    @Test
    fun `current streak counts consecutive perfect days ending today`() {
        val records = listOf(record(-2, true), record(-1, true), record(0, true))
        assertThat(StreakCalculator.currentStreak(records, today)).isEqualTo(3)
    }

    @Test
    fun `an incomplete today is pending and does not break the streak`() {
        val records = listOf(record(-2, true), record(-1, true), record(0, false))
        assertThat(StreakCalculator.currentStreak(records, today)).isEqualTo(2)
    }

    @Test
    fun `a missed earlier day breaks the current streak`() {
        val records = listOf(record(-2, true), record(-1, false), record(0, true))
        assertThat(StreakCalculator.currentStreak(records, today)).isEqualTo(1)
    }

    @Test
    fun `current streak measures up to the most recent past day when today is absent`() {
        val records = listOf(record(-3, true), record(-2, true), record(-1, true))
        assertThat(StreakCalculator.currentStreak(records, today)).isEqualTo(3)
    }

    @Test
    fun `a gap in dates ends the streak`() {
        // -3 and -2 perfect, then a missing day at -1, then today perfect.
        val records = listOf(record(-3, true), record(-2, true), record(0, true))
        assertThat(StreakCalculator.currentStreak(records, today)).isEqualTo(1)
    }

    @Test
    fun `longest streak is the maximal run anywhere`() {
        val records = listOf(
            record(-6, true), record(-5, true), // run of 2
            record(-4, false),
            record(-3, true), record(-2, true), record(-1, true), record(0, true), // run of 4
        )
        assertThat(StreakCalculator.longestStreak(records)).isEqualTo(4)
    }

    @Test
    fun `an empty day is not perfect and breaks the streak`() {
        val records = listOf(
            record(-1, true),
            CompletionRecord(date = day(0), totalItems = 0, completedItems = 0),
        )
        assertThat(StreakCalculator.longestStreak(records)).isEqualTo(1)
    }
}
