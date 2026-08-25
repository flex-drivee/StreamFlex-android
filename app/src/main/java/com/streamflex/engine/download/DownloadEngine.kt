package com.streamflex.engine.download

import com.streamflex.data.local.download.DownloadStorageManager
import com.streamflex.domain.models.StreamLink
import com.streamflex.domain.models.download.DownloadItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Unified Download Engine that orchestrates:
 * - Content-Type routing (Multi-chunk MP4/MKV Range vs Native HLS M3U8).
 * - Automatic mirror fallback across server links (FSL, Buzz, Pixeldrain, S3, Mega, PDL, 10Gbps).
 * - Subtitle fetching and local storage syncing.
 */
class DownloadEngine(
    private val storageManager: DownloadStorageManager,
    private val multiChunkDownloader: MultiChunkDownloader = MultiChunkDownloader(),
    private val hlsDownloader: HlsDownloader = HlsDownloader(),
    private val subtitleDownloader: SubtitleDownloader = SubtitleDownloader()
) {

    sealed class Result {
        data class Success(val file: File, val totalBytes: Long) : Result()
        object Paused : Result()
        object Cancelled : Result()
        data class Error(val message: String, val throwable: Throwable? = null) : Result()
    }

    /**
     * Executes the download task for [item], with automatic fallback to alternate mirrors.
     */
    suspend fun executeDownload(
        item: DownloadItem,
        onProgress: suspend (downloaded: Long, total: Long, speed: Long, eta: Long) -> Unit,
        isPaused: () -> Boolean = { false },
        isCancelled: () -> Boolean = { false }
    ): Result = withContext(Dispatchers.IO) {
        val targetFile = if (!item.localFilePath.isNullOrBlank()) {
            File(item.localFilePath)
        } else {
            storageManager.resolveTargetFile(item)
        }

        // Ordered list of candidate links (Primary first, then fallbacks)
        val candidateLinks = buildList {
            add(item.streamLink)
            addAll(item.fallbackLinks.filter { it.url != item.streamLink.url })
        }

        var lastError: Throwable? = null

        for ((index, streamLink) in candidateLinks.withIndex()) {
            if (isCancelled()) return@withContext Result.Cancelled
            if (isPaused()) return@withContext Result.Paused

            val isHls = streamLink.adaptive || streamLink.url.contains(".m3u8", ignoreCase = true)

            val downloadResult = if (isHls) {
                hlsDownloader.download(
                    streamLink = streamLink,
                    targetFile = targetFile,
                    onProgress = onProgress,
                    isPaused = isPaused,
                    isCancelled = isCancelled
                )
            } else {
                multiChunkDownloader.download(
                    streamLink = streamLink,
                    targetFile = targetFile,
                    onProgress = onProgress,
                    isPaused = isPaused,
                    isCancelled = isCancelled
                )
            }

            when (downloadResult) {
                is MultiChunkDownloader.Result.Success -> {
                    // Download subtitles if any
                    subtitleDownloader.downloadSubtitles(item.subtitles, downloadResult.file, storageManager)
                    return@withContext Result.Success(downloadResult.file, downloadResult.totalBytes)
                }
                is HlsDownloader.Result.Success -> {
                    // Download subtitles if any
                    subtitleDownloader.downloadSubtitles(item.subtitles, downloadResult.file, storageManager)
                    return@withContext Result.Success(downloadResult.file, downloadResult.totalBytes)
                }
                is MultiChunkDownloader.Result.Paused,
                is HlsDownloader.Result.Paused -> {
                    return@withContext Result.Paused
                }
                is MultiChunkDownloader.Result.Cancelled,
                is HlsDownloader.Result.Cancelled -> {
                    return@withContext Result.Cancelled
                }
                is MultiChunkDownloader.Result.Error -> {
                    lastError = downloadResult.throwable
                    // Continue to next mirror in loop
                }
                is HlsDownloader.Result.Error -> {
                    lastError = downloadResult.throwable
                    // Continue to next mirror in loop
                }
            }
        }

        Result.Error(
            message = "All ${candidateLinks.size} server mirror(s) failed for '${item.title}'",
            throwable = lastError
        )
    }
}
