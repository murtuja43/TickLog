package com.ticklog.data.datastore

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.ticklog.domain.model.DateFormat
import com.ticklog.domain.model.ThemeMode
import com.ticklog.domain.model.WeekStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Verifies that every user preference persists through the DataStore layer —
 * covering settings persistence and theme switching.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class UserPreferencesDataSourceTest {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var file: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var source: UserPreferencesDataSource

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        file = File(context.filesDir, "test_${System.nanoTime()}.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { file }
        source = UserPreferencesDataSource(dataStore)
    }

    @After
    fun tearDown() {
        file.delete()
    }

    @Test
    fun `defaults are returned before anything is stored`() = runBlocking {
        val prefs = source.userPreferences.first()
        assertThat(prefs.themeMode).isEqualTo(ThemeMode.SYSTEM)
        assertThat(prefs.dateFormat).isEqualTo(DateFormat.SYSTEM)
        assertThat(prefs.weekStart).isEqualTo(WeekStart.MONDAY)
        assertThat(prefs.animationsEnabled).isTrue()
        assertThat(prefs.onboardingCompleted).isFalse()
    }

    @Test
    fun `theme switching persists`() = runBlocking {
        source.setThemeMode(ThemeMode.DARK)
        assertThat(source.userPreferences.first().themeMode).isEqualTo(ThemeMode.DARK)

        source.setThemeMode(ThemeMode.LIGHT)
        assertThat(source.userPreferences.first().themeMode).isEqualTo(ThemeMode.LIGHT)
    }

    @Test
    fun `date format, week start and animations persist`() = runBlocking {
        source.setDateFormat(DateFormat.ISO)
        source.setWeekStart(WeekStart.SUNDAY)
        source.setAnimationsEnabled(false)

        val prefs = source.userPreferences.first()
        assertThat(prefs.dateFormat).isEqualTo(DateFormat.ISO)
        assertThat(prefs.weekStart).isEqualTo(WeekStart.SUNDAY)
        assertThat(prefs.animationsEnabled).isFalse()
    }

    @Test
    fun `resetting onboarding clears the flag but keeps the range`() = runBlocking {
        source.setOnboardingResult(
            startDate = java.time.LocalDate.of(2026, 1, 1),
            endDate = java.time.LocalDate.of(2026, 1, 31),
        )
        assertThat(source.userPreferences.first().onboardingCompleted).isTrue()

        source.clearOnboardingFlag()
        val prefs = source.userPreferences.first()
        assertThat(prefs.onboardingCompleted).isFalse()
        assertThat(prefs.scheduleStartDate).isEqualTo(java.time.LocalDate.of(2026, 1, 1))
    }
}
