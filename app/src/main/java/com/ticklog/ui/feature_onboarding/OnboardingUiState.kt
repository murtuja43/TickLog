package com.ticklog.ui.feature_onboarding

import androidx.compose.runtime.Immutable
import com.ticklog.domain.model.DateRange
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

    /**
     * True when the chosen range is valid but longer than the supported maximum
     * ([DateRange.MAX_TRACKING_DAYS]). Surfaced so the user is told why they can't
     * continue instead of hitting an unbounded generation.
     */
    val hasTooLongRange: Boolean
        get() = startDate != null && endDate != null && !hasRangeError &&
            DateRange(startDate, endDate).exceedsMaxLength

    /** True when a valid, in-range selection is made and nothing is in flight. */
    val canContinue: Boolean
        get() = startDate != null && endDate != null &&
            !hasRangeError && !hasTooLongRange && !isSaving
}
