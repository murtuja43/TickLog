package com.ticklog.core.designsystem

import androidx.compose.runtime.staticCompositionLocalOf
import com.ticklog.domain.model.DateFormat
import com.ticklog.domain.model.WeekStart
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * App-wide user preferences exposed to the whole Compose tree.
 *
 * These are provided once at the root (from the persisted preferences) so any
 * screen can honour the user's choices without threading them through every
 * ViewModel. They are `static` locals because they change rarely; a change
 * recomposes the tree, exactly like a theme change.
 */

/** Whether non-essential motion should play. */
val LocalAnimationsEnabled = staticCompositionLocalOf { true }

/** The weekday calendars start on. */
val LocalWeekStart = staticCompositionLocalOf { WeekStart.MONDAY }

/** How calendar dates should be formatted. */
val LocalDateFormat = staticCompositionLocalOf { DateFormat.SYSTEM }

/** Formats [date] according to this [DateFormat] (localized medium for SYSTEM). */
fun DateFormat.formatDate(date: LocalDate): String = when (val patternOrNull = pattern) {
    null -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    else -> date.format(DateTimeFormatter.ofPattern(patternOrNull))
}
