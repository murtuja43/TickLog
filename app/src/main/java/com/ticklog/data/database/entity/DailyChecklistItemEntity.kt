package com.ticklog.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A single tickable task within a [DailyChecklistEntity] — the per-day instance
 * of a template [ChecklistItemEntity].
 *
 * Design note — intentional snapshotting: [title] and [position] are copied from
 * the source item at generation time rather than always joined live. This is a
 * deliberate, documented denormalisation in service of the product's "preserve
 * history forever" guarantee: editing or archiving a template item must never
 * retroactively rewrite what a past day looked like. [sourceItemId] is kept
 * (nullable, set null on source deletion) purely for analytics linkage.
 */
@Entity(
    tableName = "daily_checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = DailyChecklistEntity::class,
            parentColumns = ["id"],
            childColumns = ["daily_checklist_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ChecklistItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_item_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["daily_checklist_id"]),
        Index(value = ["source_item_id"]),
    ],
)
data class DailyChecklistItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    /** Owning daily checklist. */
    @ColumnInfo(name = "daily_checklist_id")
    val dailyChecklistId: Long,

    /** The template item this was generated from; null if that item was deleted. */
    @ColumnInfo(name = "source_item_id")
    val sourceItemId: Long? = null,

    /** Snapshot of the item's title at generation time (see class note). */
    @ColumnInfo(name = "title")
    val title: String,

    /** Snapshot of the item's optional note at generation time. */
    @ColumnInfo(name = "note")
    val note: String? = null,

    /** Snapshot of the item's ordering at generation time. */
    @ColumnInfo(name = "position")
    val position: Int,

    /** Whether the user has completed this task for the day. */
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    /** When the task was completed; null while incomplete. */
    @ColumnInfo(name = "completed_at")
    val completedAt: Instant? = null,
)
