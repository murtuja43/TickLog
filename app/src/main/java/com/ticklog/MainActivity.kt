package com.ticklog

import android.content.Intent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
 * Responsibilities are deliberately thin: install the splash screen, enable
 * edge-to-edge, read the global [MainUiState], apply the resolved theme, compute
 * the window size class for adaptive layouts, honour any widget deep-link, and
 * hand off to the navigation graph. All real work lives in ViewModels and the
 * composables they drive.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    /**
     * A pending widget deep-link target, if the activity was launched from the
     * widget's streak tap. Held as Compose state so [onNewIntent] can re-trigger
     * navigation when the (singleTop) activity is reused.
     */
    private var pendingDestination by mutableStateOf<String?>(null)

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingDestination = intent?.destinationExtra()

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

                            // Honour a widget deep-link once the graph is ready.
                            // Only meaningful past onboarding; consumed after use
                            // so config changes never re-navigate.
                            LaunchedEffect(pendingDestination, state.onboardingCompleted) {
                                if (state.onboardingCompleted &&
                                    pendingDestination == DESTINATION_STATISTICS
                                ) {
                                    pendingDestination = null
                                    navController.navigate(TickDestination.Statistics.route) {
                                        launchSingleTop = true
                                    }
                                }
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDestination = intent.destinationExtra()
    }

    private fun Intent.destinationExtra(): String? = getStringExtra(EXTRA_DESTINATION)

    companion object {
        /** Intent extra naming the screen a widget tap should open. */
        const val EXTRA_DESTINATION: String = "com.ticklog.extra.DESTINATION"

        /** [EXTRA_DESTINATION] value that opens the Statistics screen. */
        const val DESTINATION_STATISTICS: String = "statistics"
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
