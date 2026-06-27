package com.ticklog.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ticklog.data.database.entity.DailyChecklistEntity
import com.ticklog.data.database.relation.DailyChecklistWithItems
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Data-access object for [DailyChecklistEntity] and its joined item reads.
 *
 * The `IGNORE` conflict strategy on insert pairs with the unique (template, date)
 * index to make day generation idempotent: re-inserting an existing day is a
 * no-op rather than an error.
 */
@Dao
interface DailyChecklistDao {

    /** Inserts a day's checklist; ignored if that (template, date) already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(checklist: DailyChecklistEntity): Long

    /** Inserts many days at once during range generation. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(checklists: List<DailyChecklistEntity>): List<Long>

    /** Observes a single day (with its items) for a template, or null. */
    @Transaction
    @Query(
        "SELECT * FROM daily_checklists WHERE template_id = :templateId AND date = :date LIMIT 1",
    )
    fun observeChecklistWithItems(
        templateId: Long,
        date: LocalDate,
    ): Flow<DailyChecklistWithItems?>

    /** Observes all days (with items) in an inclusive date range, chronologically. */
    @Transaction
    @Query(
        "SELECT * FROM daily_checklists " +
            "WHERE template_id = :templateId AND date BETWEEN :start AND :end " +
            "ORDER BY date ASC",
    )
    fun observeChecklistsInRange(
        templateId: Long,
        start: LocalDate,
        end: LocalDate,
    ): Flow<List<DailyChecklistWithItems>>

    /** One-shot lookup of a day's row id, used by generation/repair routines. */
    @Query(
        "SELECT id FROM daily_checklists WHERE template_id = :templateId AND date = :date LIMIT 1",
    )
    suspend fun findChecklistId(templateId: Long, date: LocalDate): Long?
}
