package com.streamflex.domain.repositories

import com.streamflex.domain.models.download.DownloadItem
import com.streamflex.domain.models.download.DownloadProgress
import com.streamflex.domain.models.download.DownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for managing downloaded and in-progress media.
 */
interface DownloadRepository {

    /** Reactive stream of all download tasks (active, completed, paused) */
    val allDownloads: StateFlow<List<DownloadItem>>

    /** Reactive stream of active downloads currently in queue or downloading */
    val activeDownloads: StateFlow<List<DownloadItem>>

    /** Reactive stream of completed downloads ready for offline playback */
    val completedDownloads: StateFlow<List<DownloadItem>>

    /** Get a specific download item by its task ID */
    suspend fun getDownloadById(id: String): DownloadItem?

    /** Find a downloaded item for a given media ID and optional episode ID */
    suspend fun getDownloadForMedia(mediaId: String, seasonNumber: Int? = null, episodeNumber: Int? = null): DownloadItem?

    /** Check if a media item or episode is fully downloaded on disk */
    suspend fun isDownloaded(mediaId: String, seasonNumber: Int? = null, episodeNumber: Int? = null): Boolean

    /** Save or update a download item */
    suspend fun saveDownload(item: DownloadItem)

    /** Update real-time progress for a download */
    suspend fun updateProgress(
        id: String,
        downloadedBytes: Long,
        totalBytes: Long,
        speedBytesPerSec: Long,
        etaSeconds: Long
    )

    /** Update the status of a download */
    suspend fun updateStatus(
        id: String,
        status: DownloadStatus,
        error: String? = null,
        localFilePath: String? = null
    )

    /** Remove a download record and its local files */
    suspend fun deleteDownload(id: String)
}
