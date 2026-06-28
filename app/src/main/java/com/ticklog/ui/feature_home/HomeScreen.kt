package com.ticklog.ui.feature_home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import com.ticklog.R
import com.ticklog.core.designsystem.component.DateHeader
import com.ticklog.core.designsystem.component.EmptyState
import com.ticklog.core.designsystem.component.LoadingIndicator
import com.ticklog.core.designsystem.component.TaskInputDialog
import com.ticklog.core.designsystem.component.TickTopAppBar
import com.ticklog.core.designsystem.theme.TickLogTheme
import com.ticklog.domain.model.DailyTask
import com.ticklog.domain.model.TaskScope
import com.ticklog.util.DateTimeFormatters
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Home screen — the primary daily surface and the heart of the app.
 *
 * A [HorizontalPager] anchored on today provides infinite-feeling day navigation;
 * each visible page observes its own day's checklist, so swiping reveals real
 * data with an animated date header. Tasks are ticked optimistically, edited via
 * a long-press sheet, added through the FAB, and deleted with confirmation +
 * undo. All real work lives in [HomeViewModel].
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
    val today = viewModel.today
    val scheduleRange by viewModel.scheduleRange.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(initialPage = DAY_PAGER_ANCHOR) { DAY_PAGER_PAGE_COUNT }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentDate = today.dateForPage(pagerState.currentPage)
    val isExpandedWidth = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val canAddOnCurrentDate = scheduleRange?.contains(currentDate) == true

    // Transient UI state: the long-press sheet target and the active dialog.
    var sheetTask by remember { mutableStateOf<DailyTask?>(null) }
    var dialog by remember { mutableStateOf<HomeDialog?>(null) }

    // Show the undo snackbar whenever a delete happens.
    val undoLabel = stringResource(R.string.action_undo)
    val deletedMessage = stringResource(R.string.home_deleted_snackbar)
    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.TasksDeleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = deletedMessage,
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete()
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TickTopAppBar(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = onNavigateToCalendar) {
                        Icon(Icons.Outlined.CalendarMonth, stringResource(R.string.destination_calendar))
                    }
                    IconButton(onClick = onNavigateToStatistics) {
                        Icon(Icons.Outlined.Insights, stringResource(R.string.destination_statistics))
                    }
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Outlined.History, stringResource(R.string.destination_history))
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Outlined.Settings, stringResource(R.string.destination_settings))
                    }
                },
            )
        },
        floatingActionButton = {
            // The FAB only appears on days the user can actually add to.
            AnimatedVisibility(
                visible = canAddOnCurrentDate,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                FloatingActionButton(
                    onClick = { dialog = HomeDialog.Add(currentDate) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(Icons.Outlined.Add, stringResource(R.string.home_add_item))
                }
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

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                DayPage(
                    date = today.dateForPage(page),
                    isExpandedWidth = isExpandedWidth,
                    checklistFlowFor = viewModel::checklistFlow,
                    onToggleTask = { task -> viewModel.onToggleTask(task.id, task.isCompleted) },
                    onLongPressTask = { task -> sheetTask = task },
                    onAddRequested = { date -> dialog = HomeDialog.Add(date) },
                )
            }
        }
    }

    // Long-press actions sheet.
    sheetTask?.let { task ->
        TaskActionsSheet(
            task = task,
            onEdit = { sheetTask = null; dialog = HomeDialog.Edit(task) },
            onDuplicate = { sheetTask = null; viewModel.duplicateTask(task.id) },
            onDelete = { sheetTask = null; dialog = HomeDialog.Delete(task) },
            onDismiss = { sheetTask = null },
        )
    }

    // Add / edit / delete dialogs.
    when (val current = dialog) {
        is HomeDialog.Add -> AddTaskDialog(
            onConfirm = { title, note, taskScope ->
                viewModel.addTask(current.date, title, note, taskScope)
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        is HomeDialog.Edit -> EditTaskDialog(
            task = current.task,
            onConfirm = { title, note, taskScope ->
                viewModel.editTask(current.task.id, title, note, taskScope)
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        is HomeDialog.Delete -> DeleteTaskDialog(
            task = current.task,
            onConfirm = { taskScope ->
                viewModel.deleteTask(current.task.id, taskScope)
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        null -> Unit
    }
}

/** The active Home dialog, if any. */
private sealed interface HomeDialog {
    data class Add(val date: LocalDate) : HomeDialog
    data class Edit(val task: DailyTask) : HomeDialog
    data class Delete(val task: DailyTask) : HomeDialog
}

/**
 * A single day page: observes its own day's state and renders the checklist,
 * an empty state, or an out-of-range state.
 */
@Composable
private fun DayPage(
    date: LocalDate,
    isExpandedWidth: Boolean,
    checklistFlowFor: (LocalDate) -> Flow<DayUiState>,
    onToggleTask: (DailyTask) -> Unit,
    onLongPressTask: (DailyTask) -> Unit,
    onAddRequested: (LocalDate) -> Unit,
) {
    val dayState by remember(date) { checklistFlowFor(date) }
        .collectAsStateWithLifecycle(initialValue = DayUiState.Loading(date))

    val horizontalPadding =
        if (isExpandedWidth) TickLogTheme.spacing.huge else TickLogTheme.spacing.large

    when (val state = dayState) {
        is DayUiState.Loading -> LoadingIndicator()

        is DayUiState.OutOfRange -> EmptyState(
            icon = Icons.Outlined.EventBusy,
            title = stringResource(R.string.home_out_of_range_title),
            subtitle = stringResource(R.string.home_out_of_range_subtitle),
            modifier = Modifier.padding(horizontal = horizontalPadding),
        )

        is DayUiState.Empty -> EmptyState(
            icon = Icons.Outlined.TaskAlt,
            title = stringResource(R.string.home_empty_title),
            subtitle = stringResource(R.string.home_empty_subtitle),
            modifier = Modifier.padding(horizontal = horizontalPadding),
            action = {
                TextButton(onClick = { onAddRequested(date) }) {
                    Text(stringResource(R.string.home_create_first_task))
                }
            },
        )

        is DayUiState.Content -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = horizontalPadding,
                vertical = TickLogTheme.spacing.small,
            ),
        ) {
            items(items = state.tasks, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    onToggle = { onToggleTask(task) },
                    onLongPress = { onLongPressTask(task) },
                )
            }
        }
    }
}

/** Add-task dialog: title + note + an apply-to scope choice. */
@Composable
private fun AddTaskDialog(
    onConfirm: (title: String, note: String?, scope: TaskScope) -> Unit,
    onDismiss: () -> Unit,
) {
    var scope by remember { mutableStateOf(TaskScope.TODAY_ONLY) }
    TaskInputDialog(
        dialogTitle = stringResource(R.string.add_task_title),
        confirmLabel = stringResource(R.string.action_add),
        onConfirm = { title, note -> onConfirm(title, note, scope) },
        onDismiss = onDismiss,
        extraContent = {
            ApplyToSection(scope = scope, onScopeSelected = { scope = it })
        },
    )
}

/** Edit-task dialog: title + note, with a scope choice for linked tasks. */
@Composable
private fun EditTaskDialog(
    task: DailyTask,
    onConfirm: (title: String, note: String?, scope: TaskScope) -> Unit,
    onDismiss: () -> Unit,
) {
    var scope by remember { mutableStateOf(TaskScope.TODAY_ONLY) }
    TaskInputDialog(
        dialogTitle = stringResource(R.string.edit_task_title),
        confirmLabel = stringResource(R.string.action_save),
        initialTitle = task.title,
        initialNote = task.note.orEmpty(),
        onConfirm = { title, note ->
            onConfirm(title, note, if (task.isLinkedToTemplate) scope else TaskScope.TODAY_ONLY)
        },
        onDismiss = onDismiss,
        extraContent = {
            if (task.isLinkedToTemplate) {
                ApplyToSection(scope = scope, onScopeSelected = { scope = it })
            }
        },
    )
}

/** "Apply to" label above a [TaskScopeSelector]. */
@Composable
private fun ApplyToSection(scope: TaskScope, onScopeSelected: (TaskScope) -> Unit) {
    Text(
        text = stringResource(R.string.apply_to_label),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    TaskScopeSelector(selected = scope, onSelected = onScopeSelected)
}

/** Maps a pager page index to its calendar date, anchored on the receiver (today). */
private fun LocalDate.dateForPage(page: Int): LocalDate =
    plusDays((page - DAY_PAGER_ANCHOR).toLong())

// A wide, fixed window of days centred on today — large enough to feel infinite.
private const val DAY_PAGER_PAGE_COUNT = 20_001
private const val DAY_PAGER_ANCHOR = DAY_PAGER_PAGE_COUNT / 2
