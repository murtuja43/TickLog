package com.ticklog.data.model

import com.ticklog.data.database.entity.ChecklistItemEntity
import com.ticklog.data.database.entity.ChecklistTemplateEntity
import com.ticklog.data.database.entity.CompletionHistoryEntity
import com.ticklog.data.database.entity.DailyChecklistItemEntity
import com.ticklog.data.database.relation.DailyChecklistWithItems
import com.ticklog.domain.model.ChecklistItem
import com.ticklog.domain.model.ChecklistTemplate
import com.ticklog.domain.model.CompletionRecord
import com.ticklog.domain.model.DailyChecklist
import com.ticklog.domain.model.DailyTask
import com.ticklog.domain.model.DeletedTask

/**
 * Pure mapping functions translating Room entities into domain models.
 *
 * Keeping these as small, side-effect-free extensions in the data layer means
 * the persistence schema can change shape without rippling into the domain or
 * UI, and the mappings are trivial to unit-test in isolation.
 */

/** Maps a template row to its domain model. */
fun ChecklistTemplateEntity.toDomain(): ChecklistTemplate = ChecklistTemplate(
    id = id,
    name = name,
    description = description,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

/** Maps a template-item row to its domain model. */
fun ChecklistItemEntity.toDomain(): ChecklistItem = ChecklistItem(
    id = id,
    templateId = templateId,
    title = title,
    description = description,
    position = position,
)

/** Maps a per-day task row to its domain model. */
fun DailyChecklistItemEntity.toDomain(): DailyTask = DailyTask(
    id = id,
    title = title,
    note = note,
    position = position,
    isCompleted = isCompleted,
    completedAt = completedAt,
    isLinkedToTemplate = sourceItemId != null,
)

/** Maps a joined day-with-items relation to the aggregate domain model. */
fun DailyChecklistWithItems.toDomain(): DailyChecklist = DailyChecklist(
    id = checklist.id,
    templateId = checklist.templateId,
    date = checklist.date,
    tasks = items
        .sortedBy { it.position }
        .map { it.toDomain() },
)

/** Maps a completion-history row to its domain summary model. */
fun CompletionHistoryEntity.toDomain(): CompletionRecord = CompletionRecord(
    date = date,
    totalItems = totalItems,
    completedItems = completedItems,
)

/** Captures a task row as a [DeletedTask] undo snapshot before it is removed. */
fun DailyChecklistItemEntity.toDeletedTask(): DeletedTask = DeletedTask(
    id = id,
    dailyChecklistId = dailyChecklistId,
    sourceItemId = sourceItemId,
    title = title,
    note = note,
    position = position,
    isCompleted = isCompleted,
    completedAt = completedAt,
)

/** Rebuilds the exact original row from a [DeletedTask] snapshot for undo. */
fun DeletedTask.toEntity(): DailyChecklistItemEntity = DailyChecklistItemEntity(
    id = id,
    dailyChecklistId = dailyChecklistId,
    sourceItemId = sourceItemId,
    title = title,
    note = note,
    position = position,
    isCompleted = isCompleted,
    completedAt = completedAt,
)
