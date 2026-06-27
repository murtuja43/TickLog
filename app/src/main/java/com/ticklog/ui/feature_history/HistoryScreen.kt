package com.ticklog.ui.feature_history

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ticklog.R
import com.ticklog.core.designsystem.component.EmptyState
import com.ticklog.core.designsystem.component.TickTopAppBar

/**
 * History destination — the permanent archive of completed days.
 *
 * Phase 1 provides the navigable shell, including the PDF-export action in the
 * app bar (wired to navigation) so the export entry point exists from day one.
 * The historical timeline itself is populated in Phase 2.
 *
 * @param onNavigateUp invoked when the user taps the back arrow.
 * @param onExportPdf invoked when the user taps the export action.
 * @param modifier external layout modifier.
 */
@Composable
fun HistoryScreen(
    onNavigateUp: () -> Unit,
    onExportPdf: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TickTopAppBar(
                title = stringResource(R.string.destination_history),
                onNavigateUp = onNavigateUp,
                actions = {
                    IconButton(onClick = onExportPdf) {
                        Icon(
                            imageVector = Icons.Outlined.PictureAsPdf,
                            contentDescription = stringResource(R.string.destination_pdf),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        EmptyState(
            icon = Icons.Outlined.History,
            title = stringResource(R.string.history_empty_title),
            subtitle = stringResource(R.string.history_empty_subtitle),
            modifier = Modifier.padding(innerPadding),
        )
    }
}
