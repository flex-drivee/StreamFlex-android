package com.streamflex.app.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("streamflex_settings", Context.MODE_PRIVATE)

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
}
