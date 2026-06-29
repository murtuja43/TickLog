package com.ticklog.ui.feature_calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ticklog.R
import com.ticklog.core.designsystem.component.EmptyState
import com.ticklog.core.designsystem.component.LoadingIndicator
import com.ticklog.core.designsystem.component.ProgressRing
import com.ticklog.core.designsystem.component.ResponsiveContainer
import com.ticklog.core.designsystem.component.TickTopAppBar
import com.ticklog.core.designsystem.theme.TickLogTheme
import com.ticklog.domain.calculator.CalendarGenerator
import com.ticklog.domain.model.CompletionRecord
import com.ticklog.util.DateTimeFormatters
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/**
 * Calendar destination — a monochrome Material 3 month calendar.
 *
 * Months are swiped through a [HorizontalPager]; the header animates as the
 * month changes. Each day shows a tiny [ProgressRing] of its completion (a solid
 * disc at 100%, a partial ring otherwise, an empty ring at 0%), with today
 * emphasised. Selecting a day opens that day's checklist.
 *
 * @param onNavigateUp invoked when the user taps the back arrow.
 * @param onDaySelected invoked with the chosen date to open its checklist.
 * @param modifier external layout modifier.
 */
@Composable
fun CalendarScreen(
    onNavigateUp: () -> Unit,
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TickTopAppBar(
                title = stringResource(R.string.destination_calendar),
                onNavigateUp = onNavigateUp,
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingIndicator(modifier = Modifier.padding(innerPadding))

            !uiState.hasData -> EmptyState(
                icon = Icons.Outlined.CalendarMonth,
                title = stringResource(R.string.calendar_empty_title),
                subtitle = stringResource(R.string.calendar_empty_subtitle),
                modifier = Modifier.padding(innerPadding),
            )

            else -> CalendarContent(
                summariesByDate = uiState.summariesByDate,
                today = uiState.today,
                onDaySelected = onDaySelected,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

@Composable
private fun CalendarContent(
    summariesByDate: Map<LocalDate, CompletionRecord>,
    today: LocalDate,
    onDaySelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val anchorMonth = remember { YearMonth.now() }
    val pagerState = rememberPagerState(initialPage = MONTH_PAGER_ANCHOR) { MONTH_PAGER_PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val displayedMonth = anchorMonth.monthForPage(pagerState.currentPage)

    ResponsiveContainer(modifier = modifier, maxContentWidth = 560.dp) {
        Column(modifier = Modifier.fillMaxSize()) {
            MonthHeader(
                month = displayedMonth,
                onPrevious = {
                    scope.launch {
                        pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0))
                    }
                },
                onNext = {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            (pagerState.currentPage + 1).coerceAtMost(MONTH_PAGER_PAGE_COUNT - 1),
                        )
                    }
                },
            )
            WeekdayHeader()
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { page ->
                MonthGrid(
                    month = anchorMonth.monthForPage(page),
                    summariesByDate = summariesByDate,
                    today = today,
                    onDaySelected = onDaySelected,
                )
            }
        }
    }
}

/** Animated month title with previous/next controls. */
@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TickLogTheme.spacing.small, vertical = TickLogTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Outlined.ChevronLeft, stringResource(R.string.calendar_previous_month))
        }
        AnimatedContent(
            targetState = month,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it / 2 } + fadeIn())
                        .togetherWith(slideOutHorizontally { -it / 2 } + fadeOut())
                } else {
                    (slideInHorizontally { -it / 2 } + fadeIn())
                        .togetherWith(slideOutHorizontally { it / 2 } + fadeOut())
                }
            },
            label = "monthLabel",
        ) { animatedMonth ->
            Text(
                text = DateTimeFormatters.monthYear(animatedMonth),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Outlined.ChevronRight, stringResource(R.string.calendar_next_month))
        }
    }
}

/** A row of short weekday labels (Mon..Sun). */
@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = TickLogTheme.spacing.small)) {
        CalendarGenerator.orderedWeekDays().forEach { dayOfWeek ->
            Text(
                text = dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** The month's day grid, laid out as whole weeks of seven equal cells. */
@Composable
private fun MonthGrid(
    month: YearMonth,
    summariesByDate: Map<LocalDate, CompletionRecord>,
    today: LocalDate,
    onDaySelected: (LocalDate) -> Unit,
) {
    val cells = remember(month) { CalendarGenerator.generate(month) }
    Column(modifier = Modifier.fillMaxWidth().padding(TickLogTheme.spacing.small)) {
        cells.chunked(CalendarGenerator.DAYS_PER_WEEK).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { cell ->
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        val date = cell.date
                        if (date != null) {
                            DayCell(
                                date = date,
                                summary = summariesByDate[date],
                                isToday = date == today,
                                onClick = { onDaySelected(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A single day: a progress ring with the day number, or a faint out-of-range number. */
@Composable
private fun DayCell(
    date: LocalDate,
    summary: CompletionRecord?,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val dayText = date.dayOfMonth.toString()
    val fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal

    if (summary == null) {
        // No checklist on this day: just a muted number, not selectable.
        Text(
            text = dayText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = fontWeight,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        return
    }

    val isComplete = summary.isPerfectDay
    val numberColor = when {
        isComplete -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(TickLogTheme.spacing.extraSmall)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        ProgressRing(
            progress = summary.completionRate,
            fillWhenComplete = true,
            strokeWidth = 3.dp,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = dayText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = fontWeight,
                color = numberColor,
            )
        }
    }
}

/** Maps a month-pager page index to its [YearMonth], anchored on [anchor]. */
private fun YearMonth.monthForPage(page: Int): YearMonth =
    plusMonths((page - MONTH_PAGER_ANCHOR).toLong())

// A wide window of months centred on the current month (≈100 years each way).
private const val MONTH_PAGER_PAGE_COUNT = 2_400
private const val MONTH_PAGER_ANCHOR = MONTH_PAGER_PAGE_COUNT / 2
