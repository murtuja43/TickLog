package com.ticklog.ui.feature_home

import androidx.compose.runtime.Immutable
import com.ticklog.domain.model.DailyChecklist
import java.time.LocalDate

/**
 * Immutable UI state for the Home screen.
 *
 * Phase 1 always resolves to an empty [checklist] (no template exists yet), so
 * the screen shows its placeholder. The shape, however, is the final one: once
 * generation lands, the same state simply carries a populated [DailyChecklist]
 * and the UI renders tasks with no further restructuring.
 *
 * @property selectedDate the day currently in focus.
 * @property checklist the checklist for [selectedDate], or null when none exists.
 * @property isLoading true until the first value for [selectedDate] resolves.
 */
@Immutable
data class HomeUiState(
    val selectedDate: LocalDate,
    val checklist: DailyChecklist? = null,
    val isLoading: Boolean = true,
) {
    /** True when there is no checklist content to show for the day. */
    val isEmpty: Boolean get() = !isLoading && (checklist == null || checklist.tasks.isEmpty())
}
