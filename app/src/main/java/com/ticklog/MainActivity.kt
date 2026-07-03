package com.ticklog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.ticklog.core.designsystem.LocalAnimationsEnabled
import com.ticklog.core.designsystem.LocalDateFormat
import com.ticklog.core.designsystem.LocalWeekStart
import com.ticklog.core.designsystem.component.LoadingIndicator
import com.ticklog.core.designsystem.theme.TickLogTheme
import com.ticklog.core.navigation.TickDestination
import com.ticklog.core.navigation.TickLogNavHost
import com.ticklog.domain.model.ThemeMode
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single Activity hosting the entire Compose UI.
 *
 * Responsibilities are deliberately thin: enable edge-to-edge, read the global
 * [MainUiState], apply the resolved theme, compute the window size class for
 * adaptive layouts, and hand off to the navigation graph. All real work lives in
 * ViewModels and the composables they drive.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = uiState.resolveDarkTheme(systemDark)

            TickLogTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val windowSizeClass = calculateWindowSizeClass(this)

                    when (val state = uiState) {
                        MainUiState.Loading -> LoadingIndicator()

                        is MainUiState.Ready -> {
                            val navController = rememberNavController()
                            val startDestination =
                                if (state.onboardingCompleted) {
                                    TickDestination.Home.route
                                } else {
                                    TickDestination.Onboarding.route
                                }

                            CompositionLocalProvider(
                                LocalAnimationsEnabled provides state.animationsEnabled,
                                LocalWeekStart provides state.weekStart,
                                LocalDateFormat provides state.dateFormat,
                            ) {
                                TickLogNavHost(
                                    navController = navController,
                                    startDestination = startDestination,
                                    windowSizeClass = windowSizeClass,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Resolves whether dark colours should be used for the current [MainUiState],
 * honouring an explicit [ThemeMode] choice and falling back to the system
 * setting (including during the brief [MainUiState.Loading] phase).
 */
private fun MainUiState.resolveDarkTheme(systemDark: Boolean): Boolean = when (this) {
    MainUiState.Loading -> systemDark
    is MainUiState.Ready -> when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}
