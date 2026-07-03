package com.ticklog.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Provides the shared kotlinx.serialization [Json] used for the backup format.
 *
 * `ignoreUnknownKeys` keeps older app versions able to read newer backups (they
 * skip fields they don't know), which pairs with the explicit format-version
 * check to give forward/backward compatibility.
 */
@Module
@InstallIn(SingletonComponent::class)
object SerializationModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }
}
