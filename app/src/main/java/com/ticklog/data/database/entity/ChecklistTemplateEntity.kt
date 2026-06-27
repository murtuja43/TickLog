package com.ticklog.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A reusable checklist definition — the "blueprint" a user fills in once and from
 * which concrete [DailyChecklistEntity] instances are generated for each date.
 *
 * Phase 1 only ever creates a single active template, but modelling templates as
 * first-class rows (with [isActive]) means supporting multiple named templates
 * later requires no schema migration — only new queries.
 */
@Entity(tableName = "checklist_templates")
data class ChecklistTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    /** Human-readable name, e.g. "Morning routine". */
    @ColumnInfo(name = "name")
    val name: String,

    /** Optional longer description of the template's purpose. */
    @ColumnInfo(name = "description")
    val description: String? = null,

    /** Whether this template is the one currently driving daily generation. */
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    /** Audit timestamp: when the template was first created. */
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,

    /** Audit timestamp: when the template was last edited. */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
)
