package com.ticklog.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Hand-written, non-destructive Room migrations.
 *
 * TickLog promises to "preserve history forever", so every schema change ships
 * with an explicit migration that transforms existing data in place — the
 * database is never wiped. Each migration is paired with the exported schema
 * JSON under `app/schemas/`, which Room uses to verify the result at build time.
 */

/**
 * v1 → v2: Phase 2 adds an optional per-day note to each tickable task.
 *
 * The column is added as a nullable TEXT with no default, exactly matching the
 * new [com.ticklog.data.database.entity.DailyChecklistItemEntity.note] field, so
 * existing rows simply gain a `NULL` note and no history is altered.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE daily_checklist_items ADD COLUMN note TEXT")
    }
}

/** Every migration the database knows how to apply, in order. */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
