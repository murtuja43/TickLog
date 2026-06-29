package com.ticklog.domain.usecase

import com.ticklog.domain.calculator.TaskInsightCalculator
import com.ticklog.domain.model.TaskInsight
import com.ticklog.domain.repository.ChecklistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Streams per-task [TaskInsight]s, recomputed from task occurrences as the data
 * changes. Ordering is applied in the UI layer via [TaskInsightCalculator.sort].
 */
class ObserveTaskInsightsUseCase @Inject constructor(
    private val checklistRepository: ChecklistRepository,
) {
    operator fun invoke(): Flow<List<TaskInsight>> =
        checklistRepository.observeTaskOccurrences()
            .map { occurrences -> TaskInsightCalculator.calculate(occurrences) }
}
