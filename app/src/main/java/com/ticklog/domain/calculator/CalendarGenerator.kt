package com.ticklog.domain.calculator

import com.ticklog.domain.model.CalendarCell
import java.time.DayOfWeek
import java.time.YearMonth

/**
 * Pure month-grid generation.
 *
 * Produces a flat list of [CalendarCell]s for a month, padded with nulls so the
 * first real day lands under the correct weekday column and the grid always
 * contains whole weeks (its size is a multiple of 7).
 */
object CalendarGenerator {

    /** Number of day columns in a week. */
    const val DAYS_PER_WEEK: Int = 7

    /**
     * @param yearMonth the month to lay out.
     * @param firstDayOfWeek the weekday the grid starts on (default Monday).
     */
    fun generate(
        yearMonth: YearMonth,
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ): List<CalendarCell> {
        val firstOfMonth = yearMonth.atDay(1)
        val leadingBlanks =
            ((firstOfMonth.dayOfWeek.value - firstDayOfWeek.value) + DAYS_PER_WEEK) % DAYS_PER_WEEK

        val cells = ArrayList<CalendarCell>(leadingBlanks + yearMonth.lengthOfMonth())
        repeat(leadingBlanks) { cells.add(CalendarCell(null)) }
        for (dayOfMonth in 1..yearMonth.lengthOfMonth()) {
            cells.add(CalendarCell(yearMonth.atDay(dayOfMonth)))
        }
        while (cells.size % DAYS_PER_WEEK != 0) {
            cells.add(CalendarCell(null))
        }
        return cells
    }

    /** The weekday headers, starting from [firstDayOfWeek] (e.g. Mon..Sun). */
    fun orderedWeekDays(firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY): List<DayOfWeek> =
        (0 until DAYS_PER_WEEK).map { firstDayOfWeek.plus(it.toLong()) }
}
