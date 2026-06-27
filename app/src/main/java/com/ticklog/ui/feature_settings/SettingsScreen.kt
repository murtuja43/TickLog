package com.ticklog.ui.feature_settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ticklog.R
import com.ticklog.core.designsystem.component.ResponsiveContainer
import com.ticklog.core.designsystem.component.SectionTitle
import com.ticklog.core.designsystem.component.TickFilledCard
import com.ticklog.core.designsystem.component.TickTopAppBar
import com.ticklog.core.designsystem.theme.TickLogTheme
import com.ticklog.domain.model.ThemeMode

/**
 * Settings screen (stateful entry point).
 *
 * Obtains its [SettingsViewModel] via Hilt, collects state in a lifecycle-aware
 * way and forwards everything to the stateless [SettingsContent]. This split is
 * the state-hoisting pattern used across the app: the content is fully driven by
 * its parameters and is trivial to preview and test.
 */
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToPdfExport: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsContent(
        uiState = uiState,
        onThemeModeSelected = viewModel::onThemeModeSelected,
        onNavigateUp = onNavigateUp,
        onNavigateToPdfExport = onNavigateToPdfExport,
        modifier = modifier,
    )
}

/**
 * Stateless settings UI.
 *
 * @param uiState the snapshot to render.
 * @param onThemeModeSelected raised when the user picks a theme.
 * @param onNavigateUp raised on back navigation.
 * @param onNavigateToPdfExport raised when the user opens PDF export.
 * @param modifier external layout modifier.
 */
@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateToPdfExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TickTopAppBar(
                title = stringResource(R.string.destination_settings),
                onNavigateUp = onNavigateUp,
            )
        },
    ) { innerPadding ->
        ResponsiveContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = TickLogTheme.spacing.large,
                        vertical = TickLogTheme.spacing.medium,
                    ),
                verticalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.large),
            ) {
                AppearanceSection(
                    selected = uiState.themeMode,
                    onThemeModeSelected = onThemeModeSelected,
                )

                AboutSection(
                    appVersion = uiState.appVersion,
                    onNavigateToPdfExport = onNavigateToPdfExport,
                )
            }
        }
    }
}

/** The theme-selection group. */
@Composable
private fun AppearanceSection(
    selected: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.small)) {
        SectionTitle(title = stringResource(R.string.settings_appearance))

        TickFilledCard {
            Column(modifier = Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    ThemeOptionRow(
                        mode = mode,
                        selected = mode == selected,
                        onSelect = { onThemeModeSelected(mode) },
                    )
                }
            }
        }
    }
}

/** A single selectable theme row. */
@Composable
private fun ThemeOptionRow(
    mode: ThemeMode,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(text = mode.label()) },
        leadingContent = {
            RadioButton(selected = selected, onClick = onSelect)
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onSelect,
            ),
    )
}

/** The About / version group with the export entry point. */
@Composable
private fun AboutSection(
    appVersion: String,
    onNavigateToPdfExport: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.small)) {
        SectionTitle(title = stringResource(R.string.settings_about))

        TickFilledCard {
            ListItem(
                headlineContent = { Text(text = stringResource(R.string.destination_pdf)) },
                leadingContent = {
                    Icon(imageVector = Icons.Outlined.PictureAsPdf, contentDescription = null)
                },
                trailingContent = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onNavigateToPdfExport),
            )

            ListItem(
                headlineContent = { Text(text = stringResource(R.string.settings_version)) },
                trailingContent = {
                    Text(
                        text = appVersion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        }
    }
}

/** Maps a [ThemeMode] to its localised label. */
@Composable
private fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    },
)
