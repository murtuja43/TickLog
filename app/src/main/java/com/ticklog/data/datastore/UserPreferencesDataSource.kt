package com.ticklog.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ticklog.domain.model.DateFormat
import com.ticklog.domain.model.ThemeMode
import com.ticklog.domain.model.UserPreferences
import com.ticklog.domain.model.WeekStart
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
 * Dates are stored as epoch-day longs (see [LocalDate.toEpochDay]); enums are
 * stored by name and decoded defensively so an unknown value never crashes.
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

    /** Clears the onboarding flag; the range and data are left untouched. */
    suspend fun clearOnboardingFlag() {
        dataStore.edit { prefs -> prefs[Keys.ONBOARDING_COMPLETED] = false }
    }

    /** Persists the user's theme choice. */
    suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = themeMode.name }
    }

    /** Persists the user's date-format choice. */
    suspend fun setDateFormat(dateFormat: DateFormat) {
        dataStore.edit { prefs -> prefs[Keys.DATE_FORMAT] = dateFormat.name }
    }

    /** Persists the user's week-start choice. */
    suspend fun setWeekStart(weekStart: WeekStart) {
        dataStore.edit { prefs -> prefs[Keys.WEEK_START] = weekStart.name }
    }

    /** Persists whether non-essential animations are enabled. */
    suspend fun setAnimationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.ANIMATIONS_ENABLED] = enabled }
    }

    /** Overwrites every stored preference from [preferences] (used by restore). */
    suspend fun replaceAll(preferences: UserPreferences) {
        dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = preferences.onboardingCompleted
            prefs[Keys.THEME_MODE] = preferences.themeMode.name
            prefs[Keys.DATE_FORMAT] = preferences.dateFormat.name
            prefs[Keys.WEEK_START] = preferences.weekStart.name
            prefs[Keys.ANIMATIONS_ENABLED] = preferences.animationsEnabled
            preferences.scheduleStartDate?.let { prefs[Keys.SCHEDULE_START_EPOCH_DAY] = it.toEpochDay() }
            preferences.scheduleEndDate?.let { prefs[Keys.SCHEDULE_END_EPOCH_DAY] = it.toEpochDay() }
        }
    }

    /** Decodes a raw [Preferences] snapshot into the domain model. */
    private fun Preferences.toUserPreferences(): UserPreferences = UserPreferences(
        onboardingCompleted = this[Keys.ONBOARDING_COMPLETED] ?: false,
        themeMode = decodeEnum(this[Keys.THEME_MODE], ThemeMode.entries, ThemeMode.SYSTEM),
        dateFormat = decodeEnum(this[Keys.DATE_FORMAT], DateFormat.entries, DateFormat.SYSTEM),
        weekStart = decodeEnum(this[Keys.WEEK_START], WeekStart.entries, WeekStart.MONDAY),
        animationsEnabled = this[Keys.ANIMATIONS_ENABLED] ?: true,
        scheduleStartDate = this[Keys.SCHEDULE_START_EPOCH_DAY]?.let(LocalDate::ofEpochDay),
        scheduleEndDate = this[Keys.SCHEDULE_END_EPOCH_DAY]?.let(LocalDate::ofEpochDay),
    )

    /** Maps a stored enum name back to its value, defaulting when unknown/absent. */
    private fun <T : Enum<T>> decodeEnum(raw: String?, values: List<T>, default: T): T =
        values.firstOrNull { it.name == raw } ?: default

    /** Strongly-typed preference keys, centralised to avoid stringly-typed bugs. */
    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DATE_FORMAT = stringPreferencesKey("date_format")
        val WEEK_START = stringPreferencesKey("week_start")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val SCHEDULE_START_EPOCH_DAY = longPreferencesKey("schedule_start_epoch_day")
        val SCHEDULE_END_EPOCH_DAY = longPreferencesKey("schedule_end_epoch_day")
    }

    companion object {
        /** File name of the preferences DataStore (under the app's files dir). */
        const val DATASTORE_NAME = "ticklog_user_preferences"
    }
}
