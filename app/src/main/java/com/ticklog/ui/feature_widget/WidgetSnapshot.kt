package com.ticklog.ui.feature_widget

import android.content.Context
import com.ticklog.domain.calculator.StreakCalculator
import com.ticklog.domain.model.CompletionRecord
import com.ticklog.domain.model.DailyChecklist
import com.ticklog.domain.repository.ChecklistRepository
import dagger.hilt.android.EntryPointAccessors
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * An immutable, self-contained snapshot of everything the widget renders for a
 * single update. It is computed once per [TickLogWidget.provideGlance] pass so
 * the Glance composition is a pure function of this value — no data access
 * happens during composition.
 */
data class WidgetSnapshot(
    val date: LocalDate,
    val hasChecklist: Boolean,
    val tasks: List<WidgetTask>,
    val completedCount: Int,
    val totalCount: Int,
    val streak: Int,
) {
    /** Completion percentage in 0..100; 0 when the day has no tasks. */
    val completionPercent: Int
        get() = if (totalCount == 0) 0 else (completedCount * 100f / totalCount).roundToInt()

    /** True when there are tasks and every one of them is complete. */
    val isFullyCompleted: Boolean get() = totalCount > 0 && completedCount == totalCount

    companion object {
        /** The state shown before onboarding, or when today has no checklist. */
        fun empty(date: LocalDate, streak: Int = 0): WidgetSnapshot =
            WidgetSnapshot(date, hasChecklist = false, emptyList(), 0, 0, streak)
    }
}

/** One tickable row on the widget. */
data class WidgetTask(
    val id: Long,
    val title: String,
    val isCompleted: Boolean,
)

/**
 * Builds today's snapshot from the latest values of the two repository flows.
 *
 * This is a pure function so the widget's Glance composition can observe the
 * flows directly (via `collectAsState`) and rebuild reactively: whenever a task
 * is toggled, edited, deleted or restored — in the app or the widget — Room
 * re-emits and the widget recomposes with fresh data.
 *
 * @param today the reference day (captured once per Glance session).
 * @param checklist today's checklist, or null when none exists / still loading.
 * @param summaries every day's completion summary, for the streak.
 */
fun buildWidgetSnapshot(
    today: LocalDate,
    checklist: DailyChecklist?,
    summaries: List<CompletionRecord>,
): WidgetSnapshot {
    val streak = StreakCalculator.currentStreak(summaries, today)
    if (checklist == null) return WidgetSnapshot.empty(today, streak)
    return WidgetSnapshot(
        date = today,
        hasChecklist = true,
        tasks = checklist.tasks.map { WidgetTask(it.id, it.title, it.isCompleted) },
        completedCount = checklist.completedCount,
        totalCount = checklist.totalCount,
        streak = streak,
    )
}

/** Resolves the shared repository from the application's Hilt graph. */
fun widgetRepository(context: Context): ChecklistRepository =
    EntryPointAccessors.fromApplication(
        context.applicationContext,
        WidgetEntryPoint::class.java,
    ).checklistRepository()
