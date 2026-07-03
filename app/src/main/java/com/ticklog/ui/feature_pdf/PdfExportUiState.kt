package com.ticklog.ui.feature_pdf

import androidx.compose.runtime.Immutable
import com.ticklog.domain.model.ReportScope
import java.time.LocalDate
import java.time.YearMonth

/** The span the user is choosing to export. */
enum class ReportScopeChoice {
    SINGLE_DAY,
    DATE_RANGE,
    MONTH,
    YEAR,
    ENTIRE_HISTORY,
}

/**
 * Immutable UI state for the PDF export screen.
 *
 * Holds the chosen scope and the dates each scope needs; [toScope] resolves the
 * selection to the domain [ReportScope] that drives generation.
 */
@Immutable
data class PdfExportUiState(
    val scopeChoice: ReportScopeChoice = ReportScopeChoice.ENTIRE_HISTORY,
    val singleDay: LocalDate = LocalDate.now(),
    val rangeStart: LocalDate = LocalDate.now().minusDays(6),
    val rangeEnd: LocalDate = LocalDate.now(),
    val month: YearMonth = YearMonth.now(),
    val year: Int = LocalDate.now().year,
    val isBusy: Boolean = false,
) {
    /** Whether the currently-selected range is coherent (end on/after start). */
    val isRangeValid: Boolean get() = !rangeEnd.isBefore(rangeStart)

    /** True when the current selection can be exported. */
    val canExport: Boolean
        get() = !isBusy && (scopeChoice != ReportScopeChoice.DATE_RANGE || isRangeValid)

    /** Resolves the selection to a domain [ReportScope]. */
    fun toScope(): ReportScope = when (scopeChoice) {
        ReportScopeChoice.SINGLE_DAY -> ReportScope.SingleDay(singleDay)
        ReportScopeChoice.DATE_RANGE -> ReportScope.Range(rangeStart, rangeEnd)
        ReportScopeChoice.MONTH -> ReportScope.Month(month)
        ReportScopeChoice.YEAR -> ReportScope.Year(year)
        ReportScopeChoice.ENTIRE_HISTORY -> ReportScope.EntireHistory
    }
}
