package com.streamflex.data.local.download

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.streamflex.domain.models.download.DownloadItem
import com.streamflex.domain.models.download.DownloadStatus
import com.streamflex.domain.repositories.DownloadRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Robust JSON-backed repository implementation for downloads.
 * Provides instant reactive StateFlow updates in memory and atomic disk persistence.
 */
class JsonDownloadRepository(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : DownloadRepository {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private val mutex = Mutex()
    private val manifestFile: File by lazy {
        File(context.filesDir, "downloads_manifest.json")
    }

    private val _allDownloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    override val allDownloads: StateFlow<List<DownloadItem>> = _allDownloads.asStateFlow()

    override val activeDownloads: StateFlow<List<DownloadItem>> = _allDownloads.map { list ->
        list.filter { it.status.isActive || it.status.isPaused }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val completedDownloads: StateFlow<List<DownloadItem>> = _allDownloads.map { list ->
        list.filter { it.status.isCompleted }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        scope.launch(Dispatchers.IO) {
            mutex.withLock {
                try {
                    if (manifestFile.exists()) {
                        val json = manifestFile.readText(Charsets.UTF_8)
                        val type = object : TypeToken<List<DownloadItem>>() {}.type
                        val list: List<DownloadItem>? = gson.fromJson(json, type)
                        if (list != null) {
                            _allDownloads.value = list
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to empty if corrupted
                    _allDownloads.value = emptyList()
                }
            }
        }
    }

    private suspend fun persistToDisk() = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                val json = gson.toJson(_allDownloads.value)
                val tempFile = File(context.filesDir, "downloads_manifest.tmp")
                tempFile.writeText(json, Charsets.UTF_8)
                tempFile.renameTo(manifestFile)
            } catch (_: Exception) {
                // Best effort persistence
            }
        }
    }

    override suspend fun getDownloadById(id: String): DownloadItem? {
        return _allDownloads.value.firstOrNull { it.id == id }
    }

    override suspend fun getDownloadForMedia(
        mediaId: String,
        seasonNumber: Int?,
        episodeNumber: Int?
    ): DownloadItem? {
        return _allDownloads.value.firstOrNull { item ->
            item.mediaId == mediaId &&
            item.seasonNumber == seasonNumber &&
            item.episodeNumber == episodeNumber
        }
    }

    override suspend fun isDownloaded(
        mediaId: String,
        seasonNumber: Int?,
        episodeNumber: Int?
    ): Boolean {
        val item = getDownloadForMedia(mediaId, seasonNumber, episodeNumber)
        if (item != null && item.status == DownloadStatus.COMPLETED && !item.localFilePath.isNullOrBlank()) {
            val file = File(item.localFilePath)
            return file.exists() && file.length() > 0
        }
        return false
    }

    override suspend fun saveDownload(item: DownloadItem) {
        val current = _allDownloads.value.toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) {
            current[index] = item
        } else {
            current.add(0, item)
        }
        _allDownloads.value = current
        persistToDisk()
    }

    override suspend fun updateProgress(
        id: String,
        downloadedBytes: Long,
        totalBytes: Long,
        speedBytesPerSec: Long,
        etaSeconds: Long
    ) {
        val current = _allDownloads.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val old = current[index]
            current[index] = old.copy(
                downloadedBytes = downloadedBytes,
                totalBytes = if (totalBytes > 0) totalBytes else old.totalBytes,
                speedBytesPerSec = speedBytesPerSec,
                etaSeconds = etaSeconds
            )
            _allDownloads.value = current
        }
    }

    override suspend fun updateStatus(
        id: String,
        status: DownloadStatus,
        error: String?,
        localFilePath: String?
    ) {
        val current = _allDownloads.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) {
            val old = current[index]
            current[index] = old.copy(
                status = status,
                errorMessage = error,
                localFilePath = localFilePath ?: old.localFilePath,
                completedAt = if (status == DownloadStatus.COMPLETED) System.currentTimeMillis() else old.completedAt,
                speedBytesPerSec = if (status == DownloadStatus.COMPLETED || status == DownloadStatus.PAUSED) 0L else old.speedBytesPerSec,
                etaSeconds = if (status == DownloadStatus.COMPLETED) 0L else old.etaSeconds
            )
            _allDownloads.value = current
            persistToDisk()
        }
    }

    override suspend fun deleteDownload(id: String) {
        val current = _allDownloads.value.toMutableList()
        val item = current.firstOrNull { it.id == id }
        if (item != null) {
            current.removeAll { it.id == id }
            _allDownloads.value = current
            
            // Delete actual file from storage if present
            if (!item.localFilePath.isNullOrBlank()) {
                try {
                    val file = File(item.localFilePath)
                    if (file.exists()) file.delete()
                    // Delete any matching subtitles
                    file.parentFile?.listFiles()?.filter {
                        it.name.startsWith(file.nameWithoutExtension) && it.extension in listOf("srt", "vtt")
                    }?.forEach { it.delete() }
                } catch (_: Exception) {}
            }
            
            persistToDisk()
        }
    }
}
