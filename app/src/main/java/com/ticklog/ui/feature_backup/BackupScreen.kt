package com.ticklog.ui.feature_backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ticklog.R
import com.ticklog.core.designsystem.component.PrimaryButton
import com.ticklog.core.designsystem.component.ResponsiveContainer
import com.ticklog.core.designsystem.component.SecondaryButton
import com.ticklog.core.designsystem.component.TickCard
import com.ticklog.core.designsystem.component.TickConfirmationDialog
import com.ticklog.core.designsystem.component.TickTopAppBar
import com.ticklog.core.designsystem.theme.TickLogTheme
import com.ticklog.domain.model.BackupError

/**
 * Backup & Restore destination.
 *
 * Export writes the whole app state to a file the user chooses; restore reads one
 * back — but only after an explicit confirmation, since it replaces all current
 * data. Invalid or tampered backups are rejected with a clear message and the
 * existing data left untouched.
 *
 * @param onNavigateUp invoked when the user taps the back arrow.
 * @param modifier external layout modifier.
 */
@Composable
fun BackupScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val busy by viewModel.isBusy.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val exportSucceeded = stringResource(R.string.backup_export_success)
    val exportFailed = stringResource(R.string.backup_export_failed)
    val restoreSucceeded = stringResource(R.string.backup_restore_success)
    val errorInvalid = stringResource(R.string.backup_error_invalid)
    val errorVersion = stringResource(R.string.backup_error_version)
    val errorChecksum = stringResource(R.string.backup_error_checksum)
    val errorRestore = stringResource(R.string.backup_error_restore)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::onExport) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> pendingRestoreUri = uri }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.events.collect { event ->
            val message = when (event) {
                BackupEvent.ExportSucceeded -> exportSucceeded
                BackupEvent.ExportFailed -> exportFailed
                BackupEvent.RestoreSucceeded -> restoreSucceeded
                is BackupEvent.RestoreFailed -> when (event.error) {
                    BackupError.INVALID_FILE -> errorInvalid
                    BackupError.UNSUPPORTED_VERSION -> errorVersion
                    BackupError.CHECKSUM_MISMATCH -> errorChecksum
                    BackupError.RESTORE_FAILED -> errorRestore
                }
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TickTopAppBar(
                title = stringResource(R.string.destination_backup),
                onNavigateUp = onNavigateUp,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        ResponsiveContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TickLogTheme.spacing.large),
                verticalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.large),
            ) {
                ActionCard(
                    title = stringResource(R.string.backup_export_title),
                    description = stringResource(R.string.backup_export_description),
                ) {
                    PrimaryButton(
                        text = stringResource(R.string.backup_export_action),
                        onClick = { exportLauncher.launch(viewModel.suggestedFileName()) },
                        enabled = !busy,
                        leadingIcon = Icons.Outlined.Backup,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                ActionCard(
                    title = stringResource(R.string.backup_restore_title),
                    description = stringResource(R.string.backup_restore_description),
                ) {
                    SecondaryButton(
                        text = stringResource(R.string.backup_restore_action),
                        onClick = { importLauncher.launch(arrayOf("*/*")) },
                        enabled = !busy,
                        leadingIcon = Icons.Outlined.Restore,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    // Confirm before overwriting anything.
    pendingRestoreUri?.let { uri ->
        TickConfirmationDialog(
            title = stringResource(R.string.backup_restore_confirm_title),
            message = stringResource(R.string.backup_restore_confirm_message),
            confirmText = stringResource(R.string.backup_restore_action),
            onConfirm = {
                pendingRestoreUri = null
                viewModel.onRestore(uri)
            },
            onDismiss = { pendingRestoreUri = null },
        )
    }
}

@Composable
private fun ActionCard(
    title: String,
    description: String,
    action: @Composable () -> Unit,
) {
    TickCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                top = TickLogTheme.spacing.extraSmall,
                bottom = TickLogTheme.spacing.medium,
            ),
        )
        action()
    }
}
