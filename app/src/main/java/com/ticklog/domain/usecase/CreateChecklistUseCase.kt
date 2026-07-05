package com.ticklog.domain.usecase

import com.ticklog.domain.model.DateRange
import com.ticklog.domain.model.TaskDraft
import com.ticklog.domain.repository.ChecklistRepository
import com.ticklog.domain.repository.PreferencesRepository
import javax.inject.Inject

/**
 * Creates the user's checklist at the end of onboarding, then marks onboarding
 * complete.
 *
 * This is the single domain action behind the "Create My Checklist" button. It
 * owns the business rules — a valid date range and at least one task — and
 * orchestrates the two repositories so the ViewModel stays a thin coordinator.
 * Generation runs first; only once it succeeds is onboarding recorded as done,
 * so an interruption can never leave the app "onboarded" with no checklist.
 */
class CreateChecklistUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository,
    private val preferencesRepository: PreferencesRepository,
) {
    /**
     * @param range the inclusive range of days to generate.
     * @param drafts the validated tasks to seed every day with (order preserved).
     * @return [Result.success] once created and onboarding is recorded, or
     *   [Result.failure] with an [IllegalArgumentException] describing why not.
     */
    suspend operator fun invoke(range: DateRange, drafts: List<TaskDraft>): Result<Unit> {
        if (!range.isValid) {
            return Result.failure(
                IllegalArgumentException("End date must be on or after start date."),
            )
        }
        if (range.exceedsMaxLength) {
            // Guard against unbounded day generation (ANR/OOM). The UI also blocks
            // over-long ranges; this is the domain-level safety net.
            return Result.failure(
                IllegalArgumentException(
                    "Range spans ${range.lengthInDays} days; the maximum is " +
                        "${DateRange.MAX_TRACKING_DAYS}.",
                ),
            )
        }
        if (drafts.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("Add at least one checklist item."),
            )
        }

        checklistRepository.createChecklist(
            name = DEFAULT_CHECKLIST_NAME,
            range = range,
            drafts = drafts,
        )
        preferencesRepository.completeOnboarding(
            startDate = range.start,
            endDate = range.end,
        )
        return Result.success(Unit)
    }

    private companion object {
        const val DEFAULT_CHECKLIST_NAME = "My Checklist"
    }
}
