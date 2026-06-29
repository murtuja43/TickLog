package com.ticklog.ui.feature_calendar

import androidx.compose.runtime.Immutable
import com.ticklog.domain.model.CompletionRecord
import java.time.LocalDate

/**
 * Immutable UI state for the Calendar screen.
 *
 * The day pager and currently-displayed month are UI concerns owned by the
 * screen; the ViewModel only supplies the data needed to paint any month: a
 * fast date→summary lookup and today's date.
 *
 * @property isLoading true until the first data load resolves.
 * @property today the current date (for highlighting).
 * @property summariesByDate completion summary for each generated day, by date.
 */
@Immutable
data class CalendarUiState(
    val isLoading: Boolean,
    val today: LocalDate,
    val summariesByDate: Map<LocalDate, CompletionRecord> = emptyMap(),
) {
    /** True once at least one generated day exists. */
    val hasData: Boolean get() = summariesByDate.isNotEmpty()
}
