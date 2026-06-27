package com.ticklog.ui.feature_onboarding

import androidx.compose.runtime.Immutable
import java.time.LocalDate

/**
 * Immutable UI state for the onboarding screen.
 *
 * Holds the two dates the user is choosing plus transient flags. Derived
 * booleans ([hasRangeError], [canContinue]) are computed here so the composable
 * stays purely declarative and never re-derives validation itself.
 *
 * @property startDate the selected start date, or null if not yet chosen.
 * @property endDate the selected end date, or null if not yet chosen.
 * @property isSaving true while the selection is being persisted.
 */
@Immutable
data class OnboardingUiState(
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val isSaving: Boolean = false,
) {
    /** True when both dates are present but the end precedes the start. */
    val hasRangeError: Boolean
        get() = startDate != null && endDate != null && endDate.isBefore(startDate)

    /** True when a valid range is selected and nothing is in flight. */
    val canContinue: Boolean
        get() = startDate != null && endDate != null && !hasRangeError && !isSaving
}
