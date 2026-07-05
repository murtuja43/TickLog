package com.ticklog.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ticklog.data.database.entity.ChecklistTemplateEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data-access object for [ChecklistTemplateEntity].
 *
 * Reads are exposed as cold [Flow]s so the UI observes the database reactively;
 * writes are `suspend` functions meant to be called off the main thread. Room
 * generates the implementation at compile time.
 */
@Dao
interface ChecklistTemplateDao {

    /** Inserts a template, returning its generated row id. */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(template: ChecklistTemplateEntity): Long

    /** Updates an existing template (e.g. rename, toggle active). */
    @Update
    suspend fun update(template: ChecklistTemplateEntity)

    /** Observes the single active template, or null if none exists yet. */
    @Query("SELECT * FROM checklist_templates WHERE is_active = 1 ORDER BY updated_at DESC LIMIT 1")
    fun observeActiveTemplate(): Flow<ChecklistTemplateEntity?>

    /** Observes every template, newest first. */
    @Query("SELECT * FROM checklist_templates ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<ChecklistTemplateEntity>>

    /** One-shot fetch of a template by id. */
    @Query("SELECT * FROM checklist_templates WHERE id = :templateId")
    suspend fun getById(templateId: Long): ChecklistTemplateEntity?

    /** Deletes a template by id; cascades to its items and daily checklists. */
    @Query("DELETE FROM checklist_templates WHERE id = :templateId")
    suspend fun deleteById(templateId: Long)

    /** All templates — used to serialise a full backup. */
    @Query("SELECT * FROM checklist_templates")
    suspend fun getAll(): List<ChecklistTemplateEntity>

    /** Deletes every template — used to clear data as part of an atomic restore. */
    @Query("DELETE FROM checklist_templates")
    suspend fun deleteAll()

    /** Bulk insert preserving ids — used to restore a full backup. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<ChecklistTemplateEntity>)
}
