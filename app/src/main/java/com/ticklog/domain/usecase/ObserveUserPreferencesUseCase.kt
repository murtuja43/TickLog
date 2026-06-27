package com.ticklog.domain.usecase

import com.ticklog.domain.model.UserPreferences
import com.ticklog.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Streams the current [UserPreferences].
 *
 * A use case for a one-line delegation may look ceremonial, but it gives every
 * consumer a single, intention-revealing entry point ("observe user
 * preferences") and a natural home for any future cross-cutting logic (logging,
 * combining sources) — invoked simply as `observeUserPreferences()`.
 */
class ObserveUserPreferencesUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) {
    operator fun invoke(): Flow<UserPreferences> = preferencesRepository.preferences
}
