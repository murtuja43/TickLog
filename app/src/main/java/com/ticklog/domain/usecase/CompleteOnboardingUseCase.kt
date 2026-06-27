package com.ticklog.domain.usecase

import com.ticklog.domain.model.DateRange
import com.ticklog.domain.repository.PreferencesRepository
import javax.inject.Inject

/**
 * Completes onboarding for a chosen [DateRange].
 *
 * The use case owns the business rule that the range must be valid before it can
 * be persisted, returning a [Result] so the caller can surface a precise error
 * without the ViewModel re-implementing validation.
 */
class CompleteOnboardingUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) {
    /**
     * @param range the inclusive tracking range the user selected.
     * @return [Result.success] once persisted, or [Result.failure] with an
     *   [IllegalArgumentException] if the range is invalid.
     */
    suspend operator fun invoke(range: DateRange): Result<Unit> {
        if (!range.isValid) {
            return Result.failure(
                IllegalArgumentException("End date must be on or after start date."),
            )
        }
        preferencesRepository.completeOnboarding(
            startDate = range.start,
            endDate = range.end,
        )
        return Result.success(Unit)
    }
}
