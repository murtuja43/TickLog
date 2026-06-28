package com.ticklog.data.repository

import androidx.room.withTransaction
import com.ticklog.data.database.TickLogDatabase
import com.ticklog.data.database.dao.ChecklistItemDao
import com.ticklog.data.database.dao.ChecklistTemplateDao
import com.ticklog.data.database.dao.DailyChecklistDao
import com.ticklog.data.database.dao.DailyChecklistItemDao
import com.ticklog.data.database.entity.ChecklistItemEntity
import com.ticklog.data.database.entity.ChecklistTemplateEntity
import com.ticklog.data.database.entity.DailyChecklistEntity
import com.ticklog.data.database.entity.DailyChecklistItemEntity
import com.ticklog.data.model.toDeletedTask
import com.ticklog.data.model.toDomain
import com.ticklog.data.model.toEntity
import com.ticklog.domain.model.ChecklistTemplate
import com.ticklog.domain.model.DailyChecklist
import com.ticklog.domain.model.DateRange
import com.ticklog.domain.model.DeletedTask
import com.ticklog.domain.model.TaskDraft
import com.ticklog.domain.model.TaskScope
import com.ticklog.domain.repository.ChecklistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed implementation of [ChecklistRepository] — the checklist engine.
 *
 * Every mutating method that touches more than one row runs inside a single
 * [withTransaction] block so the database can never be left half-updated. The
 * "today and future" operations rely on date-scoped DAO queries to guarantee
 * that past days are immutable, which is the foundation of TickLog's promise to
 * preserve history forever.
 */
@Singleton
class ChecklistRepositoryImpl @Inject constructor(
    private val database: TickLogDatabase,
    private val templateDao: ChecklistTemplateDao,
    private val checklistItemDao: ChecklistItemDao,
    private val dailyChecklistDao: DailyChecklistDao,
    private val dailyChecklistItemDao: DailyChecklistItemDao,
) : ChecklistRepository {

    // --- Reads --------------------------------------------------------------

    override fun observeActiveTemplate(): Flow<ChecklistTemplate?> =
        templateDao.observeActiveTemplate().map { it?.toDomain() }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeChecklistForDate(date: LocalDate): Flow<DailyChecklist?> =
        templateDao.observeActiveTemplate().flatMapLatest { template ->
            if (template == null) {
                flowOf(null)
            } else {
                dailyChecklistDao
                    .observeChecklistWithItems(templateId = template.id, date = date)
                    .map { relation -> relation?.toDomain() }
            }
        }

    // --- Creation -----------------------------------------------------------

    override suspend fun createChecklist(
        name: String,
        range: DateRange,
        drafts: List<TaskDraft>,
    ): Long = database.withTransaction {
        val now = Instant.now()

        val templateId = templateDao.insert(
            ChecklistTemplateEntity(
                name = name,
                description = null,
                isActive = true,
                createdAt = now,
                updatedAt = now,
            ),
        )

        // Template item definitions, positioned in the order the user arranged them.
        val itemEntities = drafts.mapIndexed { index, draft ->
            ChecklistItemEntity(
                templateId = templateId,
                title = draft.title,
                description = draft.note,
                position = index,
                createdAt = now,
            )
        }
        val itemIds = checklistItemDao.insertAll(itemEntities)

        // One independent day per date, each with its own independent copy of
        // every item — so editing one day can never affect another.
        val dailyItems = mutableListOf<DailyChecklistItemEntity>()
        for (date in range.datesInclusive()) {
            val dailyChecklistId = dailyChecklistDao.insert(
                DailyChecklistEntity(templateId = templateId, date = date, createdAt = now),
            )
            itemEntities.forEachIndexed { index, item ->
                dailyItems += DailyChecklistItemEntity(
                    dailyChecklistId = dailyChecklistId,
                    sourceItemId = itemIds[index],
                    title = item.title,
                    note = item.description,
                    position = item.position,
                    isCompleted = false,
                    completedAt = null,
                )
            }
        }
        dailyChecklistItemDao.insertAll(dailyItems)

        templateId
    }

    // --- Per-task mutations -------------------------------------------------

    override suspend fun setTaskCompleted(dailyTaskId: Long, completed: Boolean) {
        dailyChecklistItemDao.setCompleted(
            itemId = dailyTaskId,
            completed = completed,
            completedAt = if (completed) Instant.now() else null,
        )
    }

    override suspend fun addTask(date: LocalDate, draft: TaskDraft, scope: TaskScope) {
        val template = templateDao.observeActiveTemplate().first() ?: return
        database.withTransaction {
            when (scope) {
                TaskScope.TODAY_ONLY -> {
                    val dailyChecklistId =
                        dailyChecklistDao.findChecklistId(template.id, date)
                            ?: return@withTransaction
                    dailyChecklistItemDao.insert(
                        newTaskEntity(dailyChecklistId, sourceItemId = null, draft),
                    )
                }

                TaskScope.TODAY_AND_FUTURE -> {
                    // A canonical template item is created so the addition is part
                    // of the checklist's definition, then fanned out to each day.
                    val templatePosition = (checklistItemDao.maxPosition(template.id) ?: -1) + 1
                    val itemId = checklistItemDao.insert(
                        ChecklistItemEntity(
                            templateId = template.id,
                            title = draft.title,
                            description = draft.note,
                            position = templatePosition,
                            createdAt = Instant.now(),
                        ),
                    )
                    val days = dailyChecklistDao.getChecklistsFromDate(template.id, date)
                    for (day in days) {
                        dailyChecklistItemDao.insert(
                            newTaskEntity(day.id, sourceItemId = itemId, draft),
                        )
                    }
                }
            }
        }
    }

    override suspend fun renameTask(dailyTaskId: Long, newTitle: String, scope: TaskScope) {
        val title = newTitle.trim()
        if (title.isEmpty()) return
        database.withTransaction {
            val task = dailyChecklistItemDao.getById(dailyTaskId) ?: return@withTransaction
            val sourceId = task.sourceItemId
            if (scope == TaskScope.TODAY_AND_FUTURE && sourceId != null) {
                val fromDate = dailyChecklistDao.getDate(task.dailyChecklistId)
                    ?: return@withTransaction
                dailyChecklistItemDao.updateTitleForSourceFromDate(sourceId, fromDate, title)
                checklistItemDao.updateTitle(sourceId, title)
            } else {
                // Single day, or a standalone task with no template source.
                dailyChecklistItemDao.updateTitle(dailyTaskId, title)
            }
        }
    }

    override suspend fun updateTaskNote(dailyTaskId: Long, newNote: String?, scope: TaskScope) {
        val note = newNote?.trim()?.takeIf { it.isNotEmpty() }
        database.withTransaction {
            val task = dailyChecklistItemDao.getById(dailyTaskId) ?: return@withTransaction
            val sourceId = task.sourceItemId
            if (scope == TaskScope.TODAY_AND_FUTURE && sourceId != null) {
                val fromDate = dailyChecklistDao.getDate(task.dailyChecklistId)
                    ?: return@withTransaction
                dailyChecklistItemDao.updateNoteForSourceFromDate(sourceId, fromDate, note)
                checklistItemDao.updateDescription(sourceId, note)
            } else {
                dailyChecklistItemDao.updateNote(dailyTaskId, note)
            }
        }
    }

    override suspend fun duplicateTask(dailyTaskId: Long): Long = database.withTransaction {
        val task = dailyChecklistItemDao.getById(dailyTaskId) ?: return@withTransaction -1L
        val position = (dailyChecklistItemDao.maxPosition(task.dailyChecklistId) ?: -1) + 1
        dailyChecklistItemDao.insert(
            task.copy(
                id = 0L,
                sourceItemId = null, // a standalone copy on this day only
                position = position,
                isCompleted = false,
                completedAt = null,
            ),
        )
    }

    override suspend fun deleteTask(
        dailyTaskId: Long,
        scope: TaskScope,
    ): List<DeletedTask> = database.withTransaction {
        val task = dailyChecklistItemDao.getById(dailyTaskId) ?: return@withTransaction emptyList()
        val sourceId = task.sourceItemId
        if (scope == TaskScope.TODAY_AND_FUTURE && sourceId != null) {
            val fromDate = dailyChecklistDao.getDate(task.dailyChecklistId)
                ?: return@withTransaction emptyList()
            val removed = dailyChecklistItemDao.getBySourceFromDate(sourceId, fromDate)
                .map { it.toDeletedTask() }
            dailyChecklistItemDao.deleteBySourceFromDate(sourceId, fromDate)
            removed
        } else {
            val removed = listOf(task.toDeletedTask())
            dailyChecklistItemDao.deleteById(dailyTaskId)
            removed
        }
    }

    override suspend fun restoreTasks(deleted: List<DeletedTask>) {
        if (deleted.isEmpty()) return
        database.withTransaction {
            dailyChecklistItemDao.insertAll(deleted.map { it.toEntity() })
        }
    }

    /** Builds a fresh, incomplete task appended at the end of its day. */
    private suspend fun newTaskEntity(
        dailyChecklistId: Long,
        sourceItemId: Long?,
        draft: TaskDraft,
    ): DailyChecklistItemEntity {
        val position = (dailyChecklistItemDao.maxPosition(dailyChecklistId) ?: -1) + 1
        return DailyChecklistItemEntity(
            dailyChecklistId = dailyChecklistId,
            sourceItemId = sourceItemId,
            title = draft.title,
            note = draft.note,
            position = position,
            isCompleted = false,
            completedAt = null,
        )
    }
}
