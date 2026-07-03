package com.ticklog.data.repository

import com.ticklog.data.datastore.UserPreferencesDataSource
import com.ticklog.domain.model.DateFormat
import com.ticklog.domain.model.ThemeMode
import com.ticklog.domain.model.UserPreferences
import com.ticklog.domain.model.WeekStart
import com.ticklog.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import java.time.LocalDate

/**
 * DataStore-backed implementation of [PreferencesRepository].
 *
 * This is a thin orchestration layer: the [UserPreferencesDataSource] already
 * handles serialisation, so the repository simply exposes the domain interface
 * and delegates. Keeping it separate from the data source preserves the option
 * to add caching, validation or multiple sources later without changing callers.
 */
@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val dataSource: UserPreferencesDataSource,
) : PreferencesRepository {

    override val preferences: Flow<UserPreferences> = dataSource.userPreferences

    override suspend fun completeOnboarding(startDate: LocalDate, endDate: LocalDate) {
        dataSource.setOnboardingResult(startDate = startDate, endDate = endDate)
    }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataSource.setThemeMode(themeMode)
    }

    override suspend fun setDateFormat(dateFormat: DateFormat) {
        dataSource.setDateFormat(dateFormat)
    }

    override suspend fun setWeekStart(weekStart: WeekStart) {
        dataSource.setWeekStart(weekStart)
    }

    override suspend fun setAnimationsEnabled(enabled: Boolean) {
        dataSource.setAnimationsEnabled(enabled)
    }

    override suspend fun resetOnboarding() {
        dataSource.clearOnboardingFlag()
    }
}
