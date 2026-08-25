package com.streamflex.domain.models.download

import com.streamflex.domain.models.Quality
import com.streamflex.domain.models.StreamLink
import com.streamflex.domain.models.Subtitle

/**
 * Represents a persistent download item (Movie or TV Episode) in StreamFlex.
 */
data class DownloadItem(
    /** Unique task ID (e.g. SHA-256 or UUID) */
    val id: String,

    /** Media Identifier (TMDB ID or provider media ID) */
    val mediaId: String,

    /** Media title (e.g. "Deadpool & Wolverine" or "Stranger Things") */
    val title: String,

    /** Subtitle / Episode label (e.g. "S04 E01 - Chapter One") */
    val subtitle: String? = null,

    /** Release year */
    val year: Int? = null,

    /** Whether this is a TV Show episode */
    val isShow: Boolean = false,

    /** Season number if TV */
    val seasonNumber: Int? = null,

    /** Episode number if TV */
    val episodeNumber: Int? = null,

    /** Poster image URL */
    val posterUrl: String? = null,

    /** Video quality of the stream */
    val quality: Quality = Quality.UNKNOWN,

    /** Primary streaming link to download */
    val streamLink: StreamLink,

    /** Alternative fallback mirrors if primary fails */
    val fallbackLinks: List<StreamLink> = emptyList(),

    /** External subtitles to download and package alongside video */
    val subtitles: List<Subtitle> = emptyList(),

    /** Current download lifecycle status */
    val status: DownloadStatus = DownloadStatus.QUEUED,

    /** Total downloaded bytes so far */
    val downloadedBytes: Long = 0L,

    /** Total file size in bytes (-1 if unknown) */
    val totalBytes: Long = 0L,

    /** Current download rate (bytes/second) */
    val speedBytesPerSec: Long = 0L,

    /** Estimated time remaining (seconds) */
    val etaSeconds: Long = 0L,

    /** Error message if failed */
    val errorMessage: String? = null,

    /** Local file path on disk once completed */
    val localFilePath: String? = null,

    /** Creation timestamp */
    val createdAt: Long = System.currentTimeMillis(),

    /** Completion timestamp */
    val completedAt: Long? = null
) {
    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()

    val formattedSize: String
        get() {
            val bytes = if (totalBytes > 0) totalBytes else downloadedBytes
            if (bytes <= 0) return "Unknown size"
            val mb = bytes.toDouble() / (1024 * 1024)
            return if (mb >= 1024.0) {
                String.format("%.1f GB", mb / 1024.0)
            } else {
                String.format("%.0f MB", mb)
            }
        }

    val formattedDownloadedSize: String
        get() {
            if (downloadedBytes <= 0) return "0 MB"
            val mb = downloadedBytes.toDouble() / (1024 * 1024)
            return if (mb >= 1024.0) {
                String.format("%.2f GB", mb / 1024.0)
            } else {
                String.format("%.1f MB", mb)
            }
        }
}
