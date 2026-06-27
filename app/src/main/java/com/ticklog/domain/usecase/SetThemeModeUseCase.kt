package com.ticklog.domain.usecase

import com.ticklog.domain.model.ThemeMode
import com.ticklog.domain.repository.PreferencesRepository
import javax.inject.Inject

/**
 * Persists the user's chosen [ThemeMode].
 *
 * Exposed as a use case so the Settings ViewModel depends on a single-purpose
 * domain action rather than the whole preferences repository surface.
 */
class SetThemeModeUseCase @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) {
    suspend operator fun invoke(themeMode: ThemeMode) {
        preferencesRepository.setThemeMode(themeMode)
    }
}
