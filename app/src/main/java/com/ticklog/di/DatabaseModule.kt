package com.ticklog.di

import android.content.Context
import androidx.room.Room
import com.ticklog.data.database.TickLogDatabase
import com.ticklog.data.database.dao.ChecklistItemDao
import com.ticklog.data.database.dao.ChecklistTemplateDao
import com.ticklog.data.database.dao.CompletionHistoryDao
import com.ticklog.data.database.dao.DailyChecklistDao
import com.ticklog.data.database.dao.DailyChecklistItemDao
import com.ticklog.data.database.dao.SettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the Room database and each of its DAOs as singletons.
 *
 * The database is constructed once for the application's lifetime; DAOs are
 * obtained from it so feature code injects the narrow DAO it needs rather than
 * the whole database object.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): TickLogDatabase =
        Room.databaseBuilder(
            context,
            TickLogDatabase::class.java,
            TickLogDatabase.DATABASE_NAME,
        )
            // History must never be silently destroyed: we deliberately do NOT
            // call fallbackToDestructiveMigration(). Schema changes ship with
            // explicit, tested Migrations instead.
            .build()

    @Provides
    fun provideChecklistTemplateDao(database: TickLogDatabase): ChecklistTemplateDao =
        database.checklistTemplateDao()

    @Provides
    fun provideChecklistItemDao(database: TickLogDatabase): ChecklistItemDao =
        database.checklistItemDao()

    @Provides
    fun provideDailyChecklistDao(database: TickLogDatabase): DailyChecklistDao =
        database.dailyChecklistDao()

    @Provides
    fun provideDailyChecklistItemDao(database: TickLogDatabase): DailyChecklistItemDao =
        database.dailyChecklistItemDao()

    @Provides
    fun provideCompletionHistoryDao(database: TickLogDatabase): CompletionHistoryDao =
        database.completionHistoryDao()

    @Provides
    fun provideSettingsDao(database: TickLogDatabase): SettingsDao =
        database.settingsDao()
}
