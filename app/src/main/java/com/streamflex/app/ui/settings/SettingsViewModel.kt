package com.streamflex.app.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.streamflex.app.di.EngineModule
import com.streamflex.app.di.RepositoryModule
import com.streamflex.data.local.download.DownloadStorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("streamflex_settings", Context.MODE_PRIVATE)
    private val storageManager = RepositoryModule.downloadStorageManager
    private val downloadRepository = RepositoryModule.downloadRepository
    private val downloadQueueManager = EngineModule.downloadQueueManager

    private val _appTheme = MutableStateFlow(prefs.getString("app_theme", "SKY_DARK") ?: "SKY_DARK")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _autoPlayNext = MutableStateFlow(prefs.getBoolean("autoplay_next", true))
    val autoPlayNext: StateFlow<Boolean> = _autoPlayNext.asStateFlow()

    private val _enableSubtitles = MutableStateFlow(prefs.getBoolean("enable_subtitles", false))
    val enableSubtitles: StateFlow<Boolean> = _enableSubtitles.asStateFlow()
    
    private val _cellularData = MutableStateFlow(prefs.getBoolean("cellular_data", true))
    val cellularData: StateFlow<Boolean> = _cellularData.asStateFlow()
    
    private val _developerMode = MutableStateFlow(prefs.getBoolean("developer_mode", false))
    val developerMode: StateFlow<Boolean> = _developerMode.asStateFlow()

    private val _decoderMode = MutableStateFlow(
        prefs.getString(
            com.streamflex.player.core.DecoderMode.PREF_KEY,
            com.streamflex.player.core.DecoderMode.AUTO.key
        ) ?: com.streamflex.player.core.DecoderMode.AUTO.key
    )
    val decoderMode: StateFlow<String> = _decoderMode.asStateFlow()

    private val _dohProvider = MutableStateFlow(
        prefs.getString("doh_provider", com.streamflex.core.network.DohProvider.NONE.name) ?: com.streamflex.core.network.DohProvider.NONE.name
    )
    val dohProvider: StateFlow<String> = _dohProvider.asStateFlow()

    // --- Downloads Settings ---
    private val _wifiOnlyDownloads = MutableStateFlow(prefs.getBoolean("wifi_only_downloads", false))
    val wifiOnlyDownloads: StateFlow<Boolean> = _wifiOnlyDownloads.asStateFlow()

    private val _downloadQuality = MutableStateFlow(prefs.getString("download_quality", "1080p") ?: "1080p")
    val downloadQuality: StateFlow<String> = _downloadQuality.asStateFlow()

    private val _smartDownloads = MutableStateFlow(prefs.getBoolean("smart_downloads", true))
    val smartDownloads: StateFlow<Boolean> = _smartDownloads.asStateFlow()

    private val _storageStats = MutableStateFlow(storageManager.getStorageStats())
    val storageStats: StateFlow<DownloadStorageManager.StorageStats> = _storageStats.asStateFlow()

    init {
        refreshStorageStats()
    }

    fun refreshStorageStats() {
        _storageStats.value = storageManager.getStorageStats()
    }

    fun setAppTheme(theme: String) {
        prefs.edit().putString("app_theme", theme).apply()
        _appTheme.value = theme
    }

    fun setAutoPlayNext(enabled: Boolean) {
        prefs.edit().putBoolean("autoplay_next", enabled).apply()
        _autoPlayNext.value = enabled
    }

    fun setEnableSubtitles(enabled: Boolean) {
        prefs.edit().putBoolean("enable_subtitles", enabled).apply()
        _enableSubtitles.value = enabled
    }
    
    fun setCellularData(enabled: Boolean) {
        prefs.edit().putBoolean("cellular_data", enabled).apply()
        _cellularData.value = enabled
    }
    
    fun setDeveloperMode(enabled: Boolean) {
        prefs.edit().putBoolean("developer_mode", enabled).apply()
        _developerMode.value = enabled
    }

    fun setDecoderMode(mode: String) {
        prefs.edit().putString(com.streamflex.player.core.DecoderMode.PREF_KEY, mode).apply()
        _decoderMode.value = mode
    }

    fun setDohProvider(provider: String) {
        prefs.edit().putString("doh_provider", provider).apply()
        _dohProvider.value = provider
    }

    fun setWifiOnlyDownloads(enabled: Boolean) {
        prefs.edit().putBoolean("wifi_only_downloads", enabled).apply()
        _wifiOnlyDownloads.value = enabled
    }

    fun setDownloadQuality(quality: String) {
        prefs.edit().putString("download_quality", quality).apply()
        _downloadQuality.value = quality
    }

    fun setSmartDownloads(enabled: Boolean) {
        prefs.edit().putBoolean("smart_downloads", enabled).apply()
        _smartDownloads.value = enabled
    }

    fun deleteAllDownloads() {
        viewModelScope.launch {
            val all = downloadRepository.allDownloads.value
            all.forEach { item ->
                downloadQueueManager.cancelDownload(item.id)
            }
            refreshStorageStats()
        }
    }
}
