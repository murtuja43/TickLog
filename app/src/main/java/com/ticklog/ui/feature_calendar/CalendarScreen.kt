package com.ticklog.ui.feature_calendar

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ticklog.R
import com.ticklog.core.designsystem.component.EmptyState
import com.ticklog.core.designsystem.component.TickTopAppBar

/**
 * Calendar destination.
 *
 * Phase 1 ships the navigable screen with its app bar and empty state; the
 * month/range grid that lets users jump to any day arrives in Phase 2. The
 * screen is intentionally stateless — it only needs an up-navigation callback.
 *
 * @param onNavigateUp invoked when the user taps the back arrow.
 * @param modifier external layout modifier.
 */
@Composable
fun CalendarScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TickTopAppBar(
                title = stringResource(R.string.destination_calendar),
                onNavigateUp = onNavigateUp,
            )
        },
    ) { innerPadding ->
        EmptyState(
            icon = Icons.Outlined.CalendarMonth,
            title = stringResource(R.string.calendar_empty_title),
            subtitle = stringResource(R.string.calendar_empty_subtitle),
            modifier = Modifier.padding(innerPadding),
        )
    }
}
