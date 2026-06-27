package com.ticklog.di

import com.ticklog.di.qualifier.DefaultDispatcher
import com.ticklog.di.qualifier.IoDispatcher
import com.ticklog.di.qualifier.MainDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Provides the standard coroutine dispatchers as injectable, qualified
 * dependencies.
 *
 * Centralising dispatcher provision means production code never hard-codes
 * `Dispatchers.IO`/`Default`, and tests can replace these bindings with a single
 * test dispatcher for deterministic behaviour.
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
