package com.ticklog.data.repository

import com.ticklog.data.database.dao.SettingsDao
import com.ticklog.data.model.toDomain
import com.ticklog.data.model.toEntity
import com.ticklog.di.qualifier.IoDispatcher
import com.ticklog.domain.model.AppSettings
import com.ticklog.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room-backed implementation of [SettingsRepository].
 *
 * Reads map the persisted row to the domain model, substituting
 * [AppSettings.DEFAULT] until the row has been seeded so consumers always see a
 * valid value. Writes are dispatched onto the injected IO dispatcher.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SettingsRepository {

    override val settings: Flow<AppSettings> =
        settingsDao.observeSettings().map { entity ->
            entity?.toDomain() ?: AppSettings.DEFAULT
        }

    override suspend fun updateSettings(settings: AppSettings) {
        withContext(ioDispatcher) {
            // Seed the singleton row if absent, then apply the update.
            settingsDao.insertIfAbsent(settings.toEntity())
            settingsDao.update(settings.toEntity())
        }
    }
}
