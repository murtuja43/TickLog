package com.ticklog.domain.model

import java.time.LocalDate

/**
 * One cell in a month grid. A null [date] is leading/trailing padding that keeps
 * the weekday columns aligned.
 */
data class CalendarCell(val date: LocalDate?)
