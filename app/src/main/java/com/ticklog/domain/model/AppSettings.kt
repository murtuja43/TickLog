package com.ticklog.domain.model

import java.time.LocalTime

/**
 * Domain representation of the relational app settings stored in Room.
 *
 * Mirrors `SettingsEntity` but expressed in pure domain terms, decoupling the
 * rest of the app from the persistence schema.
 *
 * @property activeTemplateId id of the template driving daily generation, if any.
 * @property reminderEnabled whether a daily reminder is scheduled.
 * @property reminderTime time of day for the reminder, if enabled.
 * @property weekStartIsoDay first weekday for calendars (1 = Monday … 7 = Sunday).
 */
data class AppSettings(
    val activeTemplateId: Long?,
    val reminderEnabled: Boolean,
    val reminderTime: LocalTime?,
    val weekStartIsoDay: Int,
) {
    companion object {
        /** Defaults applied when no settings row has been written yet. */
        val DEFAULT = AppSettings(
            activeTemplateId = null,
            reminderEnabled = false,
            reminderTime = null,
            weekStartIsoDay = 1,
        )
    }
}
