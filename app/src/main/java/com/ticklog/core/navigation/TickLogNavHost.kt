package com.ticklog.core.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ticklog.core.designsystem.LocalAnimationsEnabled
import com.ticklog.ui.feature_backup.BackupScreen
import com.ticklog.ui.feature_calendar.CalendarScreen
import com.ticklog.ui.feature_history.HistoryScreen
import com.ticklog.ui.feature_home.HomeScreen
import com.ticklog.ui.feature_onboarding.ChecklistBuilderScreen
import com.ticklog.ui.feature_onboarding.OnboardingScreen
import com.ticklog.ui.feature_pdf.PdfExportScreen
import com.ticklog.ui.feature_settings.LicensesScreen
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
    // Honour the user's "animations" preference: gentle fade, or an instant cut.
    val duration = if (LocalAnimationsEnabled.current) FADE_DURATION_MS else 0
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(duration)) },
        exitTransition = { fadeOut(animationSpec = tween(duration)) },
        popEnterTransition = { fadeIn(animationSpec = tween(duration)) },
        popExitTransition = { fadeOut(animationSpec = tween(duration)) },
    ) {
        composable(route = TickDestination.Onboarding.route) {
            OnboardingScreen(
                windowSizeClass = windowSizeClass,
                onContinue = { range ->
                    navController.navigate(
                        TickDestination.OnboardingItems.routeFor(range.start, range.end),
                    )
                },
            )
        }

        composable(
            route = TickDestination.OnboardingItems.route,
            arguments = listOf(
                navArgument(ARG_START) { type = NavType.LongType },
                navArgument(ARG_END) { type = NavType.LongType },
            ),
        ) {
            ChecklistBuilderScreen(
                onChecklistCreated = {
                    navController.navigate(TickDestination.Home.DEFAULT) {
                        // The whole onboarding flow is one-shot: clear it entirely.
                        popUpTo(TickDestination.Onboarding.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateUp = navController::navigateUp,
            )
        }

        composable(
            route = TickDestination.Home.route,
            arguments = listOf(
                navArgument(ARG_DATE) {
                    type = NavType.LongType
                    defaultValue = HOME_NO_DATE
                },
            ),
        ) {
            HomeScreen(
                windowSizeClass = windowSizeClass,
                onNavigateToCalendar = { navController.navigateTo(TickDestination.Calendar) },
                onNavigateToHistory = { navController.navigateTo(TickDestination.History) },
                onNavigateToStatistics = { navController.navigateTo(TickDestination.Statistics) },
                onNavigateToSettings = { navController.navigateTo(TickDestination.Settings) },
            )
        }

        composable(route = TickDestination.Calendar.route) {
            CalendarScreen(
                onNavigateUp = navController::navigateUp,
                onDaySelected = { date ->
                    // Open the chosen day on Home, replacing the calendar + old Home.
                    navController.navigate(TickDestination.Home.routeForDate(date)) {
                        popUpTo(TickDestination.Home.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(route = TickDestination.History.route) {
            HistoryScreen(onNavigateUp = navController::navigateUp)
        }

        composable(route = TickDestination.Statistics.route) {
            StatisticsScreen(onNavigateUp = navController::navigateUp)
        }

        composable(route = TickDestination.Settings.route) {
            SettingsScreen(
                onNavigateUp = navController::navigateUp,
                onNavigateToPdfExport = { navController.navigateTo(TickDestination.PdfExport) },
                onNavigateToBackup = { navController.navigateTo(TickDestination.Backup) },
                onNavigateToLicenses = { navController.navigateTo(TickDestination.Licenses) },
                onOnboardingReset = {
                    navController.navigate(TickDestination.Onboarding.route) {
                        // Clear the entire back stack and return to first-run.
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(route = TickDestination.PdfExport.route) {
            PdfExportScreen(onNavigateUp = navController::navigateUp)
        }

        composable(route = TickDestination.Backup.route) {
            BackupScreen(onNavigateUp = navController::navigateUp)
        }

        composable(route = TickDestination.Licenses.route) {
            LicensesScreen(onNavigateUp = navController::navigateUp)
        }
    }
}

/** Navigates to a top-level [destination], collapsing duplicate entries. */
private fun NavHostController.navigateTo(destination: TickDestination) {
    navigate(destination.route) { launchSingleTop = true }
}

private const val FADE_DURATION_MS = 280
