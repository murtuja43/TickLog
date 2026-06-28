package com.ticklog.ui.feature_onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ticklog.R
import com.ticklog.core.designsystem.component.EmptyState
import com.ticklog.core.designsystem.component.PrimaryButton
import com.ticklog.core.designsystem.component.ResponsiveContainer
import com.ticklog.core.designsystem.component.TaskInputDialog
import com.ticklog.core.designsystem.component.TickCard
import com.ticklog.core.designsystem.component.TickTopAppBar
import com.ticklog.core.designsystem.theme.TickLogTheme
import com.ticklog.util.DateTimeFormatters

/**
 * Onboarding step 2 (stateful entry point): the checklist builder.
 *
 * Lets the user assemble unlimited items — add, edit, duplicate, delete and
 * reorder — then create everything with "Create My Checklist". Navigation home
 * fires exactly once, after creation succeeds.
 *
 * @param onChecklistCreated invoked once the checklist has been generated.
 * @param onNavigateUp invoked to return to the date step.
 * @param modifier external layout modifier.
 */
@Composable
fun ChecklistBuilderScreen(
    onChecklistCreated: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChecklistBuilderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.created.collect { onChecklistCreated() }
    }

    ChecklistBuilderContent(
        uiState = uiState,
        rangeSummary = DateTimeFormatters.rangeSummary(
            viewModel.dateRange.start,
            viewModel.dateRange.end,
        ),
        onAddItem = viewModel::addItem,
        onUpdateItem = viewModel::updateItem,
        onDeleteItem = viewModel::deleteItem,
        onDuplicateItem = viewModel::duplicateItem,
        onMoveUp = viewModel::moveUp,
        onMoveDown = viewModel::moveDown,
        onCreate = viewModel::onCreateClicked,
        onNavigateUp = onNavigateUp,
        modifier = modifier,
    )
}

/** Which builder dialog, if any, is currently open. */
private sealed interface BuilderDialog {
    data object Add : BuilderDialog
    data class Edit(val item: BuilderItem) : BuilderDialog
}

/**
 * Stateless builder UI.
 */
@Composable
private fun ChecklistBuilderContent(
    uiState: ChecklistBuilderUiState,
    rangeSummary: String,
    onAddItem: (String, String?) -> Unit,
    onUpdateItem: (Long, String, String?) -> Unit,
    onDeleteItem: (Long) -> Unit,
    onDuplicateItem: (Long) -> Unit,
    onMoveUp: (Long) -> Unit,
    onMoveDown: (Long) -> Unit,
    onCreate: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialog by remember { mutableStateOf<BuilderDialog?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TickTopAppBar(
                title = stringResource(R.string.builder_title),
                onNavigateUp = onNavigateUp,
                actions = {
                    IconButton(onClick = { dialog = BuilderDialog.Add }) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.builder_add_item),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                PrimaryButton(
                    text = stringResource(R.string.builder_create),
                    onClick = onCreate,
                    enabled = uiState.canCreate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(TickLogTheme.spacing.large),
                )
            }
        },
    ) { innerPadding ->
        ResponsiveContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (uiState.items.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.TaskAlt,
                    title = stringResource(R.string.builder_empty_title),
                    subtitle = stringResource(R.string.builder_empty_subtitle),
                    action = {
                        PrimaryButton(
                            text = stringResource(R.string.builder_add_item),
                            onClick = { dialog = BuilderDialog.Add },
                            leadingIcon = Icons.Outlined.Add,
                        )
                    },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = TickLogTheme.spacing.large,
                        vertical = TickLogTheme.spacing.medium,
                    ),
                    verticalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.medium),
                ) {
                    item(key = "summary") {
                        Text(
                            text = rangeSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = TickLogTheme.spacing.small),
                        )
                    }
                    itemsIndexed(
                        items = uiState.items,
                        key = { _, item -> item.id },
                    ) { index, item ->
                        BuilderItemCard(
                            item = item,
                            isFirst = index == 0,
                            isLast = index == uiState.items.lastIndex,
                            onEdit = { dialog = BuilderDialog.Edit(item) },
                            onDuplicate = { onDuplicateItem(item.id) },
                            onDelete = { onDeleteItem(item.id) },
                            onMoveUp = { onMoveUp(item.id) },
                            onMoveDown = { onMoveDown(item.id) },
                        )
                    }
                }
            }
        }
    }

    when (val current = dialog) {
        BuilderDialog.Add -> TaskInputDialog(
            dialogTitle = stringResource(R.string.builder_add_item),
            confirmLabel = stringResource(R.string.action_add),
            onConfirm = { title, note ->
                onAddItem(title, note)
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        is BuilderDialog.Edit -> TaskInputDialog(
            dialogTitle = stringResource(R.string.builder_edit_item),
            confirmLabel = stringResource(R.string.action_save),
            initialTitle = current.item.title,
            initialNote = current.item.note.orEmpty(),
            onConfirm = { title, note ->
                onUpdateItem(current.item.id, title, note)
                dialog = null
            },
            onDismiss = { dialog = null },
        )

        null -> Unit
    }
}

/** A single builder item with its reorder/edit/duplicate/delete controls. */
@Composable
private fun BuilderItemCard(
    item: BuilderItem,
    isFirst: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    TickCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (!item.note.isNullOrBlank()) {
            Text(
                text = item.note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = TickLogTheme.spacing.extraSmall),
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = TickLogTheme.spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onMoveUp, enabled = !isFirst) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.builder_move_up),
                )
            }
            IconButton(onClick = onMoveDown, enabled = !isLast) {
                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.builder_move_down),
                )
            }
            Box(modifier = Modifier.weight(1f))
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.action_edit),
                )
            }
            IconButton(onClick = onDuplicate) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.action_duplicate),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.action_delete),
                )
            }
        }
    }
}
