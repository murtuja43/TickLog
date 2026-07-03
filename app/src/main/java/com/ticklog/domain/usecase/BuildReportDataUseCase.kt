package com.ticklog.domain.usecase

import com.ticklog.domain.calculator.StatisticsCalculator
import com.ticklog.domain.model.DateRange
import com.ticklog.domain.model.ReportData
import com.ticklog.domain.model.ReportScope
import com.ticklog.domain.repository.ChecklistRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Assembles the [ReportData] for a chosen [ReportScope].
 *
 * Resolves the scope to a concrete inclusive range (deriving the full span for
 * "entire history" from the data), computes the summary statistics for that
 * range, and fetches every day's tasks within it. Pure orchestration over the
 * repository — the PDF rendering happens elsewhere.
 */
class BuildReportDataUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository,
) {
    suspend operator fun invoke(scope: ReportScope): ReportData {
        val allRecords = checklistRepository.observeDaySummaries().first().sortedBy { it.date }
        val range = resolveRange(scope, allRecords.firstOrNull()?.date, allRecords.lastOrNull()?.date)

        if (range == null) {
            return ReportData(
                generatedAt = LocalDateTime.now(),
                rangeStart = null,
                rangeEnd = null,
                statistics = StatisticsCalculator.calculate(emptyList(), LocalDate.now()),
                days = emptyList(),
            )
        }

        val scopedRecords = allRecords.filter { it.date in range }
        val statistics = StatisticsCalculator.calculate(scopedRecords, LocalDate.now())
        val days = checklistRepository.getChecklistsInRange(range.start, range.end)

        return ReportData(
            generatedAt = LocalDateTime.now(),
            rangeStart = range.start,
            rangeEnd = range.end,
            statistics = statistics,
            days = days,
        )
    }

    /** Resolves [scope] to a concrete range, or null when there is no data. */
    private fun resolveRange(
        scope: ReportScope,
        firstDate: LocalDate?,
        lastDate: LocalDate?,
    ): DateRange? = when (scope) {
        is ReportScope.SingleDay -> DateRange(scope.date, scope.date)
        is ReportScope.Range -> DateRange(scope.start, scope.end)
        is ReportScope.Month -> DateRange(scope.yearMonth.atDay(1), scope.yearMonth.atEndOfMonth())
        is ReportScope.Year -> DateRange(LocalDate.of(scope.year, 1, 1), LocalDate.of(scope.year, 12, 31))
        ReportScope.EntireHistory ->
            if (firstDate != null && lastDate != null) DateRange(firstDate, lastDate) else null
    }
}
