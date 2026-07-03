package com.ticklog.domain.repository

import com.ticklog.domain.model.ChecklistTemplate
import com.ticklog.domain.model.CompletionRecord
import com.ticklog.domain.model.DailyChecklist
import com.ticklog.domain.model.DateRange
import com.ticklog.domain.model.DeletedTask
import com.ticklog.domain.model.TaskDraft
import com.ticklog.domain.model.TaskOccurrence
import com.ticklog.domain.model.TaskScope
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Abstraction over checklist data — the heart of TickLog.
 *
 * Reads are reactive [Flow]s; writes are `suspend` and transactional. The write
 * surface encodes the product's history-integrity rules directly: every "future"
 * edit is bounded so past days are immutable, and generation is idempotent.
 */
interface ChecklistRepository {

    // --- Reads --------------------------------------------------------------

    /** Observes the active checklist template, or null if none exists yet. */
    fun observeActiveTemplate(): Flow<ChecklistTemplate?>

    /**
     * Observes the checklist for [date] under the active template, or null when
     * no template/day exists. Re-evaluates as the active template or day changes.
     */
    fun observeChecklistForDate(date: LocalDate): Flow<DailyChecklist?>

    /**
     * Observes a lightweight per-day completion summary for every generated day,
     * in chronological order — the shared feed behind the calendar, history and
     * statistics. Emits an empty list when no template exists.
     */
    fun observeDaySummaries(): Flow<List<CompletionRecord>>

    /**
     * Observes every template-linked task occurrence (task × day), the raw input
     * for per-task insights. Emits an empty list when no template exists.
     */
    fun observeTaskOccurrences(): Flow<List<TaskOccurrence>>

    /**
     * Observes the distinct dates whose tasks match [query] in title or note,
     * most recent first. A blank query yields an empty list.
     */
    fun searchDates(query: String): Flow<List<LocalDate>>

    /**
     * One-shot fetch of every day (with its tasks) in the inclusive range, in
     * chronological order — used to assemble a PDF report. Empty when no template.
     */
    suspend fun getChecklistsInRange(start: LocalDate, end: LocalDate): List<DailyChecklist>

    // --- Creation -----------------------------------------------------------

    /**
     * Creates the one-and-only checklist: a template with [drafts] as its items,
     * an independent [com.ticklog.domain.model.DailyChecklist] for every date in
     * [range], and an independent copy of every item on each of those days.
     *
     * Runs in a single transaction and is safe to treat as all-or-nothing.
     *
     * @return the generated template id.
     */
    suspend fun createChecklist(
        name: String,
        range: DateRange,
        drafts: List<TaskDraft>,
    ): Long

    // --- Per-task mutations -------------------------------------------------

    /** Sets a task's completion state, stamping the completion time. */
    suspend fun setTaskCompleted(dailyTaskId: Long, completed: Boolean)

    /**
     * Adds a new task on [date]. [TaskScope.TODAY_AND_FUTURE] also creates a
     * canonical template item and copies the task onto every later generated day;
     * previous days are never modified.
     */
    suspend fun addTask(date: LocalDate, draft: TaskDraft, scope: TaskScope)

    /**
     * Renames the task identified by [dailyTaskId]. With
     * [TaskScope.TODAY_AND_FUTURE] the new title is applied to this day and every
     * later day that shares the task's source item; past days keep their title.
     */
    suspend fun renameTask(dailyTaskId: Long, newTitle: String, scope: TaskScope)

    /** Updates a task's note, with the same scoping semantics as [renameTask]. */
    suspend fun updateTaskNote(dailyTaskId: Long, newNote: String?, scope: TaskScope)

    /**
     * Duplicates a task on its own day only, appended at the end. The copy is
     * standalone (not linked to a template item), returning its new id.
     */
    suspend fun duplicateTask(dailyTaskId: Long): Long

    /**
     * Deletes the task identified by [dailyTaskId]. With
     * [TaskScope.TODAY_AND_FUTURE] every later instance sharing its source item is
     * removed too; past days are untouched.
     *
     * @return snapshots of every removed row, for [restoreTasks] (undo).
     */
    suspend fun deleteTask(dailyTaskId: Long, scope: TaskScope): List<DeletedTask>

    /** Restores previously [deleted] tasks exactly, re-inserting at their ids. */
    suspend fun restoreTasks(deleted: List<DeletedTask>)
}
