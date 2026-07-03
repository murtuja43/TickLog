package com.ticklog.ui.feature_pdf

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ticklog.R
import com.ticklog.core.designsystem.component.PrimaryButton
import com.ticklog.core.designsystem.component.ResponsiveContainer
import com.ticklog.core.designsystem.component.SecondaryButton
import com.ticklog.core.designsystem.component.SectionTitle
import com.ticklog.core.designsystem.component.TickCard
import com.ticklog.core.designsystem.component.TickTopAppBar
import com.ticklog.core.designsystem.component.TickDatePickerDialog
import com.ticklog.core.designsystem.theme.TickLogTheme
import com.ticklog.util.DateTimeFormatters
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

/**
 * PDF export destination.
 *
 * The user chooses a scope (single day, range, month, year or all history) with
 * the relevant date controls, then saves (via the Storage Access Framework),
 * shares, or opens the generated report.
 *
 * @param onNavigateUp invoked when the user taps the back arrow.
 * @param modifier external layout modifier.
 */
@Composable
fun PdfExportScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PdfExportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val savedMessage = stringResource(R.string.pdf_saved)
    val failedMessage = stringResource(R.string.pdf_failed)
    val noViewerMessage = stringResource(R.string.pdf_no_viewer)

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri -> uri?.let(viewModel::onSave) }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.events.collect { event ->
            when (event) {
                is PdfExportEvent.ShareReady -> context.sharePdf(event.file)
                is PdfExportEvent.OpenReady -> context.openPdf(event.file) {
                    snackbarHostState.showSnackbar(noViewerMessage)
                }
                PdfExportEvent.Saved -> snackbarHostState.showSnackbar(savedMessage)
                PdfExportEvent.Failed -> snackbarHostState.showSnackbar(failedMessage)
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TickTopAppBar(
                title = stringResource(R.string.destination_pdf),
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
                    .verticalScroll(rememberScrollState())
                    .padding(TickLogTheme.spacing.large),
                verticalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.large),
            ) {
                ScopeChooser(
                    selected = uiState.scopeChoice,
                    onSelected = viewModel::onScopeChoiceSelected,
                )

                ScopeDetails(uiState = uiState, viewModel = viewModel)

                ExportActions(
                    enabled = uiState.canExport,
                    busy = uiState.isBusy,
                    onSave = { saveLauncher.launch(viewModel.suggestedFileName()) },
                    onShare = viewModel::onShare,
                    onOpen = viewModel::onOpen,
                )
            }
        }
    }
}

/** Radio group of report scopes. */
@Composable
private fun ScopeChooser(
    selected: ReportScopeChoice,
    onSelected: (ReportScopeChoice) -> Unit,
) {
    Column {
        SectionTitle(title = stringResource(R.string.pdf_scope_title))
        TickCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.selectableGroup()) {
                ReportScopeChoice.entries.forEach { choice ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = choice == selected,
                                role = Role.RadioButton,
                                onClick = { onSelected(choice) },
                            )
                            .padding(vertical = TickLogTheme.spacing.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = choice == selected, onClick = null)
                        Text(
                            text = choice.label(),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = TickLogTheme.spacing.small),
                        )
                    }
                }
            }
        }
    }
}

/** The date controls that a scope needs. */
@Composable
private fun ScopeDetails(uiState: PdfExportUiState, viewModel: PdfExportViewModel) {
    when (uiState.scopeChoice) {
        ReportScopeChoice.SINGLE_DAY -> DateField(
            label = stringResource(R.string.pdf_date),
            value = DateTimeFormatters.medium(uiState.singleDay),
            initialDate = uiState.singleDay,
            onDateSelected = viewModel::onSingleDaySelected,
        )

        ReportScopeChoice.DATE_RANGE -> Column(
            verticalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.medium),
        ) {
            DateField(
                label = stringResource(R.string.onboarding_start_date),
                value = DateTimeFormatters.medium(uiState.rangeStart),
                initialDate = uiState.rangeStart,
                onDateSelected = viewModel::onRangeStartSelected,
            )
            DateField(
                label = stringResource(R.string.onboarding_end_date),
                value = DateTimeFormatters.medium(uiState.rangeEnd),
                initialDate = uiState.rangeEnd,
                onDateSelected = viewModel::onRangeEndSelected,
            )
            if (!uiState.isRangeValid) {
                Text(
                    text = stringResource(R.string.onboarding_error_range),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        ReportScopeChoice.MONTH -> DateField(
            label = stringResource(R.string.pdf_month),
            value = DateTimeFormatters.monthYear(uiState.month),
            initialDate = uiState.month.atDay(1),
            onDateSelected = { viewModel.onMonthSelected(YearMonth.from(it)) },
        )

        ReportScopeChoice.YEAR -> Stepper(
            label = stringResource(R.string.pdf_year),
            value = uiState.year.toString(),
            onDecrement = { viewModel.onYearChanged(-1) },
            onIncrement = { viewModel.onYearChanged(1) },
        )

        ReportScopeChoice.ENTIRE_HISTORY -> Unit
    }
}

/** A tappable field that shows a value and opens a date picker. */
@Composable
private fun DateField(
    label: String,
    value: String,
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    TickCard(modifier = Modifier.fillMaxWidth(), onClick = { showPicker = true }) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
    if (showPicker) {
        TickDatePickerDialog(
            initialDate = initialDate,
            onDismiss = { showPicker = false },
            onDateSelected = {
                onDateSelected(it)
                showPicker = false
            },
        )
    }
}

/** A minus/value/plus stepper (used for the year). */
@Composable
private fun Stepper(label: String, value: String, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    TickCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrement) {
                Icon(Icons.Outlined.KeyboardArrowLeft, stringResource(R.string.pdf_decrement))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = TickLogTheme.spacing.large),
            )
            IconButton(onClick = onIncrement) {
                Icon(Icons.Outlined.KeyboardArrowRight, stringResource(R.string.pdf_increment))
            }
        }
    }
}

/** Save / share / open action buttons. */
@Composable
private fun ExportActions(
    enabled: Boolean,
    busy: Boolean,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(TickLogTheme.spacing.medium)) {
        PrimaryButton(
            text = stringResource(R.string.pdf_save),
            onClick = onSave,
            enabled = enabled,
            leadingIcon = Icons.Outlined.Save,
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(
            text = stringResource(R.string.pdf_share),
            onClick = onShare,
            enabled = enabled,
            leadingIcon = Icons.Outlined.Share,
            modifier = Modifier.fillMaxWidth(),
        )
        SecondaryButton(
            text = stringResource(R.string.pdf_open),
            onClick = onOpen,
            enabled = enabled,
            leadingIcon = Icons.Outlined.OpenInNew,
            modifier = Modifier.fillMaxWidth(),
        )
        if (busy) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
private fun ReportScopeChoice.label(): String = stringResource(
    when (this) {
        ReportScopeChoice.SINGLE_DAY -> R.string.pdf_scope_single_day
        ReportScopeChoice.DATE_RANGE -> R.string.pdf_scope_range
        ReportScopeChoice.MONTH -> R.string.pdf_scope_month
        ReportScopeChoice.YEAR -> R.string.pdf_scope_year
        ReportScopeChoice.ENTIRE_HISTORY -> R.string.pdf_scope_all
    },
)

/** Builds a scoped content URI and launches the system share sheet. */
private fun Context.sharePdf(file: File) {
    val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(intent, null))
}

/** Opens the PDF in an external viewer, invoking [onNoViewer] if none exists. */
private suspend fun Context.openPdf(file: File, onNoViewer: suspend () -> Unit) {
    val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        onNoViewer()
    }
}
