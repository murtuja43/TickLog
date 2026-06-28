package com.ticklog.ui.feature_home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.ticklog.R
import com.ticklog.core.designsystem.theme.TickLogTheme
import com.ticklog.domain.model.DailyTask
import com.ticklog.domain.model.TaskScope

/**
 * A radio group for choosing how far an edit/delete reaches.
 *
 * Only shown for template-linked tasks, since standalone tasks are day-local.
 */
@Composable
fun TaskScopeSelector(
    selected: TaskScope,
    onSelected: (TaskScope) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.selectableGroup()) {
        ScopeOption(
            label = stringResource(R.string.scope_today_only),
            selected = selected == TaskScope.TODAY_ONLY,
            onClick = { onSelected(TaskScope.TODAY_ONLY) },
        )
        ScopeOption(
            label = stringResource(R.string.scope_today_and_future),
            selected = selected == TaskScope.TODAY_AND_FUTURE,
            onClick = { onSelected(TaskScope.TODAY_AND_FUTURE) },
        )
    }
}

@Composable
private fun ScopeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = TickLogTheme.spacing.small),
        )
    }
}

/**
 * The long-press actions sheet for a task: edit, duplicate, delete.
 *
 * Each action simply raises an intent; the parent decides what dialog (if any)
 * to open next, keeping this component free of business logic.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskActionsSheet(
    task: DailyTask,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(bottom = TickLogTheme.spacing.large),
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    horizontal = TickLogTheme.spacing.large,
                    vertical = TickLogTheme.spacing.small,
                ),
            )
            SheetAction(
                icon = Icons.Outlined.Edit,
                label = stringResource(R.string.action_edit),
                onClick = onEdit,
            )
            SheetAction(
                icon = Icons.Outlined.ContentCopy,
                label = stringResource(R.string.action_duplicate),
                onClick = onDuplicate,
            )
            SheetAction(
                icon = Icons.Outlined.DeleteOutline,
                label = stringResource(R.string.action_delete),
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun SheetAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = TickLogTheme.spacing.large),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.large),
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * A confirmation dialog for deleting a task. Deletion is never instant: the user
 * confirms here, and an undo snackbar follows. A scope choice is offered only
 * for template-linked tasks.
 *
 * @param task the task being deleted (drives whether a scope choice is shown).
 * @param onConfirm invoked with the chosen [TaskScope] when the user confirms.
 * @param onDismiss invoked when cancelled.
 */
@Composable
fun DeleteTaskDialog(
    task: DailyTask,
    onConfirm: (TaskScope) -> Unit,
    onDismiss: () -> Unit,
) {
    var scope by remember { mutableStateOf(TaskScope.TODAY_ONLY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                text = stringResource(R.string.delete_dialog_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.small)) {
                Text(
                    text = stringResource(R.string.delete_dialog_message, task.title),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (task.isLinkedToTemplate) {
                    TaskScopeSelector(
                        selected = scope,
                        onSelected = { scope = it },
                        modifier = Modifier.padding(top = TickLogTheme.spacing.small),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(if (task.isLinkedToTemplate) scope else TaskScope.TODAY_ONLY)
                },
            ) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
