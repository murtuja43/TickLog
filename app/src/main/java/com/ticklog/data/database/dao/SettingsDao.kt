package com.ticklog.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ticklog.data.database.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data-access object for the single-row [SettingsEntity].
 */
@Dao
interface SettingsDao {

    /** Inserts the settings row; ignored if it already exists (idempotent seed). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(settings: SettingsEntity)

    /** Updates the settings row. */
    @Update
    suspend fun update(settings: SettingsEntity)

    /** Observes the settings row, or null before it has been seeded. */
    @Query("SELECT * FROM settings WHERE id = :id LIMIT 1")
    fun observeSettings(id: Int = SettingsEntity.SINGLETON_ID): Flow<SettingsEntity?>

    /** One-shot fetch of the settings row. */
    @Query("SELECT * FROM settings WHERE id = :id LIMIT 1")
    suspend fun getSettings(id: Int = SettingsEntity.SINGLETON_ID): SettingsEntity?
}
