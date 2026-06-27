package com.ticklog.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalTime

/**
 * Persisted, **relational** application configuration — a single-row table
 * (id is pinned to [SINGLETON_ID]).
 *
 * This is deliberately distinct from the lightweight UI preferences kept in
 * DataStore (theme, onboarding flag, chosen date range). Those are simple
 * key/value flags read on the hot path of UI; the values here are domain
 * configuration that naturally lives alongside the checklist tables and that
 * future phases will query relationally — e.g. which template is active and the
 * default daily reminder. Keeping the two stores cleanly separated avoids
 * duplicating any single piece of state.
 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = SINGLETON_ID,

    /** The template currently used for daily generation; null until created. */
    @ColumnInfo(name = "active_template_id")
    val activeTemplateId: Long? = null,

    /** Whether a daily reminder notification is scheduled. */
    @ColumnInfo(name = "reminder_enabled")
    val reminderEnabled: Boolean = false,

    /** Time of day for the reminder; null when reminders are off. */
    @ColumnInfo(name = "reminder_time")
    val reminderTime: LocalTime? = null,

    /** First day of the week (ISO-8601: 1 = Monday … 7 = Sunday) for calendars. */
    @ColumnInfo(name = "week_start_iso_day")
    val weekStartIsoDay: Int = 1,

    /** App-data schema version of this settings row, for future forward-compat. */
    @ColumnInfo(name = "config_version")
    val configVersion: Int = 1,
) {
    companion object {
        /** The fixed primary key of the one-and-only settings row. */
        const val SINGLETON_ID: Int = 1
    }
}
