package com.ticklog.core.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ticklog.R

/**
 * A small family of consistent dialogs built on Material 3's [AlertDialog].
 *
 * Wrapping the platform dialog gives us one place to enforce the shared shape,
 * typography and button ordering, so dialogs never drift in style across
 * features.
 */

/**
 * A confirmation dialog with a confirm and a dismiss action.
 *
 * @param title dialog heading.
 * @param message body text explaining the consequence of confirming.
 * @param confirmText label for the confirm button.
 * @param onConfirm invoked when the user confirms.
 * @param onDismiss invoked when the user cancels or taps outside.
 * @param dismissText label for the dismiss button.
 */
@Composable
fun TickConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissText: String = stringResource(R.string.action_cancel),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(text = message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissText) }
        },
    )
}

/**
 * An informational dialog with a single acknowledgement action.
 *
 * @param title dialog heading.
 * @param message body text.
 * @param onDismiss invoked when dismissed.
 * @param confirmText label for the single button.
 */
@Composable
fun TickInfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    confirmText: String = stringResource(R.string.action_confirm),
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(text = message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(confirmText) }
        },
    )
}
