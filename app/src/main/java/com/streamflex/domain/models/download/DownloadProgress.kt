package com.streamflex.domain.models.download

/**
 * Lightweight real-time progress update event for active downloads.
 */
data class DownloadProgress(
    val id: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val progress: Float, // 0.0f to 1.0f
    val speedBytesPerSec: Long,
    val etaSeconds: Long,
    val status: DownloadStatus
) {
    val progressPercent: Int
        get() = (progress * 100).toInt().coerceIn(0, 100)

    val speedFormatted: String
        get() {
            if (speedBytesPerSec <= 0) return "0 MB/s"
            val mbPerSec = speedBytesPerSec.toDouble() / (1024 * 1024)
            return if (mbPerSec >= 1.0) {
                String.format("%.1f MB/s", mbPerSec)
            } else {
                val kbPerSec = speedBytesPerSec.toDouble() / 1024
                String.format("%.0f KB/s", kbPerSec)
            }
        }

    val etaFormatted: String
        get() {
            if (etaSeconds <= 0) return ""
            val hours = etaSeconds / 3600
            val minutes = (etaSeconds % 3600) / 60
            val seconds = etaSeconds % 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m left"
                minutes > 0 -> "${minutes}m ${seconds}s left"
                else -> "${seconds}s left"
            }
        }
}
