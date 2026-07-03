package com.ticklog.ui.feature_backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ticklog.di.qualifier.IoDispatcher
import com.ticklog.domain.model.BackupError
import com.ticklog.domain.model.BackupResult
import com.ticklog.domain.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** One-shot effects for the backup screen. */
sealed interface BackupEvent {
    data object ExportSucceeded : BackupEvent
    data object ExportFailed : BackupEvent
    data object RestoreSucceeded : BackupEvent
    data class RestoreFailed(val error: BackupError) : BackupEvent
}

/**
 * Drives Backup & Restore.
 *
 * Export/import work over Storage Access Framework URIs; the ViewModel opens the
 * streams and delegates the actual (de)serialisation and validation to
 * [BackupRepository]. It never overwrites data on its own — restore only runs
 * after the screen has confirmed with the user.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val eventsChannel = Channel<BackupEvent>(Channel.BUFFERED)
    val events: Flow<BackupEvent> = eventsChannel.receiveAsFlow()

    /** A sensible default file name for the export dialog. */
    fun suggestedFileName(): String = "TickLog_backup_${java.time.LocalDate.now()}.json"

    /** Writes a full backup to the SAF [destination]. */
    fun onExport(destination: Uri) {
        _isBusy.value = true
        viewModelScope.launch {
            val ok = runCatching {
                withContext(ioDispatcher) {
                    val output = context.contentResolver.openOutputStream(destination)
                        ?: error("Cannot open destination")
                    output.use { backupRepository.exportTo(it) }
                }
            }.isSuccess
            eventsChannel.send(if (ok) BackupEvent.ExportSucceeded else BackupEvent.ExportFailed)
            _isBusy.value = false
        }
    }

    /** Validates and restores a backup from [source], replacing all data. */
    fun onRestore(source: Uri) {
        _isBusy.value = true
        viewModelScope.launch {
            val result = runCatching {
                withContext(ioDispatcher) {
                    val input = context.contentResolver.openInputStream(source)
                        ?: error("Cannot open source")
                    input.use { backupRepository.importFrom(it) }
                }
            }.getOrElse { BackupResult.Failure(BackupError.INVALID_FILE) }

            eventsChannel.send(
                when (result) {
                    BackupResult.Success -> BackupEvent.RestoreSucceeded
                    is BackupResult.Failure -> BackupEvent.RestoreFailed(result.error)
                },
            )
            _isBusy.value = false
        }
    }
}
