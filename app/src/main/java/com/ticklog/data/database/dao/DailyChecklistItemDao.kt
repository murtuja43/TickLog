package com.ticklog.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ticklog.data.database.entity.DailyChecklistItemEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Data-access object for per-day tickable tasks ([DailyChecklistItemEntity]).
 */
@Dao
interface DailyChecklistItemDao {

    /** Inserts the tasks generated for a day. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<DailyChecklistItemEntity>): List<Long>

    /** Observes the tasks of a given day, in their snapshot order. */
    @Query(
        "SELECT * FROM daily_checklist_items " +
            "WHERE daily_checklist_id = :dailyChecklistId ORDER BY position ASC",
    )
    fun observeItems(dailyChecklistId: Long): Flow<List<DailyChecklistItemEntity>>

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

    /** Counts how many tasks exist for a day. */
    @Query("SELECT COUNT(*) FROM daily_checklist_items WHERE daily_checklist_id = :dailyChecklistId")
    suspend fun countItems(dailyChecklistId: Long): Int

    /** Counts how many tasks are completed for a day. */
    @Query(
        "SELECT COUNT(*) FROM daily_checklist_items " +
            "WHERE daily_checklist_id = :dailyChecklistId AND is_completed = 1",
    )
    suspend fun countCompleted(dailyChecklistId: Long): Int
}
