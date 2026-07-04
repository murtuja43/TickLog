package com.ticklog.domain.repository

import com.ticklog.domain.model.DateFormat
import com.ticklog.domain.model.ThemeMode
import com.ticklog.domain.model.UserPreferences
import com.ticklog.domain.model.WeekStart
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Abstraction over the user's lightweight preferences (DataStore-backed).
 *
 * Declared in the domain layer so ViewModels and use cases depend on this
 * interface, never on the concrete DataStore implementation — keeping the
 * dependency arrows pointing inward.
 */
interface PreferencesRepository {

    /** A reactive stream of the current [UserPreferences]. */
    val preferences: Flow<UserPreferences>

    /**
     * Marks onboarding complete and persists the user's chosen tracking range.
     *
     * @param startDate first day of the range.
     * @param endDate last day of the range (must be on or after [startDate]).
     */
    suspend fun completeOnboarding(startDate: LocalDate, endDate: LocalDate)

    /** Persists the user's [ThemeMode] choice. */
    suspend fun setThemeMode(themeMode: ThemeMode)

    /** Persists the user's [DateFormat] choice. */
    suspend fun setDateFormat(dateFormat: DateFormat)

    /** Persists the user's [WeekStart] choice. */
    suspend fun setWeekStart(weekStart: WeekStart)

    /** Persists whether non-essential animations are enabled. */
    suspend fun setAnimationsEnabled(enabled: Boolean)

    /** Persists whether PDF exports include task notes. */
    suspend fun setIncludeNotesInExport(enabled: Boolean)

    /**
     * Clears the onboarding flag so the first-run flow is shown again. The chosen
     * date range and existing checklist data are left untouched.
     */
    suspend fun resetOnboarding()
}
