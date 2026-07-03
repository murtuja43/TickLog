package com.ticklog.domain.repository

import com.ticklog.domain.model.BackupResult
import java.io.InputStream
import java.io.OutputStream

/**
 * Offline backup and restore of the entire app state (database + preferences).
 *
 * Works over plain streams so the UI can hand it any Storage Access Framework
 * destination or source. Restore validates the backup (format version +
 * checksum) before touching existing data and never overwrites silently — the
 * caller is responsible for confirming with the user first.
 */
interface BackupRepository {

    /**
     * Serialises the full app state as a self-describing backup to [output]. The
     * caller owns [output] and is responsible for closing it.
     */
    suspend fun exportTo(output: OutputStream)

    /**
     * Validates and restores a backup read from [input], replacing all current
     * data. Returns a [BackupResult] describing success or the precise failure;
     * on any failure the existing data is left intact. The caller owns [input].
     */
    suspend fun importFrom(input: InputStream): BackupResult
}
