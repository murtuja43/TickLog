package com.ticklog.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ticklog.data.database.entity.DailyChecklistItemEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/**
 * Data-access object for per-day tickable tasks ([DailyChecklistItemEntity]).
 *
 * The "from a given date forward" queries are the backbone of TickLog's history
 * integrity: edits and deletes scoped to "today and future" target only rows
 * whose owning day is on or after the reference date, so past days are never
 * touched. The date filter is expressed as a sub-query against `daily_checklists`
 * so the scoping happens atomically inside a single statement.
 */
@Dao
interface DailyChecklistItemDao {

    /** Inserts a single task, returning its generated id. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: DailyChecklistItemEntity): Long

    /**
     * Inserts the tasks generated for one or more days. Also used to restore a
     * previously-deleted set on undo, where the entities carry their original
     * ids so foreign keys and ordering are perfectly reconstructed.
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<DailyChecklistItemEntity>): List<Long>

    /** Observes the tasks of a given day, in their snapshot order. */
    @Query(
        "SELECT * FROM daily_checklist_items " +
            "WHERE daily_checklist_id = :dailyChecklistId ORDER BY position ASC",
    )
    fun observeItems(dailyChecklistId: Long): Flow<List<DailyChecklistItemEntity>>

    /** One-shot fetch of a single task by id. */
    @Query("SELECT * FROM daily_checklist_items WHERE id = :itemId")
    suspend fun getById(itemId: Long): DailyChecklistItemEntity?

    /**
     * Sets the completion state of a task and stamps/clears its completion time
     * atomically — the single source of truth for ticking an item.
     */
    @Query(
        "UPDATE daily_checklist_items " +
            "SET is_completed = :completed, completed_at = :completedAt " +
            "WHERE id = :itemId",
    )
    suspend fun setCompleted(itemId: Long, completed: Boolean, completedAt: Instant?)

    // --- Single-day edits ---------------------------------------------------

    /** Renames a single task (this day only). */
    @Query("UPDATE daily_checklist_items SET title = :title WHERE id = :itemId")
    suspend fun updateTitle(itemId: Long, title: String)

    /** Updates a single task's note (this day only). */
    @Query("UPDATE daily_checklist_items SET note = :note WHERE id = :itemId")
    suspend fun updateNote(itemId: Long, note: String?)

    /** Deletes a single task (this day only). */
    @Query("DELETE FROM daily_checklist_items WHERE id = :itemId")
    suspend fun deleteById(itemId: Long)

    /** Highest position currently used on a day, or null when the day is empty. */
    @Query("SELECT MAX(position) FROM daily_checklist_items WHERE daily_checklist_id = :dailyChecklistId")
    suspend fun maxPosition(dailyChecklistId: Long): Int?

    // --- "Today and future" edits (past days untouched) ---------------------

    /** Renames every instance of a source item on days on/after [fromDate]. */
    @Query(
        "UPDATE daily_checklist_items SET title = :title " +
            "WHERE source_item_id = :sourceItemId AND daily_checklist_id IN " +
            "(SELECT id FROM daily_checklists WHERE date >= :fromDate)",
    )
    suspend fun updateTitleForSourceFromDate(sourceItemId: Long, fromDate: LocalDate, title: String)

    /** Updates the note of every instance of a source item on days on/after [fromDate]. */
    @Query(
        "UPDATE daily_checklist_items SET note = :note " +
            "WHERE source_item_id = :sourceItemId AND daily_checklist_id IN " +
            "(SELECT id FROM daily_checklists WHERE date >= :fromDate)",
    )
    suspend fun updateNoteForSourceFromDate(sourceItemId: Long, fromDate: LocalDate, note: String?)

    /** Reads every instance of a source item on days on/after [fromDate] (for undo capture). */
    @Query(
        "SELECT * FROM daily_checklist_items " +
            "WHERE source_item_id = :sourceItemId AND daily_checklist_id IN " +
            "(SELECT id FROM daily_checklists WHERE date >= :fromDate)",
    )
    suspend fun getBySourceFromDate(
        sourceItemId: Long,
        fromDate: LocalDate,
    ): List<DailyChecklistItemEntity>

    /** Deletes every instance of a source item on days on/after [fromDate]. */
    @Query(
        "DELETE FROM daily_checklist_items " +
            "WHERE source_item_id = :sourceItemId AND daily_checklist_id IN " +
            "(SELECT id FROM daily_checklists WHERE date >= :fromDate)",
    )
    suspend fun deleteBySourceFromDate(sourceItemId: Long, fromDate: LocalDate)

    /** All task rows — used to serialise a full backup. */
    @Query("SELECT * FROM daily_checklist_items")
    suspend fun getAll(): List<DailyChecklistItemEntity>
}
