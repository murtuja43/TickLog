package com.ticklog.domain.model

/**
 * The outcome of a restore attempt.
 *
 * Restore is intentionally fail-safe: rather than throwing, it returns a typed
 * result so the UI can explain exactly why an import was rejected and leave the
 * existing data untouched.
 */
sealed interface BackupResult {

    /** The backup was valid and has been restored. */
    data object Success : BackupResult

    /** The backup could not be restored; [error] says why. */
    data class Failure(val error: BackupError) : BackupResult
}

/** Why a restore failed. */
enum class BackupError {
    /** The file is not a TickLog backup or is corrupt/unreadable. */
    INVALID_FILE,

    /** The backup was written by a newer, unsupported app version. */
    UNSUPPORTED_VERSION,

    /** The integrity checksum did not match — the file was altered or truncated. */
    CHECKSUM_MISMATCH,

    /** An unexpected error occurred while writing the data. */
    RESTORE_FAILED,
}
