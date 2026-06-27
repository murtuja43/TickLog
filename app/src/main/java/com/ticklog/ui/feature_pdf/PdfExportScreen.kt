package com.ticklog.ui.feature_pdf

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ticklog.R
import com.ticklog.core.designsystem.component.EmptyState
import com.ticklog.core.designsystem.component.TickTopAppBar

/**
 * PDF export destination.
 *
 * Phase 1 reserves the screen and its place in the navigation graph; the actual
 * document generation (rendering history to a shareable PDF) is implemented in
 * a later phase.
 *
 * @param onNavigateUp invoked when the user taps the back arrow.
 * @param modifier external layout modifier.
 */
@Composable
fun PdfExportScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TickTopAppBar(
                title = stringResource(R.string.destination_pdf),
                onNavigateUp = onNavigateUp,
            )
        },
    ) { innerPadding ->
        EmptyState(
            icon = Icons.Outlined.PictureAsPdf,
            title = stringResource(R.string.pdf_empty_title),
            subtitle = stringResource(R.string.pdf_empty_subtitle),
            modifier = Modifier.padding(innerPadding),
        )
    }
}
