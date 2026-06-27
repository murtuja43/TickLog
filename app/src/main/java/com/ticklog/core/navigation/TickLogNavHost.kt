package com.ticklog.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ticklog.ui.feature_calendar.CalendarScreen
import com.ticklog.ui.feature_history.HistoryScreen
import com.ticklog.ui.feature_home.HomeScreen
import com.ticklog.ui.feature_onboarding.OnboardingScreen
import com.ticklog.ui.feature_pdf.PdfExportScreen
import com.ticklog.ui.feature_settings.SettingsScreen
import com.ticklog.ui.feature_statistics.StatisticsScreen

/**
 * The single navigation graph for the app.
 *
 * Each destination from [TickDestination] is wired here to its feature screen,
 * with navigation intent flowing in through typed lambdas — features never hold
 * a [NavHostController] themselves, which keeps them isolated and previewable.
 *
 * A consistent, gentle fade is applied to every transition so movement between
 * screens feels calm and premium rather than jarring.
 *
 * @param navController controller backing the graph.
 * @param startDestination the route to show first (onboarding vs. home).
 * @param windowSizeClass current window size, forwarded to screens that adapt
 *   their layout for tablets and landscape.
 * @param modifier external layout modifier (typically carries Scaffold padding).
 */
@Composable
fun TickLogNavHost(
    navController: NavHostController,
    startDestination: String,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(FADE_DURATION_MS)) },
        exitTransition = { fadeOut(animationSpec = tween(FADE_DURATION_MS)) },
        popEnterTransition = { fadeIn(animationSpec = tween(FADE_DURATION_MS)) },
        popExitTransition = { fadeOut(animationSpec = tween(FADE_DURATION_MS)) },
    ) {
        composable(route = TickDestination.Onboarding.route) {
            OnboardingScreen(
                windowSizeClass = windowSizeClass,
                onOnboardingComplete = {
                    navController.navigate(TickDestination.Home.route) {
                        // Onboarding is one-shot: remove it from the back stack.
                        popUpTo(TickDestination.Onboarding.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(route = TickDestination.Home.route) {
            HomeScreen(
                windowSizeClass = windowSizeClass,
                onNavigateToCalendar = { navController.navigateTo(TickDestination.Calendar) },
                onNavigateToHistory = { navController.navigateTo(TickDestination.History) },
                onNavigateToStatistics = { navController.navigateTo(TickDestination.Statistics) },
                onNavigateToSettings = { navController.navigateTo(TickDestination.Settings) },
            )
        }

        composable(route = TickDestination.Calendar.route) {
            CalendarScreen(onNavigateUp = navController::navigateUp)
        }

        composable(route = TickDestination.History.route) {
            HistoryScreen(
                onNavigateUp = navController::navigateUp,
                onExportPdf = { navController.navigateTo(TickDestination.PdfExport) },
            )
        }

        composable(route = TickDestination.Statistics.route) {
            StatisticsScreen(onNavigateUp = navController::navigateUp)
        }

        composable(route = TickDestination.Settings.route) {
            SettingsScreen(
                onNavigateUp = navController::navigateUp,
                onNavigateToPdfExport = { navController.navigateTo(TickDestination.PdfExport) },
            )
        }

        composable(route = TickDestination.PdfExport.route) {
            PdfExportScreen(onNavigateUp = navController::navigateUp)
        }
    }
}

/** Navigates to a top-level [destination], collapsing duplicate entries. */
private fun NavHostController.navigateTo(destination: TickDestination) {
    navigate(destination.route) { launchSingleTop = true }
}

private const val FADE_DURATION_MS = 280
