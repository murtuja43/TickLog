package com.ticklog.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ticklog.R
import com.ticklog.domain.model.MAX_TASK_NOTE_LENGTH
import com.ticklog.domain.model.MAX_TASK_TITLE_LENGTH

/**
 * A reusable dialog for entering or editing a task's title and optional note.
 *
 * It owns only ephemeral text-field state, enforces the domain length limits as
 * the user types, and raises the trimmed result through [onConfirm]. An optional
 * [extraContent] slot lets callers inject extra controls (e.g. a scope selector)
 * directly above the action buttons, so the same dialog serves the onboarding
 * builder, "add task", and "edit task" flows without duplication.
 *
 * @param dialogTitle heading shown at the top of the dialog.
 * @param confirmLabel label for the confirm button.
 * @param onConfirm receives the trimmed title and the trimmed note (null if blank).
 * @param onDismiss invoked when the dialog is dismissed without confirming.
 * @param initialTitle pre-filled title (for editing).
 * @param initialNote pre-filled note (for editing).
 * @param showNoteField whether to show the note field.
 * @param extraContent optional controls rendered between the fields and buttons.
 */
@Composable
fun TaskInputDialog(
    dialogTitle: String,
    confirmLabel: String,
    onConfirm: (title: String, note: String?) -> Unit,
    onDismiss: () -> Unit,
    initialTitle: String = "",
    initialNote: String = "",
    showNoteField: Boolean = true,
    extraContent: @Composable ColumnScope.() -> Unit = {},
) {
    // rememberSaveable so in-progress text survives configuration changes.
    var title by rememberSaveable(initialTitle) { mutableStateOf(initialTitle) }
    var note by rememberSaveable(initialNote) { mutableStateOf(initialNote) }

    val isTitleValid = remember(title) { title.trim().isNotEmpty() }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(text = dialogTitle, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= MAX_TASK_TITLE_LENGTH) title = it },
                    label = { Text(stringResource(R.string.task_title_label)) },
                    singleLine = false,
                    isError = title.isNotEmpty() && !isTitleValid,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showNoteField) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { if (it.length <= MAX_TASK_NOTE_LENGTH) note = it },
                        label = { Text(stringResource(R.string.task_note_label)) },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                extraContent()
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title.trim(), note.trim().ifEmpty { null }) },
                enabled = isTitleValid,
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
