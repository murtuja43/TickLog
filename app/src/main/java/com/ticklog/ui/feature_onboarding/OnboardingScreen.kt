package com.ticklog.ui.feature_onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ticklog.R
import com.ticklog.core.designsystem.component.PrimaryButton
import com.ticklog.core.designsystem.component.ResponsiveContainer
import com.ticklog.core.designsystem.component.TickCard
import com.ticklog.core.designsystem.theme.TickLogTheme
import com.ticklog.domain.model.DateRange
import com.ticklog.util.DateTimeFormatters
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.LaunchedEffect
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Onboarding step 1 (stateful entry point): choosing the date range.
 *
 * Collects state and the one-shot "proceed" event from [OnboardingViewModel],
 * advancing to the checklist builder via [onContinue] exactly once with the
 * validated range. All rendering is delegated to the stateless [OnboardingContent].
 *
 * @param windowSizeClass current window size, used to widen the layout on tablets.
 * @param onContinue invoked once with the chosen range to open the builder step.
 * @param modifier external layout modifier.
 */
@Composable
fun OnboardingScreen(
    windowSizeClass: WindowSizeClass,
    onContinue: (DateRange) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Advance exactly once when the ViewModel emits the validated range.
    LaunchedEffect(viewModel) {
        viewModel.proceed.collect { range -> onContinue(range) }
    }

    OnboardingContent(
        uiState = uiState,
        isExpandedWidth = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact,
        onStartDateSelected = viewModel::onStartDateSelected,
        onEndDateSelected = viewModel::onEndDateSelected,
        onContinueClicked = viewModel::onContinueClicked,
        modifier = modifier,
    )
}

/** Identifies which date field's picker is currently open. */
private enum class PickerTarget { START, END }

/**
 * Stateless onboarding UI.
 *
 * @param uiState the snapshot to render.
 * @param isExpandedWidth whether to use the roomier large-screen layout.
 * @param onStartDateSelected raised when a start date is picked.
 * @param onEndDateSelected raised when an end date is picked.
 * @param onContinueClicked raised when the user confirms the range.
 * @param modifier external layout modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingContent(
    uiState: OnboardingUiState,
    isExpandedWidth: Boolean,
    onStartDateSelected: (LocalDate) -> Unit,
    onEndDateSelected: (LocalDate) -> Unit,
    onContinueClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Ephemeral, UI-only state: which picker (if any) is open. Kept local because
    // it is pure presentation concern with no bearing on business state.
    var activePicker by remember { mutableStateOf<PickerTarget?>(null) }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        ResponsiveContainer(
            maxContentWidth = if (isExpandedWidth) 520.dp else 600.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        horizontal = TickLogTheme.spacing.large,
                        vertical = TickLogTheme.spacing.huge,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BrandHeader()

                Spacer(Modifier.size(TickLogTheme.spacing.huge))

                DateSelectorField(
                    label = stringResource(R.string.onboarding_start_date),
                    date = uiState.startDate,
                    onClick = { activePicker = PickerTarget.START },
                )

                Spacer(Modifier.size(TickLogTheme.spacing.medium))

                DateSelectorField(
                    label = stringResource(R.string.onboarding_end_date),
                    date = uiState.endDate,
                    onClick = { activePicker = PickerTarget.END },
                )

                if (uiState.hasRangeError) {
                    Spacer(Modifier.size(TickLogTheme.spacing.small))
                    Text(
                        text = stringResource(R.string.onboarding_error_range),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(Modifier.size(TickLogTheme.spacing.huge))

                PrimaryButton(
                    text = stringResource(R.string.action_continue),
                    onClick = onContinueClicked,
                    enabled = uiState.canContinue,
                    leadingIcon = Icons.Outlined.Check,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    // The date-picker dialog, shown for whichever field is active.
    activePicker?.let { target ->
        val initialDate = when (target) {
            PickerTarget.START -> uiState.startDate
            PickerTarget.END -> uiState.endDate
        }
        OnboardingDatePickerDialog(
            initialDate = initialDate,
            onDismiss = { activePicker = null },
            onDateSelected = { selected ->
                when (target) {
                    PickerTarget.START -> onStartDateSelected(selected)
                    PickerTarget.END -> onEndDateSelected(selected)
                }
                activePicker = null
            },
        )
    }
}

/** The branded header: a monochrome check badge above the welcome copy. */
@Composable
private fun BrandHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(88.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = stringResource(R.string.cd_app_logo),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(44.dp),
                )
            }
        }

        Spacer(Modifier.size(TickLogTheme.spacing.large))

        Text(
            text = stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(TickLogTheme.spacing.small))
        Text(
            text = stringResource(R.string.onboarding_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** A tappable card showing a date field's label and current value. */
@Composable
private fun DateSelectorField(
    label: String,
    date: LocalDate?,
    onClick: () -> Unit,
) {
    TickCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(TickLogTheme.spacing.extraSmall))
                Text(
                    text = date?.let(DateTimeFormatters::medium)
                        ?: stringResource(R.string.onboarding_select_date),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Wraps the Material 3 date picker, converting to/from [LocalDate] in UTC. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnboardingDatePickerDialog(
    initialDate: LocalDate?,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.toUtcMillis(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        colors = DatePickerDefaults.colors(),
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(millis.toLocalDateUtc())
                    }
                },
                enabled = datePickerState.selectedDateMillis != null,
            ) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

/** The date picker works in UTC millis; these keep the conversion in one place. */
private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDateUtc(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
