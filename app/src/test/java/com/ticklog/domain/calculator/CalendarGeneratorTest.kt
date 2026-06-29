package com.ticklog.domain.calculator

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.YearMonth

/**
 * Unit tests for [CalendarGenerator] grid layout.
 */
class CalendarGeneratorTest {

    @Test
    fun `grid contains every day of the month`() {
        val month = YearMonth.of(2026, 6) // June has 30 days
        val days = CalendarGenerator.generate(month).mapNotNull { it.date }

        assertThat(days).hasSize(30)
        assertThat(days.first()).isEqualTo(month.atDay(1))
        assertThat(days.last()).isEqualTo(month.atDay(30))
    }

    @Test
    fun `grid is always whole weeks`() {
        for (monthValue in 1..12) {
            val cells = CalendarGenerator.generate(YearMonth.of(2026, monthValue))
            assertThat(cells.size % CalendarGenerator.DAYS_PER_WEEK).isEqualTo(0)
        }
    }

    @Test
    fun `leading padding aligns the first day under its weekday (Monday start)`() {
        val month = YearMonth.of(2026, 6)
        val cells = CalendarGenerator.generate(month, firstDayOfWeek = DayOfWeek.MONDAY)

        val leadingBlanks = cells.indexOfFirst { it.date != null }
        val expected = (month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
        assertThat(leadingBlanks).isEqualTo(expected)
    }

    @Test
    fun `ordered week days start at the configured first day`() {
        assertThat(CalendarGenerator.orderedWeekDays(DayOfWeek.MONDAY).first())
            .isEqualTo(DayOfWeek.MONDAY)
        assertThat(CalendarGenerator.orderedWeekDays(DayOfWeek.SUNDAY))
            .containsExactly(
                DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
            ).inOrder()
    }
}
