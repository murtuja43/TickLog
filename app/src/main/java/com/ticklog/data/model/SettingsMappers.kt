package com.ticklog.data.model

import com.ticklog.data.database.entity.SettingsEntity
import com.ticklog.domain.model.AppSettings

/** Maps the persisted settings row to its domain model. */
fun SettingsEntity.toDomain(): AppSettings = AppSettings(
    activeTemplateId = activeTemplateId,
    reminderEnabled = reminderEnabled,
    reminderTime = reminderTime,
    weekStartIsoDay = weekStartIsoDay,
)

/**
 * Maps a domain [AppSettings] back to the persisted row.
 *
 * The primary key is pinned to the singleton id so updates always target the
 * single settings record.
 */
fun AppSettings.toEntity(): SettingsEntity = SettingsEntity(
    id = SettingsEntity.SINGLETON_ID,
    activeTemplateId = activeTemplateId,
    reminderEnabled = reminderEnabled,
    reminderTime = reminderTime,
    weekStartIsoDay = weekStartIsoDay,
)
