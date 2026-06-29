package com.ticklog.ui.feature_history

import androidx.compose.runtime.Immutable
import com.ticklog.domain.model.CompletionRecord

/** Which days the history timeline shows. */
enum class HistoryFilter {
    /** Every day. */
    ALL,

    /** Only days where every task was completed. */
    COMPLETED,

    /** Only days that have at least one incomplete task. */
    INCOMPLETE,
}

/** Chronological ordering of the timeline. */
enum class HistorySort {
    /** Most recent day first. */
    NEWEST,

    /** Oldest day first. */
    OLDEST,
}

/**
 * Immutable UI state for the History screen.
 *
 * [days] is the already-filtered, searched and sorted list rendered by the
 * timeline. [totalDays] is the unfiltered count, used to tell "no history at
 * all" apart from "no days match the current filter/search".
 *
 * @property isLoading true until the first load resolves.
 * @property days the rows to display.
 * @property totalDays number of generated days before filtering.
 * @property filter the active completion filter.
 * @property sort the active ordering.
 */
@Immutable
data class HistoryUiState(
    val isLoading: Boolean,
    val days: List<CompletionRecord> = emptyList(),
    val totalDays: Int = 0,
    val filter: HistoryFilter = HistoryFilter.ALL,
    val sort: HistorySort = HistorySort.NEWEST,
) {
    /** True once any day exists in history. */
    val hasAnyHistory: Boolean get() = totalDays > 0

    /** True when there is history but nothing matches the current filter/search. */
    val isEmptyResult: Boolean get() = !isLoading && hasAnyHistory && days.isEmpty()
}
