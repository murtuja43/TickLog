package com.ticklog.ui.feature_home

import androidx.compose.runtime.Immutable
import com.ticklog.domain.model.DailyTask
import java.time.LocalDate

/**
 * The state of a single day page on the Home screen.
 *
 * Each day in the pager resolves to exactly one of these. Modelling the states
 * as an exhaustive hierarchy (rather than a bag of nullable flags) lets the UI
 * render with a single `when` and makes the empty/out-of-range distinctions
 * impossible to get wrong.
 */
@Immutable
sealed interface DayUiState {

    /** The date this state describes. */
    val date: LocalDate

    /** Still resolving the day's data. */
    data class Loading(override val date: LocalDate) : DayUiState

    /** The date falls outside the user's generated tracking range. */
    data class OutOfRange(override val date: LocalDate) : DayUiState

    /** The date is in range but has no tasks. */
    data class Empty(override val date: LocalDate) : DayUiState

    /** The date has one or more tasks. */
    data class Content(
        override val date: LocalDate,
        val tasks: List<DailyTask>,
    ) : DayUiState
}

/**
 * One-shot effects the Home screen reacts to (e.g. showing the undo snackbar).
 *
 * Delivered through a channel so they fire exactly once and never replay on
 * recomposition or configuration change.
 */
sealed interface HomeEvent {
    /** A delete just happened and can be undone; [count] rows were removed. */
    data class TasksDeleted(val count: Int) : HomeEvent

    /** A write (toggle, add, edit, delete, undo) failed to persist. */
    data object ActionFailed : HomeEvent
}
