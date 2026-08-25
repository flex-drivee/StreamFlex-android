package com.streamflex.domain.models.download

/**
 * Lifecycle states of a download task.
 */
enum class DownloadStatus {
    /** Waiting in queue */
    QUEUED,

    /** Connecting to server, resolving headers & range support */
    CONNECTING,

    /** Actively downloading chunks */
    DOWNLOADING,

    /** Paused by user or network loss */
    PAUSED,

    /** Download finished and verified on disk */
    COMPLETED,

    /** Encountered unrecoverable error */
    FAILED,

    /** Cancelled and cleaned up */
    CANCELLED;

    val isActive: Boolean
        get() = this == QUEUED || this == CONNECTING || this == DOWNLOADING

    val isPaused: Boolean
        get() = this == PAUSED

    val isCompleted: Boolean
        get() = this == COMPLETED

    val isFailed: Boolean
        get() = this == FAILED
}
