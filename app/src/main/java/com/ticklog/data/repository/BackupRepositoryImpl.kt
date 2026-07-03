package com.ticklog.data.repository

import androidx.room.withTransaction
import com.ticklog.BuildConfig
import com.ticklog.data.backup.BackupDocument
import com.ticklog.data.backup.BackupPayload
import com.ticklog.data.backup.toDomain
import com.ticklog.data.backup.toDto
import com.ticklog.data.backup.toEntity
import com.ticklog.data.database.TickLogDatabase
import com.ticklog.data.database.dao.ChecklistItemDao
import com.ticklog.data.database.dao.ChecklistTemplateDao
import com.ticklog.data.database.dao.CompletionHistoryDao
import com.ticklog.data.database.dao.DailyChecklistDao
import com.ticklog.data.database.dao.DailyChecklistItemDao
import com.ticklog.data.database.dao.SettingsDao
import com.ticklog.data.datastore.UserPreferencesDataSource
import com.ticklog.di.qualifier.IoDispatcher
import com.ticklog.domain.model.BackupError
import com.ticklog.domain.model.BackupResult
import com.ticklog.domain.repository.BackupRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JSON-based implementation of [BackupRepository].
 *
 * A backup is a single self-describing JSON document: a versioned wrapper around
 * every entity (mapped to primitive DTOs) plus the preferences, sealed with a
 * SHA-256 checksum over the serialised payload. Restore validates the version and
 * checksum before it writes anything, then replaces all data inside one
 * transaction — so a corrupt or tampered file is rejected with the existing data
 * left completely intact.
 */
@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val database: TickLogDatabase,
    private val templateDao: ChecklistTemplateDao,
    private val itemDao: ChecklistItemDao,
    private val dailyChecklistDao: DailyChecklistDao,
    private val dailyItemDao: DailyChecklistItemDao,
    private val completionHistoryDao: CompletionHistoryDao,
    private val settingsDao: SettingsDao,
    private val preferencesDataSource: UserPreferencesDataSource,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BackupRepository {

    override suspend fun exportTo(output: OutputStream) {
        val payload = buildPayload()
        val document = BackupDocument(
            formatVersion = FORMAT_VERSION,
            appVersion = BuildConfig.VERSION_NAME,
            createdAtEpochMillis = System.currentTimeMillis(),
            checksum = checksumOf(payload),
            payload = payload,
        )
        val bytes = json.encodeToString(BackupDocument.serializer(), document)
            .encodeToByteArray()
        // The caller owns the stream's lifecycle; we only write and flush.
        withContext(ioDispatcher) {
            output.write(bytes)
            output.flush()
        }
    }

    override suspend fun importFrom(input: InputStream): BackupResult {
        val text = withContext(ioDispatcher) { input.readBytes().decodeToString() }

        val document = try {
            json.decodeFromString(BackupDocument.serializer(), text)
        } catch (_: Exception) {
            return BackupResult.Failure(BackupError.INVALID_FILE)
        }

        if (document.formatVersion > FORMAT_VERSION) {
            return BackupResult.Failure(BackupError.UNSUPPORTED_VERSION)
        }
        if (checksumOf(document.payload) != document.checksum) {
            return BackupResult.Failure(BackupError.CHECKSUM_MISMATCH)
        }

        return try {
            restore(document.payload)
            BackupResult.Success
        } catch (_: Exception) {
            BackupResult.Failure(BackupError.RESTORE_FAILED)
        }
    }

    private suspend fun buildPayload(): BackupPayload = BackupPayload(
        preferences = preferencesDataSource.userPreferences.first().toDto(),
        templates = templateDao.getAll().map { it.toDto() },
        items = itemDao.getAll().map { it.toDto() },
        dailyChecklists = dailyChecklistDao.getAll().map { it.toDto() },
        dailyItems = dailyItemDao.getAll().map { it.toDto() },
        completionHistory = completionHistoryDao.getAll().map { it.toDto() },
        settings = settingsDao.getAll().map { it.toDto() },
    )

    private suspend fun restore(payload: BackupPayload) {
        // Wipe first (clearAllTables manages its own transaction), then re-insert
        // in foreign-key order inside a single transaction.
        database.clearAllTables()
        database.withTransaction {
            templateDao.insertAll(payload.templates.map { it.toEntity() })
            itemDao.insertAll(payload.items.map { it.toEntity() })
            dailyChecklistDao.insertAll(payload.dailyChecklists.map { it.toEntity() })
            dailyItemDao.insertAll(payload.dailyItems.map { it.toEntity() })
            completionHistoryDao.insertAll(payload.completionHistory.map { it.toEntity() })
            settingsDao.insertAll(payload.settings.map { it.toEntity() })
        }
        preferencesDataSource.replaceAll(payload.preferences.toDomain())
    }

    /** Deterministic SHA-256 (hex) over the serialised payload. */
    private fun checksumOf(payload: BackupPayload): String {
        val bytes = json.encodeToString(BackupPayload.serializer(), payload).encodeToByteArray()
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val FORMAT_VERSION = 1
    }
}
