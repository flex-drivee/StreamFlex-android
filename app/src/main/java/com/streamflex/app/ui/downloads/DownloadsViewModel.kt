package com.streamflex.app.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.streamflex.app.di.EngineModule
import com.streamflex.app.di.RepositoryModule
import com.streamflex.data.local.download.DownloadStorageManager
import com.streamflex.domain.models.download.DownloadItem
import com.streamflex.domain.repositories.DownloadRepository
import com.streamflex.engine.download.DownloadQueueManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DownloadsViewModel(
    private val downloadRepository: DownloadRepository = RepositoryModule.downloadRepository,
    private val downloadQueueManager: DownloadQueueManager = EngineModule.downloadQueueManager,
    private val storageManager: DownloadStorageManager = RepositoryModule.downloadStorageManager
) : ViewModel() {

    val allDownloads: StateFlow<List<DownloadItem>> = downloadRepository.allDownloads

    private val _storageStats = MutableStateFlow(storageManager.getStorageStats())
    val storageStats: StateFlow<DownloadStorageManager.StorageStats> = _storageStats.asStateFlow()

    private val _smartDownloadsEnabled = MutableStateFlow(true)
    val smartDownloadsEnabled: StateFlow<Boolean> = _smartDownloadsEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            allDownloads.collect {
                _storageStats.value = storageManager.getStorageStats()
            }
        }
    }

    fun pauseDownload(id: String) {
        downloadQueueManager.pauseDownload(id)
    }

    fun resumeDownload(id: String) {
        downloadQueueManager.resumeDownload(id)
    }

    fun cancelDownload(id: String) {
        downloadQueueManager.cancelDownload(id)
    }

    fun retryDownload(id: String) {
        downloadQueueManager.retryDownload(id)
    }

    fun deleteDownload(id: String) {
        downloadQueueManager.cancelDownload(id)
    }

    fun deleteAllDownloads() {
        viewModelScope.launch {
            val list = allDownloads.value
            list.forEach { item ->
                downloadQueueManager.cancelDownload(item.id)
            }
        }
    }

    fun toggleSmartDownloads(enabled: Boolean) {
        _smartDownloadsEnabled.value = enabled
    }
}

class DownloadsViewModelFactory(
    private val downloadRepository: DownloadRepository = RepositoryModule.downloadRepository,
    private val downloadQueueManager: DownloadQueueManager = EngineModule.downloadQueueManager,
    private val storageManager: DownloadStorageManager = RepositoryModule.downloadStorageManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DownloadsViewModel(downloadRepository, downloadQueueManager, storageManager) as T
    }
}
