package com.ticklog.ui.feature_statistics

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ticklog.R
import com.ticklog.core.designsystem.component.EmptyState
import com.ticklog.core.designsystem.component.TickTopAppBar

/**
 * Statistics destination — streaks and completion trends.
 *
 * Phase 1 ships the navigable shell and empty state; the charts and aggregates
 * (powered by the completion-history ledger) follow in Phase 2.
 *
 * @param onNavigateUp invoked when the user taps the back arrow.
 * @param modifier external layout modifier.
 */
@Composable
fun StatisticsScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TickTopAppBar(
                title = stringResource(R.string.destination_statistics),
                onNavigateUp = onNavigateUp,
            )
        },
    ) { innerPadding ->
        EmptyState(
            icon = Icons.Outlined.Insights,
            title = stringResource(R.string.statistics_empty_title),
            subtitle = stringResource(R.string.statistics_empty_subtitle),
            modifier = Modifier.padding(innerPadding),
        )
    }
}
