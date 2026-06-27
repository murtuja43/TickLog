package com.ticklog.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ticklog.domain.model.ThemeMode
import com.ticklog.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin, typed wrapper around the Preferences [DataStore].
 *
 * It owns the preference *keys* and the (de)serialisation between stored
 * primitives and the [UserPreferences] domain model, exposing a clean reactive
 * surface. The repository layer depends on this data source rather than touching
 * DataStore directly, which keeps key handling in exactly one place.
 *
 * Dates are stored as epoch-day longs (see [LocalDate.toEpochDay]); the theme is
 * stored as the [ThemeMode] enum name and decoded defensively.
 */
@Singleton
class UserPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    /** A reactive stream of the decoded [UserPreferences]. */
    val userPreferences: Flow<UserPreferences> =
        dataStore.data.map { prefs -> prefs.toUserPreferences() }

    /** Persists the onboarding flag together with the chosen date range. */
    suspend fun setOnboardingResult(startDate: LocalDate, endDate: LocalDate) {
        dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = true
            prefs[Keys.SCHEDULE_START_EPOCH_DAY] = startDate.toEpochDay()
            prefs[Keys.SCHEDULE_END_EPOCH_DAY] = endDate.toEpochDay()
        }
    }

    /** Persists the user's theme choice. */
    suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode.name
        }
    }

    /** Decodes a raw [Preferences] snapshot into the domain model. */
    private fun Preferences.toUserPreferences(): UserPreferences = UserPreferences(
        onboardingCompleted = this[Keys.ONBOARDING_COMPLETED] ?: false,
        themeMode = decodeThemeMode(this[Keys.THEME_MODE]),
        scheduleStartDate = this[Keys.SCHEDULE_START_EPOCH_DAY]?.let(LocalDate::ofEpochDay),
        scheduleEndDate = this[Keys.SCHEDULE_END_EPOCH_DAY]?.let(LocalDate::ofEpochDay),
    )

    /** Maps a stored theme name back to the enum, defaulting to [ThemeMode.SYSTEM]. */
    private fun decodeThemeMode(raw: String?): ThemeMode =
        ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.SYSTEM

    /** Strongly-typed preference keys, centralised to avoid stringly-typed bugs. */
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SCHEDULE_START_EPOCH_DAY = longPreferencesKey("schedule_start_epoch_day")
        val SCHEDULE_END_EPOCH_DAY = longPreferencesKey("schedule_end_epoch_day")
    }

    companion object {
        /** File name of the preferences DataStore (under the app's files dir). */
        const val DATASTORE_NAME = "ticklog_user_preferences"
    }
}
