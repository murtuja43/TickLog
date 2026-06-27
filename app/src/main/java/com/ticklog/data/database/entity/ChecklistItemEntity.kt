package com.ticklog.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A single item belonging to a [ChecklistTemplateEntity] — the definition of one
 * task the user wants to track each day (e.g. "Drink water").
 *
 * Items are **soft-deleted** via [isArchived] rather than removed, because a
 * deleted item may still be referenced by historical daily checklists. The
 * foreign key cascades on template deletion, so removing a whole template cleans
 * up its item definitions in one step.
 *
 * [position] gives the template a stable, user-defined ordering.
 */
@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = ChecklistTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["template_id"])],
)
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    /** Owning template. */
    @ColumnInfo(name = "template_id")
    val templateId: Long,

    /** Short task label shown to the user. */
    @ColumnInfo(name = "title")
    val title: String,

    /** Optional supporting detail for the task. */
    @ColumnInfo(name = "description")
    val description: String? = null,

    /** Zero-based ordering within the template. */
    @ColumnInfo(name = "position")
    val position: Int,

    /** Soft-delete flag; archived items stop generating into new days. */
    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    /** Audit timestamp: when the item was created. */
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
)
