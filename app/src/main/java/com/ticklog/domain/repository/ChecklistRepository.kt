package com.ticklog.domain.repository

import com.ticklog.domain.model.ChecklistTemplate
import com.ticklog.domain.model.DailyChecklist
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Abstraction over checklist data.
 *
 * Phase 1 intentionally exposes only the **read** surface needed to drive the
 * Home screen's empty state: observing the (currently absent) active template
 * and a given day's checklist. The write surface — creating templates, items
 * and generating days across a range — is added in Phase 2. Defining the
 * interface now keeps the architecture stable and the UI decoupled from the
 * concrete data source from day one.
 */
interface ChecklistRepository {

    /** Observes the active checklist template, or null if none exists yet. */
    fun observeActiveTemplate(): Flow<ChecklistTemplate?>

    /**
     * Observes the checklist for [date] under the active template, or null when
     * no template/day exists. Re-evaluates automatically as the active template
     * or the day's data changes.
     */
    fun observeChecklistForDate(date: LocalDate): Flow<DailyChecklist?>
}
