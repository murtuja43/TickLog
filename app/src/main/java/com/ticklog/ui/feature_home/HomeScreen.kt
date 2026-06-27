package com.ticklog.ui.feature_home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ticklog.R
import com.ticklog.core.designsystem.component.DateHeader
import com.ticklog.core.designsystem.component.EmptyState
import com.ticklog.core.designsystem.component.LoadingIndicator
import com.ticklog.core.designsystem.component.TickTopAppBar
import com.ticklog.core.designsystem.theme.TickLogTheme
import com.ticklog.util.DateTimeFormatters
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Home screen — the primary daily surface.
 *
 * Day navigation is built on a [HorizontalPager] anchored on today, so swiping
 * left/right moves between days; the header arrows animate the same pager. This
 * is the "swipe between days" capability the brief asks to be prepared — it is
 * wired end-to-end here, ready for Phase 2 to fill each page with real tasks.
 * Today the page content is the empty placeholder.
 *
 * @param windowSizeClass current window size, used to relax padding on large screens.
 * @param onNavigateToCalendar opens the calendar destination.
 * @param onNavigateToHistory opens the history destination.
 * @param onNavigateToStatistics opens the statistics destination.
 * @param onNavigateToSettings opens the settings destination.
 * @param modifier external layout modifier.
 */
@Composable
fun HomeScreen(
    windowSizeClass: WindowSizeClass,
    onNavigateToCalendar: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val today = viewModel.today

    val pagerState = rememberPagerState(initialPage = DAY_PAGER_ANCHOR) { DAY_PAGER_PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val fabMessage = stringResource(R.string.home_fab_placeholder)

    // Translate the settled page into a date and notify the ViewModel so the
    // correct day's checklist is observed.
    LaunchedEffect(pagerState, today) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            viewModel.onDateSelected(today.dateForPage(page))
        }
    }

    val currentDate = today.dateForPage(pagerState.currentPage)
    val isExpandedWidth = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    Scaffold(
        modifier = modifier,
        topBar = {
            TickTopAppBar(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = onNavigateToCalendar) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = stringResource(R.string.destination_calendar),
                        )
                    }
                    IconButton(onClick = onNavigateToStatistics) {
                        Icon(
                            imageVector = Icons.Outlined.Insights,
                            contentDescription = stringResource(R.string.destination_statistics),
                        )
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = stringResource(R.string.destination_history),
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.destination_settings),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { scope.launch { snackbarHostState.showSnackbar(fabMessage) } },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.home_add_item),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            DateHeader(
                primaryText = DateTimeFormatters.headline(currentDate),
                secondaryText = DateTimeFormatters.relativeLabel(currentDate, today),
                modifier = Modifier.padding(
                    horizontal = TickLogTheme.spacing.small,
                    vertical = TickLogTheme.spacing.medium,
                ),
                onPreviousClick = {
                    scope.launch {
                        pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                    }
                },
                onNextClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            (pagerState.currentPage + 1).coerceAtMost(DAY_PAGER_PAGE_COUNT - 1),
                        )
                    }
                },
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                HomeDayContent(
                    pageDate = today.dateForPage(page),
                    uiState = uiState,
                    isExpandedWidth = isExpandedWidth,
                )
            }
        }
    }
}

/**
 * The content of a single day page.
 *
 * Renders a loading indicator only for the focused, still-loading day; every
 * other case resolves to the empty placeholder in Phase 1. Once tasks exist, the
 * populated branch is added here without touching the surrounding pager.
 *
 * @param pageDate the date this page represents.
 * @param uiState the focused day's state (matches [pageDate] when settled).
 * @param isExpandedWidth whether to use roomier large-screen padding.
 */
@Composable
private fun HomeDayContent(
    pageDate: LocalDate,
    uiState: HomeUiState,
    isExpandedWidth: Boolean,
) {
    val horizontalPadding =
        if (isExpandedWidth) TickLogTheme.spacing.huge else TickLogTheme.spacing.large

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding),
    ) {
        val isFocusedAndLoading = pageDate == uiState.selectedDate && uiState.isLoading
        if (isFocusedAndLoading) {
            LoadingIndicator()
        } else {
            EmptyState(
                icon = Icons.Outlined.Add,
                title = stringResource(R.string.home_empty_title),
                subtitle = stringResource(R.string.home_empty_subtitle),
            )
        }
    }
}

/** Maps a pager page index to its calendar date, anchored on the receiver (today). */
private fun LocalDate.dateForPage(page: Int): LocalDate =
    plusDays((page - DAY_PAGER_ANCHOR).toLong())

// The pager spans a wide, fixed window of days centred on today. This is large
// enough to feel "infinite" for a daily app while keeping page indices stable.
private const val DAY_PAGER_PAGE_COUNT = 20_001
private const val DAY_PAGER_ANCHOR = DAY_PAGER_PAGE_COUNT / 2
