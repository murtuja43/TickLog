package com.ticklog.ui.feature_history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ticklog.R
import com.ticklog.core.designsystem.component.EmptyState
import com.ticklog.core.designsystem.component.LoadingIndicator
import com.ticklog.core.designsystem.component.ProgressRing
import com.ticklog.core.designsystem.LocalDateFormat
import com.ticklog.core.designsystem.component.TickCard
import com.ticklog.core.designsystem.component.TickTopAppBar
import com.ticklog.core.designsystem.formatDate
import com.ticklog.core.designsystem.theme.TickLogTheme
import com.ticklog.domain.model.CompletionRecord
import com.ticklog.domain.model.DailyChecklist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToInt

/**
 * History destination — a searchable, filterable timeline of every day.
 *
 * Cards are collapsed by default and expand on tap to reveal that day's
 * completed and incomplete tasks (loaded lazily). The list is a [LazyColumn]
 * with stable keys so it scales to thousands of days; search runs in the
 * database. "Jump to date" scrolls straight to a day.
 *
 * @param onNavigateUp invoked when the user taps the back arrow.
 * @param modifier external layout modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = {
            TickTopAppBar(
                title = stringResource(R.string.destination_history),
                onNavigateUp = onNavigateUp,
                actions = {
                    if (uiState.hasAnyHistory) {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Outlined.CalendarMonth, stringResource(R.string.history_jump_to_date))
                        }
                        IconButton(
                            onClick = {
                                viewModel.onSortChange(
                                    if (uiState.sort == HistorySort.NEWEST) {
                                        HistorySort.OLDEST
                                    } else {
                                        HistorySort.NEWEST
                                    },
                                )
                            },
                        ) {
                            Icon(Icons.Outlined.SwapVert, stringResource(R.string.history_toggle_sort))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingIndicator(modifier = Modifier.padding(innerPadding))

            !uiState.hasAnyHistory -> EmptyState(
                icon = Icons.Outlined.History,
                title = stringResource(R.string.history_empty_title),
                subtitle = stringResource(R.string.history_empty_subtitle),
                modifier = Modifier.padding(innerPadding),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                SearchField(
                    query = query,
                    onQueryChange = {
                        query = it
                        viewModel.onQueryChange(it)
                    },
                )
                FilterRow(
                    selected = uiState.filter,
                    onSelected = viewModel::onFilterChange,
                )

                if (uiState.isEmptyResult) {
                    EmptyState(
                        icon = Icons.Outlined.Search,
                        title = stringResource(R.string.history_no_matches_title),
                        subtitle = stringResource(R.string.history_no_matches_subtitle),
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = TickLogTheme.spacing.large,
                            vertical = TickLogTheme.spacing.small,
                        ),
                        verticalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.small),
                    ) {
                        items(items = uiState.days, key = { it.date.toEpochDay() }) { record ->
                            HistoryDayCard(
                                record = record,
                                tasksProvider = viewModel::tasksForDate,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        JumpToDateDialog(
            onDismiss = { showDatePicker = false },
            onDateChosen = { date ->
                showDatePicker = false
                val index = uiState.days.indexOfFirst { it.date == date }
                if (index >= 0) scope.launch { listState.animateScrollToItem(index) }
            },
        )
    }
}

/** A persistent search field matching task titles and notes. */
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TickLogTheme.spacing.large, vertical = TickLogTheme.spacing.small),
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
        placeholder = { Text(stringResource(R.string.history_search_placeholder)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    )
}

/** The completion filter chips. */
@Composable
private fun FilterRow(selected: HistoryFilter, onSelected: (HistoryFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TickLogTheme.spacing.large),
        horizontalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.small),
    ) {
        FilterOption(stringResource(R.string.history_filter_all), selected == HistoryFilter.ALL) {
            onSelected(HistoryFilter.ALL)
        }
        FilterOption(stringResource(R.string.history_filter_completed), selected == HistoryFilter.COMPLETED) {
            onSelected(HistoryFilter.COMPLETED)
        }
        FilterOption(stringResource(R.string.history_filter_incomplete), selected == HistoryFilter.INCOMPLETE) {
            onSelected(HistoryFilter.INCOMPLETE)
        }
    }
}

@Composable
private fun FilterOption(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

/** One expandable day card: summary always, tasks on expand. */
@Composable
private fun HistoryDayCard(
    record: CompletionRecord,
    tasksProvider: (LocalDate) -> Flow<DailyChecklist?>,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(record.date) { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "expandRotation")
    val percent = (record.completionRate * 100).roundToInt()

    TickCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        onClick = { expanded = !expanded },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                progress = record.completionRate,
                fillWhenComplete = true,
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp),
            ) {
                Text(
                    text = "$percent%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (record.isPerfectDay) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = TickLogTheme.spacing.medium),
            ) {
                Text(
                    text = LocalDateFormat.current.formatDate(record.date),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        R.string.history_completed_of_total,
                        record.completedItems,
                        record.totalItems,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier.rotate(rotation),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        AnimatedVisibility(visible = expanded) {
            ExpandedDayTasks(date = record.date, tasksProvider = tasksProvider)
        }
    }
}

/** Lazily-loaded task breakdown shown when a card is expanded. */
@Composable
private fun ExpandedDayTasks(
    date: LocalDate,
    tasksProvider: (LocalDate) -> Flow<DailyChecklist?>,
) {
    val checklist by remember(date) { tasksProvider(date) }
        .collectAsStateWithLifecycle(initialValue = null)

    val resolved = checklist?.tasks.orEmpty()
    val completed = resolved.filter { it.isCompleted }
    val incomplete = resolved.filter { !it.isCompleted }

    Column(
        modifier = Modifier.padding(top = TickLogTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.small),
    ) {
        if (completed.isNotEmpty()) {
            TaskGroup(
                heading = stringResource(R.string.history_section_completed, completed.size),
                titles = completed.map { it.title },
            )
        }
        if (incomplete.isNotEmpty()) {
            TaskGroup(
                heading = stringResource(R.string.history_section_incomplete, incomplete.size),
                titles = incomplete.map { it.title },
            )
        }
    }
}

@Composable
private fun TaskGroup(heading: String, titles: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.extraSmall)) {
        Text(
            text = heading,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        titles.forEach { title ->
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/** A date picker used to jump to a specific day in the timeline. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JumpToDateDialog(onDismiss: () -> Unit, onDateChosen: (LocalDate) -> Unit) {
    val state = rememberDatePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onDateChosen(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
                enabled = state.selectedDateMillis != null,
            ) { Text(stringResource(R.string.action_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        DatePicker(state = state)
    }
}
