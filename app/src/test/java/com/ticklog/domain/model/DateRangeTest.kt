package com.ticklog.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [DateRange] — the pure basis of day generation.
 */
class DateRangeTest {

    private val base: LocalDate = LocalDate.of(2026, 1, 10)

    @Test
    fun `datesInclusive returns a single date for a one-day range`() {
        val range = DateRange(base, base)

        assertThat(range.datesInclusive()).containsExactly(base)
        assertThat(range.lengthInDays).isEqualTo(1)
    }

    @Test
    fun `datesInclusive returns every day in order for a multi-day range`() {
        val range = DateRange(base, base.plusDays(3))

        assertThat(range.datesInclusive()).containsExactly(
            base,
            base.plusDays(1),
            base.plusDays(2),
            base.plusDays(3),
        ).inOrder()
        assertThat(range.lengthInDays).isEqualTo(4)
    }

    @Test
    fun `an invalid range is empty and reports invalid`() {
        val range = DateRange(base.plusDays(1), base)

        assertThat(range.isValid).isFalse()
        assertThat(range.datesInclusive()).isEmpty()
    }

    @Test
    fun `contains is inclusive of both ends and excludes outside`() {
        val range = DateRange(base, base.plusDays(2))

        assertThat(base in range).isTrue()
        assertThat(base.plusDays(2) in range).isTrue()
        assertThat(base.minusDays(1) in range).isFalse()
        assertThat(base.plusDays(3) in range).isFalse()
    }

    @Test
    fun `datesInclusive spans across a month boundary`() {
        val start = LocalDate.of(2026, 1, 30)
        val end = LocalDate.of(2026, 2, 2)

        assertThat(DateRange(start, end).datesInclusive()).containsExactly(
            LocalDate.of(2026, 1, 30),
            LocalDate.of(2026, 1, 31),
            LocalDate.of(2026, 2, 1),
            LocalDate.of(2026, 2, 2),
        ).inOrder()
    }
}
