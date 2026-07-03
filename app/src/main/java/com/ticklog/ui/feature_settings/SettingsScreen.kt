package com.ticklog.ui.feature_settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ticklog.R
import com.ticklog.core.designsystem.component.SectionTitle
import com.ticklog.core.designsystem.component.TickConfirmationDialog
import com.ticklog.core.designsystem.component.TickFilledCard
import com.ticklog.core.designsystem.component.TickInfoDialog
import com.ticklog.core.designsystem.component.TickTopAppBar
import com.ticklog.core.designsystem.component.ResponsiveContainer
import com.ticklog.core.designsystem.theme.TickLogTheme
import com.ticklog.domain.model.DateFormat
import com.ticklog.domain.model.ThemeMode
import com.ticklog.domain.model.WeekStart
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Settings screen (stateful entry point).
 *
 * Groups appearance (theme, date format, week start, animations), data (export
 * and backup), and about (version, licences, privacy). Choice settings open a
 * compact single-choice dialog; toggles are inline switches.
 */
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToPdfExport: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onOnboardingReset: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsContent(
        uiState = uiState,
        onThemeSelected = viewModel::onThemeModeSelected,
        onDateFormatSelected = viewModel::onDateFormatSelected,
        onWeekStartSelected = viewModel::onWeekStartSelected,
        onAnimationsToggled = viewModel::onAnimationsToggled,
        onResetOnboarding = {
            viewModel.onResetOnboarding()
            onOnboardingReset()
        },
        onNavigateUp = onNavigateUp,
        onNavigateToPdfExport = onNavigateToPdfExport,
        onNavigateToBackup = onNavigateToBackup,
        onNavigateToLicenses = onNavigateToLicenses,
        modifier = modifier,
    )
}

/** Which transient dialog is open. */
private sealed interface SettingsDialog {
    data object Theme : SettingsDialog
    data object DateFormatChoice : SettingsDialog
    data object WeekStartChoice : SettingsDialog
    data object ResetConfirm : SettingsDialog
    data object Privacy : SettingsDialog
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onThemeSelected: (ThemeMode) -> Unit,
    onDateFormatSelected: (DateFormat) -> Unit,
    onWeekStartSelected: (WeekStart) -> Unit,
    onAnimationsToggled: (Boolean) -> Unit,
    onResetOnboarding: () -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateToPdfExport: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }

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
                    .padding(horizontal = TickLogTheme.spacing.large, vertical = TickLogTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.large),
            ) {
                AppearanceSection(
                    uiState = uiState,
                    onOpenTheme = { dialog = SettingsDialog.Theme },
                    onOpenDateFormat = { dialog = SettingsDialog.DateFormatChoice },
                    onOpenWeekStart = { dialog = SettingsDialog.WeekStartChoice },
                    onAnimationsToggled = onAnimationsToggled,
                )
                DataSection(
                    onNavigateToPdfExport = onNavigateToPdfExport,
                    onNavigateToBackup = onNavigateToBackup,
                )
                GeneralSection(onResetOnboarding = { dialog = SettingsDialog.ResetConfirm })
                AboutSection(
                    appVersion = uiState.appVersion,
                    onNavigateToLicenses = onNavigateToLicenses,
                    onOpenPrivacy = { dialog = SettingsDialog.Privacy },
                )
            }
        }
    }

    when (dialog) {
        SettingsDialog.Theme -> SingleChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries,
            selected = uiState.themeMode,
            label = { it.themeLabel() },
            onSelect = { onThemeSelected(it); dialog = null },
            onDismiss = { dialog = null },
        )

        SettingsDialog.DateFormatChoice -> SingleChoiceDialog(
            title = stringResource(R.string.settings_date_format),
            options = DateFormat.entries,
            selected = uiState.dateFormat,
            label = { it.sampleLabel() },
            onSelect = { onDateFormatSelected(it); dialog = null },
            onDismiss = { dialog = null },
        )

        SettingsDialog.WeekStartChoice -> SingleChoiceDialog(
            title = stringResource(R.string.settings_week_start),
            options = WeekStart.entries,
            selected = uiState.weekStart,
            label = { it.weekLabel() },
            onSelect = { onWeekStartSelected(it); dialog = null },
            onDismiss = { dialog = null },
        )

        SettingsDialog.ResetConfirm -> TickConfirmationDialog(
            title = stringResource(R.string.settings_reset_onboarding),
            message = stringResource(R.string.settings_reset_onboarding_message),
            confirmText = stringResource(R.string.action_confirm),
            onConfirm = { dialog = null; onResetOnboarding() },
            onDismiss = { dialog = null },
        )

        SettingsDialog.Privacy -> TickInfoDialog(
            title = stringResource(R.string.settings_privacy),
            message = stringResource(R.string.settings_privacy_message),
            onDismiss = { dialog = null },
        )

        null -> Unit
    }
}

@Composable
private fun AppearanceSection(
    uiState: SettingsUiState,
    onOpenTheme: () -> Unit,
    onOpenDateFormat: () -> Unit,
    onOpenWeekStart: () -> Unit,
    onAnimationsToggled: (Boolean) -> Unit,
) {
    SettingsGroup(title = stringResource(R.string.settings_appearance)) {
        NavigationRow(
            icon = { Icon(Icons.Outlined.Palette, contentDescription = null) },
            title = stringResource(R.string.settings_theme),
            value = uiState.themeMode.themeLabel(),
            onClick = onOpenTheme,
        )
        NavigationRow(
            icon = { Icon(Icons.Outlined.DateRange, contentDescription = null) },
            title = stringResource(R.string.settings_date_format),
            value = uiState.dateFormat.sampleLabel(),
            onClick = onOpenDateFormat,
        )
        NavigationRow(
            icon = { Icon(Icons.Outlined.CalendarViewWeek, contentDescription = null) },
            title = stringResource(R.string.settings_week_start),
            value = uiState.weekStart.weekLabel(),
            onClick = onOpenWeekStart,
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_animations)) },
            supportingContent = { Text(stringResource(R.string.settings_animations_description)) },
            trailingContent = {
                Switch(checked = uiState.animationsEnabled, onCheckedChange = onAnimationsToggled)
            },
            colors = surfaceListColors(),
        )
    }
}

@Composable
private fun DataSection(onNavigateToPdfExport: () -> Unit, onNavigateToBackup: () -> Unit) {
    SettingsGroup(title = stringResource(R.string.settings_data)) {
        NavigationRow(
            icon = { Icon(Icons.Outlined.PictureAsPdf, contentDescription = null) },
            title = stringResource(R.string.destination_pdf),
            value = null,
            onClick = onNavigateToPdfExport,
        )
        NavigationRow(
            icon = { Icon(Icons.Outlined.Backup, contentDescription = null) },
            title = stringResource(R.string.destination_backup),
            value = null,
            onClick = onNavigateToBackup,
        )
    }
}

@Composable
private fun GeneralSection(onResetOnboarding: () -> Unit) {
    SettingsGroup(title = stringResource(R.string.settings_general)) {
        NavigationRow(
            icon = { Icon(Icons.Outlined.RestartAlt, contentDescription = null) },
            title = stringResource(R.string.settings_reset_onboarding),
            value = null,
            onClick = onResetOnboarding,
        )
    }
}

@Composable
private fun AboutSection(
    appVersion: String,
    onNavigateToLicenses: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    SettingsGroup(title = stringResource(R.string.settings_about)) {
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_version)) },
            trailingContent = {
                Text(
                    text = appVersion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            colors = surfaceListColors(),
        )
        NavigationRow(
            icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
            title = stringResource(R.string.settings_licenses),
            value = null,
            onClick = onNavigateToLicenses,
        )
        NavigationRow(
            icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
            title = stringResource(R.string.settings_privacy),
            value = null,
            onClick = onOpenPrivacy,
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.small)) {
        SectionTitle(title = title)
        TickFilledCard { content() }
    }
}

@Composable
private fun NavigationRow(
    icon: @Composable () -> Unit,
    title: String,
    value: String?,
    onClick: () -> Unit,
) {
    ListItem(
        leadingContent = icon,
        headlineContent = { Text(title) },
        supportingContent = value?.let { { Text(it) } },
        trailingContent = {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
        },
        colors = surfaceListColors(),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick),
    )
}

@Composable
private fun surfaceListColors() =
    ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer)

@Composable
private fun <T> SingleChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                options.forEach { option ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option == selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(option) },
                            )
                            .padding(vertical = TickLogTheme.spacing.small),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Text(
                            text = label(option),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = TickLogTheme.spacing.small),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun ThemeMode.themeLabel(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    },
)

@Composable
private fun WeekStart.weekLabel(): String = stringResource(
    when (this) {
        WeekStart.SUNDAY -> R.string.settings_week_sunday
        WeekStart.MONDAY -> R.string.settings_week_monday
    },
)

/** A live sample of the format applied to a fixed date, or "System default". */
@Composable
private fun DateFormat.sampleLabel(): String {
    val sample = remember { LocalDate.of(2026, 6, 27) }
    return when (val pattern = this.pattern) {
        null -> stringResource(R.string.settings_date_format_system) +
            " (" + sample.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) + ")"
        else -> sample.format(DateTimeFormatter.ofPattern(pattern))
    }
}
