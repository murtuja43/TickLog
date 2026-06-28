package com.ticklog.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

/**
 * Centralised date/time formatting and relative-date helpers.
 *
 * All user-facing date strings flow through here so formatting stays consistent
 * and locale-aware. `java.time` is available natively on our minSdk (API 28),
 * so no core-library desugaring is required.
 */
object DateTimeFormatters {

    /** e.g. "Saturday, 27 June" — the prominent line of the day header. */
    private val dayHeadline: DateTimeFormatter =
        DateTimeFormatter.ofPattern("EEEE, d MMMM")

    /** e.g. "27 Jun 2026" — compact form for lists and history rows. */
    private val mediumDate: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    /** e.g. "2026" — the supporting line of the day header. */
    private val yearOnly: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy")

    /** Formats [date] as the headline form, e.g. "Saturday, 27 June". */
    fun headline(date: LocalDate): String = date.format(dayHeadline)

    /** Formats [date] in the medium localised form, e.g. "27 Jun 2026". */
    fun medium(date: LocalDate): String = date.format(mediumDate)

    /** Formats just the year of [date], e.g. "2026". */
    fun year(date: LocalDate): String = date.format(yearOnly)

    /** A compact inclusive-range label, e.g. "27 Jun 2026 – 4 Jul 2026". */
    fun rangeSummary(start: LocalDate, end: LocalDate): String =
        "${medium(start)} – ${medium(end)}"

    /**
     * A friendly relative label for [date] measured against [today].
     *
     * Returns "Today", "Yesterday" or "Tomorrow" when applicable, otherwise the
     * year — used as the day header's supporting line so the user always has
     * quick temporal context.
     */
    fun relativeLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String =
        when (ChronoUnit.DAYS.between(today, date)) {
            0L -> "Today"
            -1L -> "Yesterday"
            1L -> "Tomorrow"
            else -> year(date)
        }
}
