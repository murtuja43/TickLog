package com.ticklog.core.pdf

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.ticklog.di.qualifier.IoDispatcher
import com.ticklog.domain.model.ReportScope
import com.ticklog.domain.repository.PreferencesRepository
import com.ticklog.domain.usecase.BuildReportDataUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns a [ReportScope] into a PDF, either written to a Storage Access Framework
 * destination (Save) or to a private cache file that can be shared/opened via a
 * scoped [FileProvider] URI. All heavy work runs off the main thread.
 */
@Singleton
class PdfExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val buildReportData: BuildReportDataUseCase,
    private val renderer: PdfReportRenderer,
    private val preferencesRepository: PreferencesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    /** Renders the report for [scope] into a fresh cache file and returns it. */
    suspend fun renderToCache(scope: ReportScope): File = withContext(ioDispatcher) {
        val data = buildReportData(scope)
        val prefs = preferencesRepository.preferences.first()
        val dir = File(context.cacheDir, EXPORTS_DIR).apply { mkdirs() }
        val file = File(dir, "TickLog_${System.currentTimeMillis()}.pdf")
        file.outputStream().use {
            renderer.render(data, prefs.dateFormat, prefs.includeNotesInExport, it)
        }
        file
    }

    /** Renders the report for [scope] into the SAF [destination]. */
    suspend fun renderToUri(scope: ReportScope, destination: Uri) = withContext(ioDispatcher) {
        val data = buildReportData(scope)
        val prefs = preferencesRepository.preferences.first()
        val stream = context.contentResolver.openOutputStream(destination)
            ?: error("Unable to open destination for writing")
        stream.use { renderer.render(data, prefs.dateFormat, prefs.includeNotesInExport, it) }
    }

    /** A scoped, shareable content URI for a cache [file]. */
    fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private companion object {
        const val EXPORTS_DIR = "exports"
    }
}
