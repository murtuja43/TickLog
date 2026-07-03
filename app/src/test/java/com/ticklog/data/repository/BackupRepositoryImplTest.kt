package com.ticklog.data.repository

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.ticklog.data.database.TickLogDatabase
import com.ticklog.data.datastore.UserPreferencesDataSource
import com.ticklog.domain.model.BackupError
import com.ticklog.domain.model.BackupResult
import com.ticklog.domain.model.DateRange
import com.ticklog.domain.model.TaskDraft
import com.ticklog.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate

/**
 * End-to-end backup and restore tests against a real in-memory Room database and
 * a real DataStore — covering the export/import round-trip, checksum validation
 * and graceful rejection of invalid files.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BackupRepositoryImplTest {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var database: TickLogDatabase
    private lateinit var prefsFile: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var preferences: UserPreferencesDataSource
    private lateinit var checklistRepository: ChecklistRepositoryImpl
    private lateinit var backupRepository: BackupRepositoryImpl

    private val base: LocalDate = LocalDate.of(2026, 1, 10)
    private fun day(offset: Int) = base.plusDays(offset.toLong())

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, TickLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefsFile = File(context.filesDir, "backup_test_${System.nanoTime()}.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { prefsFile }
        preferences = UserPreferencesDataSource(dataStore)

        checklistRepository = ChecklistRepositoryImpl(
            database, database.checklistTemplateDao(), database.checklistItemDao(),
            database.dailyChecklistDao(), database.dailyChecklistItemDao(),
        )
        backupRepository = BackupRepositoryImpl(
            database = database,
            templateDao = database.checklistTemplateDao(),
            itemDao = database.checklistItemDao(),
            dailyChecklistDao = database.dailyChecklistDao(),
            dailyItemDao = database.dailyChecklistItemDao(),
            completionHistoryDao = database.completionHistoryDao(),
            settingsDao = database.settingsDao(),
            preferencesDataSource = preferences,
            json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
            ioDispatcher = Dispatchers.IO,
        )
    }

    @After
    fun tearDown() {
        database.close()
        prefsFile.delete()
    }

    private suspend fun seed() {
        checklistRepository.createChecklist(
            name = "My Checklist",
            range = DateRange(day(0), day(2)),
            drafts = listOf(TaskDraft.from("Apple", "water")!!, TaskDraft.from("Banana", null)!!),
        )
        preferences.setThemeMode(ThemeMode.DARK)
    }

    @Test
    fun `export then restore round-trips all data and preferences`() = runBlocking {
        seed()
        val bytes = ByteArrayOutputStream().also { backupRepository.exportTo(it) }.toByteArray()

        // Simulate a changed device, then restore.
        preferences.setThemeMode(ThemeMode.LIGHT)
        val result = backupRepository.importFrom(ByteArrayInputStream(bytes))

        assertThat(result).isEqualTo(BackupResult.Success)
        assertThat(checklistRepository.observeDaySummaries().first()).hasSize(3)
        assertThat(preferences.userPreferences.first().themeMode).isEqualTo(ThemeMode.DARK)
    }

    @Test
    fun `a tampered backup is rejected by the checksum`() = runBlocking {
        seed()
        val text = ByteArrayOutputStream().also { backupRepository.exportTo(it) }
            .toByteArray().decodeToString()
        // Alter the payload without recomputing the checksum.
        val tampered = text.replaceFirst("Apple", "Zpple")

        val result = backupRepository.importFrom(ByteArrayInputStream(tampered.encodeToByteArray()))

        assertThat(result).isEqualTo(BackupResult.Failure(BackupError.CHECKSUM_MISMATCH))
    }

    @Test
    fun `a non-backup file is rejected as invalid`() = runBlocking {
        val result = backupRepository.importFrom(
            ByteArrayInputStream("this is not a backup".encodeToByteArray()),
        )
        assertThat(result).isEqualTo(BackupResult.Failure(BackupError.INVALID_FILE))
    }
}
