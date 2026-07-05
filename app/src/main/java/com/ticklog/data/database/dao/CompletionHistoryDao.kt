package com.ticklog.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ticklog.data.database.entity.CompletionHistoryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Data-access object for the per-day completion ledger ([CompletionHistoryEntity])
 * that backs the history timeline and statistics screens.
 */
@Dao
interface CompletionHistoryDao {

    /** Inserts or replaces the summary row for a day (keyed by daily checklist). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: CompletionHistoryEntity)

    /** Observes the full history, most recent day first. */
    @Query("SELECT * FROM completion_history ORDER BY date DESC")
    fun observeAll(): Flow<List<CompletionHistoryEntity>>

    /** Observes history within an inclusive date range, chronologically. */
    @Query(
        "SELECT * FROM completion_history WHERE date BETWEEN :start AND :end ORDER BY date ASC",
    )
    fun observeRange(start: LocalDate, end: LocalDate): Flow<List<CompletionHistoryEntity>>

    /**
     * Observes the number of days on which every task was completed — a
     * fully-completed-day count used by the statistics screen.
     */
    @Query(
        "SELECT COUNT(*) FROM completion_history " +
            "WHERE total_items > 0 AND completed_items >= total_items",
    )
    fun observePerfectDayCount(): Flow<Int>

    /** All ledger rows — used to serialise a full backup. */
    @Query("SELECT * FROM completion_history")
    suspend fun getAll(): List<CompletionHistoryEntity>

    /** Deletes every ledger row — used to clear data during an atomic restore. */
    @Query("DELETE FROM completion_history")
    suspend fun deleteAll()

    /** Bulk insert preserving ids — used to restore a full backup. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<CompletionHistoryEntity>)
}
