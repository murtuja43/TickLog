package com.ticklog.di

import com.ticklog.data.repository.ChecklistRepositoryImpl
import com.ticklog.data.repository.PreferencesRepositoryImpl
import com.ticklog.data.repository.SettingsRepositoryImpl
import com.ticklog.domain.repository.ChecklistRepository
import com.ticklog.domain.repository.PreferencesRepository
import com.ticklog.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds repository interfaces to their concrete implementations.
 *
 * Using `@Binds` (rather than `@Provides`) is the most efficient way to express
 * an interface→implementation mapping and is what keeps the rest of the app
 * depending on the domain abstractions while Hilt wires in the data-layer
 * concretes.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(
        impl: PreferencesRepositoryImpl,
    ): PreferencesRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl,
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindChecklistRepository(
        impl: ChecklistRepositoryImpl,
    ): ChecklistRepository
}
