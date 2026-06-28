package com.ticklog.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * Pure domain models for the checklist feature.
 *
 * These are deliberately free of any Room/persistence annotations. The data
 * layer maps its entities to and from these types, so ViewModels and (eventual)
 * use cases depend only on the domain — the essence of Clean Architecture.
 *
 * Phase 1 ships the models and the read paths that produce them; the creation
 * and completion flows that populate them arrive in Phase 2.
 */

/** A checklist blueprint the user defines once. */
data class ChecklistTemplate(
    val id: Long,
    val name: String,
    val description: String?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** A task definition within a [ChecklistTemplate]. */
data class ChecklistItem(
    val id: Long,
    val templateId: Long,
    val title: String,
    val description: String?,
    val position: Int,
)

/**
 * A single tickable task for one day.
 *
 * @property isLinkedToTemplate whether this task descends from a template item.
 *   Only linked tasks can be edited/deleted "for today and all future days";
 *   standalone tasks (added or duplicated for a single day) are day-local. The
 *   UI uses this to decide whether to offer a scope choice.
 */
data class DailyTask(
    val id: Long,
    val title: String,
    val note: String?,
    val position: Int,
    val isCompleted: Boolean,
    val completedAt: Instant?,
    val isLinkedToTemplate: Boolean,
)

/**
 * A whole day's checklist: the date plus its ordered tasks.
 *
 * Derived progress values live here as computed properties so the UI never has
 * to recompute them and they can never drift out of sync with [tasks].
 */
data class DailyChecklist(
    val id: Long,
    val templateId: Long,
    val date: LocalDate,
    val tasks: List<DailyTask>,
) {
    /** Number of completed tasks. */
    val completedCount: Int get() = tasks.count { it.isCompleted }

    /** Total number of tasks. */
    val totalCount: Int get() = tasks.size

    /** Completion ratio in [0f, 1f]; 0 when the day has no tasks. */
    val completionRate: Float
        get() = if (tasks.isEmpty()) 0f else completedCount.toFloat() / totalCount

    /** True when there are tasks and every one of them is complete. */
    val isFullyCompleted: Boolean get() = tasks.isNotEmpty() && completedCount == totalCount
}

/**
 * An immutable summary of one day's completion, read from the history ledger.
 *
 * The completion rate is computed on demand rather than stored, keeping the
 * persisted data free of derivable, drift-prone columns.
 */
data class CompletionRecord(
    val date: LocalDate,
    val totalItems: Int,
    val completedItems: Int,
) {
    /** Completion ratio in [0f, 1f]; 0 when the day had no tasks. */
    val completionRate: Float
        get() = if (totalItems == 0) 0f else completedItems.toFloat() / totalItems

    /** True when the day was fully completed. */
    val isPerfectDay: Boolean get() = totalItems > 0 && completedItems >= totalItems
}
