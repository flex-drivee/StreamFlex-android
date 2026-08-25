package com.streamflex.data.local.download

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.streamflex.domain.models.download.DownloadItem
import java.io.File

/**
 * Manages storage locations, folder structures, and disk space calculation for StreamFlex downloads.
 */
class DownloadStorageManager(private val context: Context) {

    companion object {
        private const val ROOT_DIR_NAME = "StreamFlex"
        private const val MOVIES_DIR_NAME = "Movies"
        private const val TV_DIR_NAME = "TV Shows"
    }

    /**
     * Resolves the root directory for storing downloaded videos.
     * Prefers app-specific external storage to ensure compatibility across all Android versions (Android 5 to 15+).
     */
    fun getDownloadsRootDir(): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: context.filesDir
        val root = File(baseDir, ROOT_DIR_NAME)
        if (!root.exists()) {
            root.mkdirs()
        }
        return root
    }

    /**
     * Resolves the target file destination for a [DownloadItem].
     * Example outputs:
     * - Movies: `.../StreamFlex/Movies/Deadpool & Wolverine (2024)/Deadpool & Wolverine.1080p.mp4`
     * - Shows:  `.../StreamFlex/TV Shows/Stranger Things/Season 4/Stranger Things - S04E01 - Chapter One.1080p.mp4`
     */
    fun resolveTargetFile(item: DownloadItem, extension: String = "mp4"): File {
        val root = getDownloadsRootDir()
        val safeTitle = sanitizeFilename(item.title)
        val qualityTag = if (item.quality.name.isNotBlank() && item.quality.name != "UNKNOWN") {
            ".${item.quality.name.lowercase().removePrefix("q_")}"
        } else ""

        val safeExt = if (extension.startsWith(".")) extension else ".$extension"

        return if (item.isShow && item.seasonNumber != null) {
            val tvDir = File(root, TV_DIR_NAME)
            val showDir = File(tvDir, safeTitle)
            val seasonDir = File(showDir, "Season ${item.seasonNumber}")
            if (!seasonDir.exists()) seasonDir.mkdirs()

            val sNum = item.seasonNumber.toString().padStart(2, '0')
            val eNum = (item.episodeNumber ?: 1).toString().padStart(2, '0')
            val epSubtitle = if (!item.subtitle.isNullOrBlank()) {
                val cleanSub = sanitizeFilename(item.subtitle)
                " - $cleanSub"
            } else ""

            val filename = "$safeTitle - S${sNum}E${eNum}$epSubtitle$qualityTag$safeExt"
            File(seasonDir, filename)
        } else {
            val movieDir = File(root, MOVIES_DIR_NAME)
            val titleWithYear = if (item.year != null && item.year > 0) "$safeTitle (${item.year})" else safeTitle
            val specificMovieDir = File(movieDir, titleWithYear)
            if (!specificMovieDir.exists()) specificMovieDir.mkdirs()

            val filename = "$titleWithYear$qualityTag$safeExt"
            File(specificMovieDir, filename)
        }
    }

    /**
     * Resolves the subtitle file destination for a video file.
     */
    fun resolveSubtitleFile(videoFile: File, languageCode: String = "en", ext: String = "srt"): File {
        val baseName = videoFile.nameWithoutExtension
        val safeExt = if (ext.startsWith(".")) ext else ".$ext"
        return File(videoFile.parentFile, "$baseName.$languageCode$safeExt")
    }

    /**
     * Sanitizes strings to make them safe for file and directory names.
     */
    fun sanitizeFilename(name: String): String {
        return name.replace(Regex("""[\\/:*?"<>|]"""), "_")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .take(120)
    }

    /**
     * Calculates storage statistics in bytes.
     */
    fun getStorageStats(): StorageStats {
        return try {
            val root = getDownloadsRootDir()
            val stat = StatFs(root.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize

            val appUsedBytes = calculateDirSize(root)

            StorageStats(
                totalBytes = totalBytes,
                freeBytes = freeBytes,
                usedBytes = totalBytes - freeBytes,
                streamFlexUsedBytes = appUsedBytes
            )
        } catch (_: Exception) {
            StorageStats(0, 0, 0, 0)
        }
    }

    private fun calculateDirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) calculateDirSize(file) else file.length()
        }
        return size
    }

    data class StorageStats(
        val totalBytes: Long,
        val freeBytes: Long,
        val usedBytes: Long,
        val streamFlexUsedBytes: Long
    ) {
        val formattedFree: String
            get() = formatBytes(freeBytes)

        val formattedTotal: String
            get() = formatBytes(totalBytes)

        val formattedStreamFlexUsed: String
            get() = formatBytes(streamFlexUsedBytes)

        private fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 GB"
            val gb = bytes.toDouble() / (1024 * 1024 * 1024)
            return String.format("%.1f GB", gb)
        }
    }
}
