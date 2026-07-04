package com.ticklog.ui.feature_widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import com.ticklog.MainActivity
import com.ticklog.R
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * The TickLog home-screen widget: a glanceable window onto today's checklist.
 *
 * It renders today's date, completion percentage, the day's tasks (each a live
 * toggle) and the current streak, in a strictly black-and-white style that
 * mirrors the app. A single [SizeMode.Responsive] declaration lets one
 * composition adapt cleanly across 2×2, 4×2 and 4×4 placements — Glance re-runs
 * the content with [LocalSize] pinned to the closest declared size, so each
 * layout is chosen deterministically rather than measured at runtime.
 */
class TickLogWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(SMALL, WIDE, LARGE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = widgetRepository(context)
        // "Today" is fixed for the life of a session; the date-changed broadcast
        // starts a fresh session (with a new value) at midnight.
        val today = LocalDate.now()
        // Seed the first frame with the current values so there is no loading
        // flash, then keep observing so the composition rebuilds reactively on
        // every checklist change — the toggle, an in-app edit, a restore, etc.
        val initialChecklist = repository.observeChecklistForDate(today).first()
        val initialSummaries = repository.observeDaySummaries().first()
        provideContent {
            val checklist by repository.observeChecklistForDate(today)
                .collectAsState(initial = initialChecklist)
            val summaries by repository.observeDaySummaries()
                .collectAsState(initial = initialSummaries)
            WidgetContent(buildWidgetSnapshot(today, checklist, summaries))
        }
    }

    companion object {
        /** ~2×2: a compact progress dial. */
        val SMALL = DpSize(150.dp, 110.dp)

        /** ~4×2: progress on the left, the first few tasks on the right. */
        val WIDE = DpSize(250.dp, 110.dp)

        /** ~4×4: header, streak and the full scrollable task list. */
        val LARGE = DpSize(250.dp, 250.dp)
    }
}

// --- Layout dispatch --------------------------------------------------------

@Composable
private fun WidgetContent(snapshot: WidgetSnapshot) {
    val size = LocalSize.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(WidgetColors.background)
            .cornerRadius(20.dp)
            .padding(14.dp),
    ) {
        when {
            !snapshot.hasChecklist -> EmptyLayout()
            size.width < 210.dp -> SmallLayout(snapshot)
            size.height < 180.dp -> WideLayout(snapshot)
            else -> LargeLayout(snapshot)
        }
    }
}

// --- Size-specific layouts --------------------------------------------------

/** 2×2 — a single hero percentage with the date above and streak below. */
@Composable
private fun SmallLayout(snapshot: WidgetSnapshot) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(homeIntent(context))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = shortDateFormatter.format(snapshot.date),
            style = TextStyle(color = WidgetColors.subtle, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = "${snapshot.completionPercent}%",
            style = TextStyle(color = WidgetColors.foreground, fontSize = 34.sp, fontWeight = FontWeight.Bold),
        )
        Text(
            text = "${snapshot.completedCount}/${snapshot.totalCount} done",
            style = TextStyle(color = WidgetColors.subtle, fontSize = 11.sp),
        )
        Spacer(GlanceModifier.height(8.dp))
        StreakChip(snapshot.streak)
    }
}

/** 4×2 — progress + streak on the left, the first few tasks on the right. */
@Composable
private fun WideLayout(snapshot: WidgetSnapshot) {
    val context = LocalContext.current
    Row(modifier = GlanceModifier.fillMaxSize()) {
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .clickable(actionStartActivity(homeIntent(context))),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = shortDateFormatter.format(snapshot.date),
                style = TextStyle(color = WidgetColors.subtle, fontSize = 12.sp, fontWeight = FontWeight.Medium),
            )
            Text(
                text = "${snapshot.completionPercent}%",
                style = TextStyle(color = WidgetColors.foreground, fontSize = 30.sp, fontWeight = FontWeight.Bold),
            )
            Text(
                text = "${snapshot.completedCount}/${snapshot.totalCount} done",
                style = TextStyle(color = WidgetColors.subtle, fontSize = 11.sp),
            )
            Spacer(GlanceModifier.height(6.dp))
            StreakChip(snapshot.streak)
        }
        Spacer(GlanceModifier.width(12.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            val visible = snapshot.tasks.take(WIDE_TASK_LIMIT)
            visible.forEach { TaskRow(it, compact = true) }
            val overflow = snapshot.tasks.size - visible.size
            if (overflow > 0) {
                Text(
                    text = "+$overflow more",
                    style = TextStyle(color = WidgetColors.subtle, fontSize = 11.sp),
                    modifier = GlanceModifier
                        .padding(top = 2.dp)
                        .clickable(actionStartActivity(homeIntent(context))),
                )
            }
        }
    }
}

/** 4×4 — full header, streak and the complete, scrollable task list. */
@Composable
private fun LargeLayout(snapshot: WidgetSnapshot) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Header(snapshot)
        Spacer(GlanceModifier.height(6.dp))
        StreakChip(snapshot.streak)
        Spacer(GlanceModifier.height(8.dp))
        Divider()
        Spacer(GlanceModifier.height(4.dp))
        if (snapshot.tasks.isEmpty()) {
            Text(
                text = LocalContext.current.getString(R.string.widget_no_tasks),
                style = TextStyle(color = WidgetColors.subtle, fontSize = 13.sp),
                modifier = GlanceModifier.padding(top = 8.dp),
            )
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(snapshot.tasks, itemId = { it.id }) { task ->
                    TaskRow(task, compact = false)
                }
            }
        }
    }
}

/** Shown before onboarding, or on a day with no generated checklist. */
@Composable
private fun EmptyLayout() {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(homeIntent(context))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = context.getString(R.string.widget_empty_title),
            style = TextStyle(
                color = WidgetColors.foreground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = context.getString(R.string.widget_empty_subtitle),
            style = TextStyle(color = WidgetColors.subtle, fontSize = 12.sp, textAlign = TextAlign.Center),
        )
    }
}

// --- Shared pieces ----------------------------------------------------------

/** The header: the full date and today's completion percentage; opens the app. */
@Composable
private fun Header(snapshot: WidgetSnapshot) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity(homeIntent(context))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = headerDateFormatter.format(snapshot.date),
                style = TextStyle(color = WidgetColors.foreground, fontSize = 17.sp, fontWeight = FontWeight.Bold),
            )
            Text(
                text = context.getString(R.string.widget_today),
                style = TextStyle(color = WidgetColors.subtle, fontSize = 12.sp),
            )
        }
        Text(
            text = "${snapshot.completionPercent}%",
            style = TextStyle(color = WidgetColors.foreground, fontSize = 26.sp, fontWeight = FontWeight.Bold),
        )
    }
}

/** The streak "chip"; opens Statistics. Muted when there is no active streak. */
@Composable
private fun StreakChip(streak: Int) {
    val context = LocalContext.current
    val statisticsIntent = Intent(context, MainActivity::class.java).apply {
        putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DESTINATION_STATISTICS)
    }
    val label = if (streak > 0) {
        "🔥  " + context.resources.getQuantityString(R.plurals.widget_streak, streak, streak)
    } else {
        context.getString(R.string.widget_no_streak)
    }
    Text(
        text = label,
        style = TextStyle(
            color = if (streak > 0) WidgetColors.foreground else WidgetColors.subtle,
            fontSize = 13.sp,
            fontWeight = if (streak > 0) FontWeight.Medium else FontWeight.Normal,
        ),
        modifier = GlanceModifier
            .clickable(actionStartActivity(statisticsIntent))
            .semantics { contentDescription = label },
    )
}

/** One task: a monochrome checkbox + title that toggles completion on tap. */
@Composable
private fun TaskRow(task: WidgetTask, compact: Boolean) {
    val context = LocalContext.current
    val description = context.getString(
        if (task.isCompleted) R.string.widget_task_completed_cd else R.string.widget_task_incomplete_cd,
        task.title,
    )
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 3.dp else 6.dp)
            .clickable(
                actionRunCallback<ToggleTaskAction>(
                    actionParametersOf(
                        TaskIdKey to task.id,
                        TargetStateKey to !task.isCompleted,
                    ),
                ),
            )
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(
                if (task.isCompleted) R.drawable.ic_widget_check_box else R.drawable.ic_widget_check_box_blank,
            ),
            contentDescription = null,
            colorFilter = ColorFilter.tint(WidgetColors.foreground),
            modifier = GlanceModifier.size(if (compact) 18.dp else 20.dp),
        )
        Spacer(GlanceModifier.width(10.dp))
        Text(
            text = task.title,
            maxLines = 1,
            style = TextStyle(
                color = if (task.isCompleted) WidgetColors.subtle else WidgetColors.foreground,
                fontSize = if (compact) 13.sp else 14.sp,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            ),
            modifier = GlanceModifier.defaultWeight(),
        )
    }
}

/** A hairline divider that adapts to light/dark. */
@Composable
private fun Divider() {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(1.dp)
            .background(WidgetColors.divider),
    ) {}
}

// --- Style ------------------------------------------------------------------

/**
 * The widget's strictly monochrome palette. Each colour is a day/night pair that
 * Glance resolves against the system theme, so the widget honours dark mode with
 * no branching in the composition.
 */
private object WidgetColors {
    val background = ColorProvider(day = Color.White, night = Color(0xFF0F0F0F))
    val foreground = ColorProvider(day = Color.Black, night = Color.White)
    val subtle = ColorProvider(day = Color(0xFF5F5F5F), night = Color(0xFFAAAAAA))
    val divider = ColorProvider(day = Color(0xFFE3E3E3), night = Color(0xFF2A2A2A))
}

private const val WIDE_TASK_LIMIT = 3

private val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
private val headerDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")

/** Intent that brings the app to the foreground on today's page. */
private fun homeIntent(context: Context): Intent = Intent(context, MainActivity::class.java)
