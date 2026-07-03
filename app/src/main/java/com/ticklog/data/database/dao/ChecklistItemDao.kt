package com.ticklog.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ticklog.data.database.entity.ChecklistItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data-access object for template [ChecklistItemEntity] definitions.
 */
@Dao
interface ChecklistItemDao {

    /** Inserts a single item, returning its generated id. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: ChecklistItemEntity): Long

    /** Inserts several items at once (e.g. when seeding a template). */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<ChecklistItemEntity>): List<Long>

    /** Updates an existing item. */
    @Update
    suspend fun update(item: ChecklistItemEntity)

    /** Observes the active (non-archived) items of a template, in order. */
    @Query(
        "SELECT * FROM checklist_items " +
            "WHERE template_id = :templateId AND is_archived = 0 " +
            "ORDER BY position ASC",
    )
    fun observeItemsForTemplate(templateId: Long): Flow<List<ChecklistItemEntity>>

    /** One-shot fetch of the active items of a template, in order. */
    @Query(
        "SELECT * FROM checklist_items " +
            "WHERE template_id = :templateId AND is_archived = 0 " +
            "ORDER BY position ASC",
    )
    suspend fun getActiveItemsForTemplate(templateId: Long): List<ChecklistItemEntity>

    /** One-shot fetch of a template item by id. */
    @Query("SELECT * FROM checklist_items WHERE id = :itemId")
    suspend fun getById(itemId: Long): ChecklistItemEntity?

    /** Highest position used in a template, or null when it has no items. */
    @Query("SELECT MAX(position) FROM checklist_items WHERE template_id = :templateId")
    suspend fun maxPosition(templateId: Long): Int?

    /** Renames the canonical template item (used when propagating to future days). */
    @Query("UPDATE checklist_items SET title = :title WHERE id = :itemId")
    suspend fun updateTitle(itemId: Long, title: String)

    /** Updates the canonical template item's note. */
    @Query("UPDATE checklist_items SET description = :description WHERE id = :itemId")
    suspend fun updateDescription(itemId: Long, description: String?)

    /** Archives an item (soft delete) so history that references it is preserved. */
    @Query("UPDATE checklist_items SET is_archived = 1 WHERE id = :itemId")
    suspend fun archive(itemId: Long)

    /** All template items — used to serialise a full backup. */
    @Query("SELECT * FROM checklist_items")
    suspend fun getAll(): List<ChecklistItemEntity>
}
