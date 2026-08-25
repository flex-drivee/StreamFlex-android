package com.streamflex.engine.download

import android.content.Context
import com.streamflex.domain.models.download.DownloadItem
import com.streamflex.domain.models.download.DownloadStatus
import com.streamflex.domain.repositories.DownloadRepository
import com.streamflex.engine.download.service.StreamFlexDownloadService
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Queue and Lifecycle Manager for StreamFlex downloads.
 *
 * Responsibilities:
 * - Concurrency control (limits parallel downloads to prevent bandwidth contention).
 * - Lifecycle commands: Enqueue, Pause, Resume, Cancel, Retry.
 * - Auto-reconnection & auto-restart of interrupted downloads.
 * - Integration with StreamFlexDownloadService for foreground notifications.
 */
class DownloadQueueManager(
    private val context: Context,
    private val repository: DownloadRepository,
    private val downloadEngine: DownloadEngine,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {

    companion object {
        private const val MAX_CONCURRENT_DOWNLOADS = 2
    }

    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val pausedIds = ConcurrentHashMap.newKeySet<String>()
    private val cancelledIds = ConcurrentHashMap.newKeySet<String>()

    init {
        // Auto-recover any downloads interrupted by app close or crash
        scope.launch {
            delay(1000L) // Grace period for app startup
            recoverInterruptedDownloads()
        }
    }

    /**
     * Enqueues a new item for download.
     */
    fun enqueueDownload(item: DownloadItem) {
        scope.launch {
            pausedIds.remove(item.id)
            cancelledIds.remove(item.id)
            repository.saveDownload(item.copy(status = DownloadStatus.QUEUED))
            StreamFlexDownloadService.startService(context)
            processQueue()
        }
    }

    /**
     * Pauses an active or queued download.
     */
    fun pauseDownload(id: String) {
        pausedIds.add(id)
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        scope.launch {
            repository.updateStatus(id, DownloadStatus.PAUSED)
            processQueue()
        }
    }

    /**
     * Resumes a paused download.
     */
    fun resumeDownload(id: String) {
        pausedIds.remove(id)
        cancelledIds.remove(id)
        scope.launch {
            repository.updateStatus(id, DownloadStatus.QUEUED)
            StreamFlexDownloadService.startService(context)
            processQueue()
        }
    }

    /**
     * Cancels and deletes a download.
     */
    fun cancelDownload(id: String) {
        cancelledIds.add(id)
        activeJobs[id]?.cancel()
        activeJobs.remove(id)
        scope.launch {
            repository.deleteDownload(id)
            processQueue()
        }
    }

    /**
     * Retries a failed download.
     */
    fun retryDownload(id: String) {
        pausedIds.remove(id)
        cancelledIds.remove(id)
        scope.launch {
            repository.updateStatus(id, DownloadStatus.QUEUED, error = null)
            StreamFlexDownloadService.startService(context)
            processQueue()
        }
    }

    private suspend fun recoverInterruptedDownloads() {
        val all = repository.allDownloads.value
        all.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.CONNECTING }.forEach { item ->
            repository.updateStatus(item.id, DownloadStatus.QUEUED)
        }
        processQueue()
    }

    private fun processQueue() = synchronized(this) {
        scope.launch {
            val all = repository.allDownloads.value
            val activeCount = activeJobs.size

            if (activeCount >= MAX_CONCURRENT_DOWNLOADS) return@launch

            val nextItems = all.filter { it.status == DownloadStatus.QUEUED && !pausedIds.contains(it.id) && !cancelledIds.contains(it.id) }
                .take(MAX_CONCURRENT_DOWNLOADS - activeCount)

            if (nextItems.isEmpty() && activeJobs.isEmpty()) {
                // No active or queued downloads, stop foreground service
                StreamFlexDownloadService.stopService(context)
                return@launch
            }

            for (item in nextItems) {
                if (!activeJobs.containsKey(item.id)) {
                    startDownloadWorker(item)
                }
            }
        }
    }

    private fun startDownloadWorker(item: DownloadItem) {
        val job = scope.launch {
            try {
                repository.updateStatus(item.id, DownloadStatus.CONNECTING)

                val result = downloadEngine.executeDownload(
                    item = item,
                    onProgress = { downloaded, total, speed, eta ->
                        repository.updateProgress(item.id, downloaded, total, speed, eta)
                        val updated = repository.getDownloadById(item.id)
                        if (updated != null && updated.status != DownloadStatus.DOWNLOADING) {
                            repository.updateStatus(item.id, DownloadStatus.DOWNLOADING)
                        }
                    },
                    isPaused = { pausedIds.contains(item.id) },
                    isCancelled = { cancelledIds.contains(item.id) }
                )

                when (result) {
                    is DownloadEngine.Result.Success -> {
                        repository.updateStatus(
                            id = item.id,
                            status = DownloadStatus.COMPLETED,
                            localFilePath = result.file.absolutePath
                        )
                    }
                    is DownloadEngine.Result.Paused -> {
                        repository.updateStatus(item.id, DownloadStatus.PAUSED)
                    }
                    is DownloadEngine.Result.Cancelled -> {
                        repository.deleteDownload(item.id)
                    }
                    is DownloadEngine.Result.Error -> {
                        repository.updateStatus(
                            id = item.id,
                            status = DownloadStatus.FAILED,
                            error = result.message
                        )
                    }
                }
            } catch (e: CancellationException) {
                if (pausedIds.contains(item.id)) {
                    repository.updateStatus(item.id, DownloadStatus.PAUSED)
                }
            } catch (e: Exception) {
                repository.updateStatus(item.id, DownloadStatus.FAILED, error = e.message)
            } finally {
                activeJobs.remove(item.id)
                processQueue()
            }
        }

        activeJobs[item.id] = job
    }
}
