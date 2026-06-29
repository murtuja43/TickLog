package com.ticklog.ui.feature_history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticklog.domain.model.CompletionRecord
import com.ticklog.domain.model.DailyChecklist
import com.ticklog.domain.repository.ChecklistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

/**
 * Drives the History timeline.
 *
 * The displayed list is derived reactively from four inputs — the per-day
 * summaries, the (database-backed) search results, the completion filter and the
 * sort order. Search runs in Room and only the matching dates are intersected
 * here, so the screen scales to thousands of days. Each day's task detail is
 * loaded lazily, only when a card is expanded.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val checklistRepository: ChecklistRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(HistoryFilter.ALL)
    private val sort = MutableStateFlow(HistorySort.NEWEST)

    /** Dates matching the current text search, or null when no search is active. */
    private val matchingDates: Flow<Set<LocalDate>?> =
        query.flatMapLatest { raw ->
            if (raw.isBlank()) flowOf(null) else checklistRepository.searchDates(raw).map { it.toSet() }
        }

    val uiState: StateFlow<HistoryUiState> =
        combine(
            checklistRepository.observeDaySummaries(),
            matchingDates,
            filter,
            sort,
        ) { records, dates, activeFilter, activeSort ->
            val searched = if (dates == null) records else records.filter { it.date in dates }
            val filtered = searched.filter { it.matches(activeFilter) }
            val ordered = when (activeSort) {
                HistorySort.NEWEST -> filtered.sortedByDescending { it.date }
                HistorySort.OLDEST -> filtered.sortedBy { it.date }
            }
            HistoryUiState(
                isLoading = false,
                days = ordered,
                totalDays = records.size,
                filter = activeFilter,
                sort = activeSort,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = HistoryUiState(isLoading = true),
        )

    /** Updates the text search (matches task titles and notes). */
    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    /** Sets the completion filter. */
    fun onFilterChange(newFilter: HistoryFilter) {
        filter.value = newFilter
    }

    /** Sets the chronological ordering. */
    fun onSortChange(newSort: HistorySort) {
        sort.value = newSort
    }

    /** A lazily-collected stream of a single day's tasks, for an expanded card. */
    fun tasksForDate(date: LocalDate): Flow<DailyChecklist?> =
        checklistRepository.observeChecklistForDate(date)

    private fun CompletionRecord.matches(filter: HistoryFilter): Boolean = when (filter) {
        HistoryFilter.ALL -> true
        HistoryFilter.COMPLETED -> isPerfectDay
        HistoryFilter.INCOMPLETE -> totalItems > 0 && completedItems < totalItems
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
