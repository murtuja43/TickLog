package com.ticklog.domain.repository

import com.ticklog.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction over the relational app settings stored in Room.
 *
 * Separate from [PreferencesRepository]: this covers domain configuration that
 * future phases query alongside the checklist data (active template, reminders,
 * week start), while preferences covers UI-level flags.
 */
interface SettingsRepository {

    /** A reactive stream of the current [AppSettings], with sensible defaults. */
    val settings: Flow<AppSettings>

    /** Persists an updated [AppSettings] snapshot. */
    suspend fun updateSettings(settings: AppSettings)
}
