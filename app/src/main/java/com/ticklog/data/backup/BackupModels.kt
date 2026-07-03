package com.ticklog.data.backup

import com.ticklog.data.database.entity.ChecklistItemEntity
import com.ticklog.data.database.entity.ChecklistTemplateEntity
import com.ticklog.data.database.entity.CompletionHistoryEntity
import com.ticklog.data.database.entity.DailyChecklistEntity
import com.ticklog.data.database.entity.DailyChecklistItemEntity
import com.ticklog.data.database.entity.SettingsEntity
import com.ticklog.domain.model.DateFormat
import com.ticklog.domain.model.ThemeMode
import com.ticklog.domain.model.UserPreferences
import com.ticklog.domain.model.WeekStart
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * The on-disk backup format.
 *
 * A [BackupDocument] wraps the entire app state ([BackupPayload]) with a format
 * version, provenance and an integrity [checksum] computed over the serialised
 * payload. Entities are mapped to plain, primitive DTOs (dates as epoch days,
 * instants as epoch millis) so the format is stable and portable, decoupled from
 * the Room schema.
 */
@Serializable
data class BackupDocument(
    val formatVersion: Int,
    val appVersion: String,
    val createdAtEpochMillis: Long,
    val checksum: String,
    val payload: BackupPayload,
)

@Serializable
data class BackupPayload(
    val preferences: PreferencesDto,
    val templates: List<TemplateDto>,
    val items: List<ItemDto>,
    val dailyChecklists: List<DailyChecklistDto>,
    val dailyItems: List<DailyItemDto>,
    val completionHistory: List<CompletionHistoryDto>,
    val settings: List<SettingsDto>,
)

@Serializable
data class PreferencesDto(
    val onboardingCompleted: Boolean,
    val themeMode: String,
    val dateFormat: String,
    val weekStart: String,
    val animationsEnabled: Boolean,
    val scheduleStartEpochDay: Long?,
    val scheduleEndEpochDay: Long?,
)

@Serializable
data class TemplateDto(
    val id: Long,
    val name: String,
    val description: String?,
    val isActive: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

@Serializable
data class ItemDto(
    val id: Long,
    val templateId: Long,
    val title: String,
    val description: String?,
    val position: Int,
    val isArchived: Boolean,
    val createdAtMillis: Long,
)

@Serializable
data class DailyChecklistDto(
    val id: Long,
    val templateId: Long,
    val dateEpochDay: Long,
    val createdAtMillis: Long,
)

@Serializable
data class DailyItemDto(
    val id: Long,
    val dailyChecklistId: Long,
    val sourceItemId: Long?,
    val title: String,
    val note: String?,
    val position: Int,
    val isCompleted: Boolean,
    val completedAtMillis: Long?,
)

@Serializable
data class CompletionHistoryDto(
    val id: Long,
    val dailyChecklistId: Long,
    val dateEpochDay: Long,
    val totalItems: Int,
    val completedItems: Int,
    val updatedAtMillis: Long,
)

@Serializable
data class SettingsDto(
    val id: Int,
    val activeTemplateId: Long?,
    val reminderEnabled: Boolean,
    val reminderTimeSecondOfDay: Int?,
    val weekStartIsoDay: Int,
    val configVersion: Int,
)

// --- Entity <-> DTO mappers ------------------------------------------------

fun ChecklistTemplateEntity.toDto() = TemplateDto(
    id, name, description, isActive, createdAt.toEpochMilli(), updatedAt.toEpochMilli(),
)

fun TemplateDto.toEntity() = ChecklistTemplateEntity(
    id = id,
    name = name,
    description = description,
    isActive = isActive,
    createdAt = Instant.ofEpochMilli(createdAtMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtMillis),
)

fun ChecklistItemEntity.toDto() = ItemDto(
    id, templateId, title, description, position, isArchived, createdAt.toEpochMilli(),
)

fun ItemDto.toEntity() = ChecklistItemEntity(
    id = id,
    templateId = templateId,
    title = title,
    description = description,
    position = position,
    isArchived = isArchived,
    createdAt = Instant.ofEpochMilli(createdAtMillis),
)

fun DailyChecklistEntity.toDto() = DailyChecklistDto(
    id, templateId, date.toEpochDay(), createdAt.toEpochMilli(),
)

fun DailyChecklistDto.toEntity() = DailyChecklistEntity(
    id = id,
    templateId = templateId,
    date = LocalDate.ofEpochDay(dateEpochDay),
    createdAt = Instant.ofEpochMilli(createdAtMillis),
)

fun DailyChecklistItemEntity.toDto() = DailyItemDto(
    id, dailyChecklistId, sourceItemId, title, note, position, isCompleted,
    completedAt?.toEpochMilli(),
)

fun DailyItemDto.toEntity() = DailyChecklistItemEntity(
    id = id,
    dailyChecklistId = dailyChecklistId,
    sourceItemId = sourceItemId,
    title = title,
    note = note,
    position = position,
    isCompleted = isCompleted,
    completedAt = completedAtMillis?.let(Instant::ofEpochMilli),
)

fun CompletionHistoryEntity.toDto() = CompletionHistoryDto(
    id, dailyChecklistId, date.toEpochDay(), totalItems, completedItems, updatedAt.toEpochMilli(),
)

fun CompletionHistoryDto.toEntity() = CompletionHistoryEntity(
    id = id,
    dailyChecklistId = dailyChecklistId,
    date = LocalDate.ofEpochDay(dateEpochDay),
    totalItems = totalItems,
    completedItems = completedItems,
    updatedAt = Instant.ofEpochMilli(updatedAtMillis),
)

fun SettingsEntity.toDto() = SettingsDto(
    id, activeTemplateId, reminderEnabled, reminderTime?.toSecondOfDay(), weekStartIsoDay, configVersion,
)

fun SettingsDto.toEntity() = SettingsEntity(
    id = id,
    activeTemplateId = activeTemplateId,
    reminderEnabled = reminderEnabled,
    reminderTime = reminderTimeSecondOfDay?.let { LocalTime.ofSecondOfDay(it.toLong()) },
    weekStartIsoDay = weekStartIsoDay,
    configVersion = configVersion,
)

fun UserPreferences.toDto() = PreferencesDto(
    onboardingCompleted = onboardingCompleted,
    themeMode = themeMode.name,
    dateFormat = dateFormat.name,
    weekStart = weekStart.name,
    animationsEnabled = animationsEnabled,
    scheduleStartEpochDay = scheduleStartDate?.toEpochDay(),
    scheduleEndEpochDay = scheduleEndDate?.toEpochDay(),
)

fun PreferencesDto.toDomain() = UserPreferences(
    onboardingCompleted = onboardingCompleted,
    themeMode = enumByNameOr(themeMode, ThemeMode.SYSTEM),
    dateFormat = enumByNameOr(dateFormat, DateFormat.SYSTEM),
    weekStart = enumByNameOr(weekStart, WeekStart.MONDAY),
    animationsEnabled = animationsEnabled,
    scheduleStartDate = scheduleStartEpochDay?.let(LocalDate::ofEpochDay),
    scheduleEndDate = scheduleEndEpochDay?.let(LocalDate::ofEpochDay),
)

private inline fun <reified T : Enum<T>> enumByNameOr(name: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default
