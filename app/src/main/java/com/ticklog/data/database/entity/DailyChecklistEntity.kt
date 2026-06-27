package com.ticklog.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * A concrete checklist for one specific calendar [date], generated from a
 * [ChecklistTemplateEntity].
 *
 * The composite unique index on (template_id, date) enforces the core invariant
 * that a template produces **at most one** checklist per day — generation can be
 * run repeatedly (idempotently) without creating duplicates.
 */
@Entity(
    tableName = "daily_checklists",
    foreignKeys = [
        ForeignKey(
            entity = ChecklistTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["template_id", "date"], unique = true),
        Index(value = ["date"]),
    ],
)
data class DailyChecklistEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    /** Template this checklist was generated from. */
    @ColumnInfo(name = "template_id")
    val templateId: Long,

    /** The calendar date this checklist belongs to (stored as epoch day). */
    @ColumnInfo(name = "date")
    val date: LocalDate,

    /** Audit timestamp: when this day's checklist was generated. */
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
)
