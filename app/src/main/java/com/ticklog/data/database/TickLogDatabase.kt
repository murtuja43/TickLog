package com.ticklog.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ticklog.data.database.converter.Converters
import com.ticklog.data.database.dao.ChecklistItemDao
import com.ticklog.data.database.dao.ChecklistTemplateDao
import com.ticklog.data.database.dao.CompletionHistoryDao
import com.ticklog.data.database.dao.DailyChecklistDao
import com.ticklog.data.database.dao.DailyChecklistItemDao
import com.ticklog.data.database.dao.SettingsDao
import com.ticklog.data.database.entity.ChecklistItemEntity
import com.ticklog.data.database.entity.ChecklistTemplateEntity
import com.ticklog.data.database.entity.CompletionHistoryEntity
import com.ticklog.data.database.entity.DailyChecklistEntity
import com.ticklog.data.database.entity.DailyChecklistItemEntity
import com.ticklog.data.database.entity.SettingsEntity

/**
 * The Room database — the single offline source of truth for all TickLog data.
 *
 * The schema is exported (see the `room.schemaLocation` KSP arg in the module's
 * build script) so that every version is captured on disk and future migrations
 * can be written and tested against a known history. Because the product
 * promises to "preserve history forever", destructive fallback migrations are
 * intentionally **not** enabled here; real [androidx.room.migration.Migration]s
 * will be added as the schema evolves.
 */
@Database(
    entities = [
        ChecklistTemplateEntity::class,
        ChecklistItemEntity::class,
        DailyChecklistEntity::class,
        DailyChecklistItemEntity::class,
        CompletionHistoryEntity::class,
        SettingsEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class TickLogDatabase : RoomDatabase() {

    abstract fun checklistTemplateDao(): ChecklistTemplateDao
    abstract fun checklistItemDao(): ChecklistItemDao
    abstract fun dailyChecklistDao(): DailyChecklistDao
    abstract fun dailyChecklistItemDao(): DailyChecklistItemDao
    abstract fun completionHistoryDao(): CompletionHistoryDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        /** On-disk file name of the database. */
        const val DATABASE_NAME = "ticklog.db"
    }
}
