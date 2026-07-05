package com.ticklog.data.repository

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.ticklog.data.backup.BackupDocument
import com.ticklog.data.backup.BackupPayload
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
import java.security.MessageDigest
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

    @Test
    fun `a backup from a newer format version is rejected`() = runBlocking {
        seed()
        val text = ByteArrayOutputStream().also { backupRepository.exportTo(it) }
            .toByteArray().decodeToString()
        val doc = testJson.decodeFromString(BackupDocument.serializer(), text)
        // Bump the format version beyond what this build understands. The version
        // gate is checked before the checksum, so no re-hashing is needed.
        val newer = doc.copy(formatVersion = doc.formatVersion + 1)
        val bytes = testJson.encodeToString(BackupDocument.serializer(), newer).encodeToByteArray()

        val result = backupRepository.importFrom(ByteArrayInputStream(bytes))

        assertThat(result).isEqualTo(BackupResult.Failure(BackupError.UNSUPPORTED_VERSION))
    }

    @Test
    fun `a failing restore rolls back and leaves existing data completely intact`() = runBlocking {
        seed()
        val validText = ByteArrayOutputStream().also { backupRepository.exportTo(it) }
            .toByteArray().decodeToString()
        val validDoc = testJson.decodeFromString(BackupDocument.serializer(), validText)

        // Corrupt the payload with a duplicate task row (same primary key). The
        // second insert violates the UNIQUE primary-key constraint, so the restore
        // transaction fails partway through re-inserting — but the checksum is
        // recomputed to stay valid so the file passes validation and actually
        // reaches the restore, exercising the rollback path.
        val duplicate = validDoc.payload.dailyItems.first()
        val corruptPayload = validDoc.payload.copy(
            dailyItems = validDoc.payload.dailyItems + duplicate,
        )
        val corruptDoc = validDoc.copy(
            checksum = sha256Hex(testJson.encodeToString(BackupPayload.serializer(), corruptPayload)),
            payload = corruptPayload,
        )
        val corruptBytes =
            testJson.encodeToString(BackupDocument.serializer(), corruptDoc).encodeToByteArray()

        val before = checklistRepository.observeDaySummaries().first()

        val result = backupRepository.importFrom(ByteArrayInputStream(corruptBytes))

        assertThat(result).isEqualTo(BackupResult.Failure(BackupError.RESTORE_FAILED))
        // The transaction rolled back: the original data is exactly as it was, and
        // in particular the database was never left empty.
        val after = checklistRepository.observeDaySummaries().first()
        assertThat(after).isEqualTo(before)
        assertThat(after).hasSize(3)
    }

    /** Matches [BackupRepositoryImpl]'s private checksum: SHA-256 hex of the payload JSON. */
    private fun sha256Hex(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.encodeToByteArray())
            .joinToString("") { "%02x".format(it) }

    private companion object {
        val testJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }
}
