package com.ticklog.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ticklog.data.database.entity.DailyChecklistEntity
import com.ticklog.data.database.relation.DailyChecklistWithItems
import com.ticklog.data.database.relation.DaySummaryRow
import com.ticklog.data.database.relation.TaskOccurrenceRow
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

    /** The calendar date of a given day row, used to scope "future" edits. */
    @Query("SELECT date FROM daily_checklists WHERE id = :dailyChecklistId LIMIT 1")
    suspend fun getDate(dailyChecklistId: Long): LocalDate?

    /** All day rows for a template on or after [fromDate], used to fan out new tasks. */
    @Query(
        "SELECT * FROM daily_checklists " +
            "WHERE template_id = :templateId AND date >= :fromDate ORDER BY date ASC",
    )
    suspend fun getChecklistsFromDate(
        templateId: Long,
        fromDate: LocalDate,
    ): List<DailyChecklistEntity>

    /**
     * One [DaySummaryRow] per generated day (chronological), aggregated entirely
     * in SQL. A `LEFT JOIN` keeps days that have had all their tasks removed
     * (total = 0). This single query powers the calendar, history and statistics.
     */
    @Query(
        "SELECT dc.date AS date, " +
            "COUNT(dci.id) AS totalItems, " +
            "COALESCE(SUM(dci.is_completed), 0) AS completedItems " +
            "FROM daily_checklists dc " +
            "LEFT JOIN daily_checklist_items dci ON dci.daily_checklist_id = dc.id " +
            "WHERE dc.template_id = :templateId " +
            "GROUP BY dc.id " +
            "ORDER BY dc.date ASC",
    )
    fun observeDaySummaries(templateId: Long): Flow<List<DaySummaryRow>>

    /**
     * Every template-linked task occurrence (one row per task per day), ordered
     * by task then date — the raw feed for per-task insight calculations.
     */
    @Query(
        "SELECT dci.source_item_id AS sourceItemId, dci.title AS title, " +
            "dc.date AS date, dci.is_completed AS isCompleted " +
            "FROM daily_checklist_items dci " +
            "JOIN daily_checklists dc ON dc.id = dci.daily_checklist_id " +
            "WHERE dc.template_id = :templateId AND dci.source_item_id IS NOT NULL " +
            "ORDER BY dci.source_item_id ASC, dc.date ASC",
    )
    fun observeTaskOccurrences(templateId: Long): Flow<List<TaskOccurrenceRow>>

    /**
     * Distinct dates (most recent first) whose tasks match [query] in title or
     * note — a fast, index-backed history search performed in the database.
     */
    @Query(
        "SELECT DISTINCT dc.date FROM daily_checklists dc " +
            "JOIN daily_checklist_items dci ON dci.daily_checklist_id = dc.id " +
            "WHERE dc.template_id = :templateId AND (" +
            "dci.title LIKE '%' || :query || '%' OR dci.note LIKE '%' || :query || '%') " +
            "ORDER BY dc.date DESC",
    )
    fun searchDates(templateId: Long, query: String): Flow<List<LocalDate>>

    /**
     * All days (with their items) for a template within an inclusive range,
     * fetched once (not observed) for building a PDF report.
     */
    @Transaction
    @Query(
        "SELECT * FROM daily_checklists " +
            "WHERE template_id = :templateId AND date BETWEEN :start AND :end " +
            "ORDER BY date ASC",
    )
    suspend fun getChecklistsWithItemsInRange(
        templateId: Long,
        start: LocalDate,
        end: LocalDate,
    ): List<DailyChecklistWithItems>

    /** All day rows — used to serialise a full backup. */
    @Query("SELECT * FROM daily_checklists")
    suspend fun getAll(): List<DailyChecklistEntity>

    /** Deletes every day row — used to clear data during an atomic restore. */
    @Query("DELETE FROM daily_checklists")
    suspend fun deleteAll()
}
