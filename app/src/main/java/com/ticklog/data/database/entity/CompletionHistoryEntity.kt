package com.ticklog.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * The permanent, per-day completion ledger that powers history and statistics.
 *
 * One row is written/updated per [DailyChecklistEntity] capturing how many of
 * that day's items were completed. It exists as its own table (rather than being
 * recomputed from [DailyChecklistItemEntity] every time) so statistics and the
 * history timeline — which scan across potentially years of days — stay fast and
 * can be queried without loading every individual task.
 *
 * Note on normalisation: we store [totalItems] and [completedItems] only. The
 * completion *rate* is derived in the domain layer rather than stored, so no
 * redundant, drift-prone value is persisted.
 */
@Entity(
    tableName = "completion_history",
    foreignKeys = [
        ForeignKey(
            entity = DailyChecklistEntity::class,
            parentColumns = ["id"],
            childColumns = ["daily_checklist_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["daily_checklist_id"], unique = true),
        Index(value = ["date"]),
    ],
)
data class CompletionHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    /** The daily checklist this record summarises. */
    @ColumnInfo(name = "daily_checklist_id")
    val dailyChecklistId: Long,

    /** Denormalised date (epoch day) for fast range queries without a join. */
    @ColumnInfo(name = "date")
    val date: LocalDate,

    /** Total number of tasks that existed on the day. */
    @ColumnInfo(name = "total_items")
    val totalItems: Int,

    /** Number of those tasks the user completed. */
    @ColumnInfo(name = "completed_items")
    val completedItems: Int,

    /** Audit timestamp: when this summary row was last updated. */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
)
