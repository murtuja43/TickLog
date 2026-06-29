package com.ticklog.data.database.relation

import java.time.LocalDate

/**
 * Lightweight projections used by the productivity features (calendar, history,
 * statistics). They are computed by aggregate SQL rather than loaded as full
 * entities, so the screens can summarise thousands of days cheaply.
 */

/**
 * A per-day completion roll-up: one row per generated day with its task totals.
 * Produced by a `GROUP BY` over the day's items, so it never loads the items
 * themselves.
 *
 * @property date the day.
 * @property totalItems number of tasks that exist on the day.
 * @property completedItems how many of them are completed.
 */
data class DaySummaryRow(
    val date: LocalDate,
    val totalItems: Int,
    val completedItems: Int,
)

/**
 * One occurrence of a template-linked task on a particular day — the raw input
 * for per-task insight calculations.
 *
 * @property sourceItemId the template item this occurrence belongs to.
 * @property title the task's title on that day.
 * @property date the day.
 * @property isCompleted whether it was completed that day.
 */
data class TaskOccurrenceRow(
    val sourceItemId: Long,
    val title: String,
    val date: LocalDate,
    val isCompleted: Boolean,
)
