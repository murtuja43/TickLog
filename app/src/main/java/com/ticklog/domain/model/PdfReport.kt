package com.ticklog.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

/**
 * What span of history a PDF report should cover.
 *
 * A closed set of scopes the user can pick from; each resolves to a concrete
 * inclusive date range when the report is built.
 */
sealed interface ReportScope {
    /** A single day. */
    data class SingleDay(val date: LocalDate) : ReportScope

    /** An explicit inclusive date range. */
    data class Range(val start: LocalDate, val end: LocalDate) : ReportScope

    /** Every generated day. */
    data object EntireHistory : ReportScope

    /** A whole calendar month. */
    data class Month(val yearMonth: YearMonth) : ReportScope

    /** A whole calendar year. */
    data class Year(val year: Int) : ReportScope
}

/**
 * The fully-assembled content of a report, ready to render to PDF.
 *
 * @property generatedAt when the export was produced.
 * @property rangeStart first day of the covered span (null when there is no data).
 * @property rangeEnd last day of the covered span (null when there is no data).
 * @property statistics summary metrics for the covered span.
 * @property days the days to detail, in chronological order.
 */
data class ReportData(
    val generatedAt: LocalDateTime,
    val rangeStart: LocalDate?,
    val rangeEnd: LocalDate?,
    val statistics: ChecklistStatistics,
    val days: List<DailyChecklist>,
) {
    /** True when there is nothing to report on. */
    val isEmpty: Boolean get() = days.isEmpty()
}
