package com.ticklog.ui.feature_pdf

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticklog.core.pdf.PdfExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** One-shot effects the PDF screen reacts to. */
sealed interface PdfExportEvent {
    /** A report is ready to be shared from [file]. */
    data class ShareReady(val file: File) : PdfExportEvent

    /** A report is ready to be opened from [file]. */
    data class OpenReady(val file: File) : PdfExportEvent

    /** The report was written to the chosen destination. */
    data object Saved : PdfExportEvent

    /** Generation or writing failed. */
    data object Failed : PdfExportEvent
}

/**
 * Drives the PDF export screen: holds the scope selection and runs the three
 * export actions (save to a Storage Access Framework destination, share, open)
 * through [PdfExporter], surfacing results as one-shot events.
 */
@HiltViewModel
class PdfExportViewModel @Inject constructor(
    private val pdfExporter: PdfExporter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PdfExportUiState())
    val uiState: StateFlow<PdfExportUiState> = _uiState.asStateFlow()

    private val eventsChannel = Channel<PdfExportEvent>(Channel.BUFFERED)
    val events: Flow<PdfExportEvent> = eventsChannel.receiveAsFlow()

    fun onScopeChoiceSelected(choice: ReportScopeChoice) =
        _uiState.update { it.copy(scopeChoice = choice) }

    fun onSingleDaySelected(date: LocalDate) = _uiState.update { it.copy(singleDay = date) }
    fun onRangeStartSelected(date: LocalDate) = _uiState.update { it.copy(rangeStart = date) }
    fun onRangeEndSelected(date: LocalDate) = _uiState.update { it.copy(rangeEnd = date) }
    fun onMonthSelected(month: YearMonth) = _uiState.update { it.copy(month = month) }
    fun onYearChanged(delta: Int) = _uiState.update { it.copy(year = it.year + delta) }

    /** A sensible default file name for the Save dialog. */
    fun suggestedFileName(): String = "TickLog_report_${LocalDate.now()}.pdf"

    /** Writes the report to the SAF [destination]. */
    fun onSave(destination: Uri) = runExport {
        pdfExporter.renderToUri(currentScope(), destination)
        eventsChannel.send(PdfExportEvent.Saved)
    }

    /** Generates a cache copy and asks the screen to share it. */
    fun onShare() = runExport {
        val file = pdfExporter.renderToCache(currentScope())
        eventsChannel.send(PdfExportEvent.ShareReady(file))
    }

    /** Generates a cache copy and asks the screen to open it. */
    fun onOpen() = runExport {
        val file = pdfExporter.renderToCache(currentScope())
        eventsChannel.send(PdfExportEvent.OpenReady(file))
    }

    private fun currentScope() = _uiState.value.toScope()

    private inline fun runExport(crossinline block: suspend () -> Unit) {
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            try {
                block()
            } catch (_: Exception) {
                eventsChannel.send(PdfExportEvent.Failed)
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }
}
