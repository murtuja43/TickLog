package com.ticklog.domain.model

import java.time.Instant

/**
 * Domain types and rules for creating and editing tasks.
 *
 * These keep the "what" of an edit (its content and reach) in the domain layer,
 * independent of Room or Compose, so the same rules drive the UI's validation
 * and the repository's persistence.
 */

/** Maximum allowed length of a task title, after trimming. */
const val MAX_TASK_TITLE_LENGTH: Int = 120

/** Maximum allowed length of a task note, after trimming. */
const val MAX_TASK_NOTE_LENGTH: Int = 500

/**
 * How far an edit reaches across the generated days.
 *
 * History integrity rule: [TODAY_AND_FUTURE] affects the reference day and every
 * later day, but **never** a day in the past.
 */
enum class TaskScope {
    /** Apply only to the single day being viewed. */
    TODAY_ONLY,

    /** Apply to the day being viewed and all later generated days. */
    TODAY_AND_FUTURE,
}

/**
 * A validated piece of task content (title + optional note) ready to persist.
 *
 * Instances are only created through [from], which trims whitespace and enforces
 * the length limits, so an invalid draft cannot exist.
 *
 * @property title non-blank, trimmed, at most [MAX_TASK_TITLE_LENGTH] chars.
 * @property note trimmed and at most [MAX_TASK_NOTE_LENGTH] chars, or null.
 */
data class TaskDraft private constructor(
    val title: String,
    val note: String?,
) {
    companion object {
        /**
         * Builds a [TaskDraft] from raw input, or returns null if the title is
         * blank. Whitespace is trimmed and over-long values are rejected by
         * returning null (the UI prevents over-typing, this is the safety net).
         */
        fun from(rawTitle: String, rawNote: String?): TaskDraft? {
            val title = rawTitle.trim()
            if (title.isEmpty() || title.length > MAX_TASK_TITLE_LENGTH) return null

            val note = rawNote?.trim()?.takeIf { it.isNotEmpty() }
            if (note != null && note.length > MAX_TASK_NOTE_LENGTH) return null

            return TaskDraft(title = title, note = note)
        }
    }
}

/**
 * A snapshot of a deleted task, sufficient to recreate the exact row on undo.
 *
 * Carrying the original [id] means a restore re-inserts at the same primary key,
 * keeping every foreign-key relationship and the task's completion state intact.
 */
data class DeletedTask(
    val id: Long,
    val dailyChecklistId: Long,
    val sourceItemId: Long?,
    val title: String,
    val note: String?,
    val position: Int,
    val isCompleted: Boolean,
    val completedAt: Instant?,
)
