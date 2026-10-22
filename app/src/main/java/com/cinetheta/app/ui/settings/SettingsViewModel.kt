package com.cinetheta.app.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.cinetheta.core.cache.CacheManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VideoPlayer(val title: String, val packageName: String?) {
    SYSTEM("Built-in Player (Auto: LibVLC & Media3)", null),
    VLC("External VLC App", "org.videolan.vlc"),
    MX_PLAYER("External MX Player App", "com.mxtech.videoplayer.ad"),
    MX_PLAYER_PRO("External MX Player Pro App", "com.mxtech.videoplayer.pro"),
    JUST_PLAYER("External Just Player App", "com.brouken.player")
}

enum class PlayerEngine(val key: String, val title: String, val description: String) {
    AUTO("AUTO", "Auto (Smart Selection)", "LibVLC for Castle TV & Live Sports, Media3 for Movies/Shows"),
    VLC("VLC", "LibVLC Engine", "Forces VLC for all playback (Best for Multi-Audio & Live Streams)"),
    MEDIA3("MEDIA3", "Media3 / ExoPlayer", "Forces ExoPlayer for all playback (Fast seeking)")
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("cinetheta_settings", Context.MODE_PRIVATE)
    private val cacheManager = CacheManager()

    private val _appTheme = MutableStateFlow(prefs.getString("app_theme", "SYSTEM") ?: "SYSTEM")
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _autoPlayNext = MutableStateFlow(prefs.getBoolean("autoplay_next", true))
    val autoPlayNext: StateFlow<Boolean> = _autoPlayNext.asStateFlow()

    private val _enableSubtitles = MutableStateFlow(prefs.getBoolean("enable_subtitles", true))
    val enableSubtitles: StateFlow<Boolean> = _enableSubtitles.asStateFlow()

    private val _subtitleTextColor = MutableStateFlow(prefs.getString("subtitle_text_color", "#FFFFFF") ?: "#FFFFFF")
    val subtitleTextColor: StateFlow<String> = _subtitleTextColor.asStateFlow()

    private val _subtitleBgColor = MutableStateFlow(prefs.getString("subtitle_bg_color", "#80000000") ?: "#80000000")
    val subtitleBgColor: StateFlow<String> = _subtitleBgColor.asStateFlow()
    
    private val _cellularData = MutableStateFlow(prefs.getBoolean("cellular_data", true))
    val cellularData: StateFlow<Boolean> = _cellularData.asStateFlow()
    
    private val _developerMode = MutableStateFlow(prefs.getBoolean("developer_mode", false))
    val developerMode: StateFlow<Boolean> = _developerMode.asStateFlow()

    private val _decoderMode = MutableStateFlow(
        prefs.getString(
            com.cinetheta.player.core.DecoderMode.PREF_KEY,
            com.cinetheta.player.core.DecoderMode.AUTO.key
        ) ?: com.cinetheta.player.core.DecoderMode.AUTO.key
    )
    val decoderMode: StateFlow<String> = _decoderMode.asStateFlow()

    private val _dohProvider = MutableStateFlow(
        prefs.getString("doh_provider", com.cinetheta.core.network.DohProvider.GOOGLE.name) ?: com.cinetheta.core.network.DohProvider.GOOGLE.name
    )
    val dohProvider: StateFlow<String> = _dohProvider.asStateFlow()

    private val _playerVideoQuality = MutableStateFlow(prefs.getString("player_video_quality", "Auto") ?: "Auto")
    val playerVideoQuality: StateFlow<String> = _playerVideoQuality.asStateFlow()
    
    private val _preferredPlayer = MutableStateFlow(
        VideoPlayer.values().find { it.name == (prefs.getString("preferred_player", VideoPlayer.SYSTEM.name) ?: VideoPlayer.SYSTEM.name) } ?: VideoPlayer.SYSTEM
    )
    val preferredPlayer: StateFlow<VideoPlayer> = _preferredPlayer.asStateFlow()

    private val _playerEngine = MutableStateFlow(
        PlayerEngine.values().find { it.key.equals(prefs.getString("player_engine", PlayerEngine.AUTO.key), ignoreCase = true) } ?: PlayerEngine.AUTO
    )
    val playerEngine: StateFlow<PlayerEngine> = _playerEngine.asStateFlow()

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

    fun setSubtitleTextColor(colorHex: String) {
        prefs.edit().putString("subtitle_text_color", colorHex).apply()
        _subtitleTextColor.value = colorHex
    }

    fun setSubtitleBgColor(colorHex: String) {
        prefs.edit().putString("subtitle_bg_color", colorHex).apply()
        _subtitleBgColor.value = colorHex
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
        prefs.edit().putString(com.cinetheta.player.core.DecoderMode.PREF_KEY, mode).apply()
        _decoderMode.value = mode
    }

    fun setDohProvider(provider: String) {
        prefs.edit().putString("doh_provider", provider).apply()
        _dohProvider.value = provider
    }

    fun setPlayerVideoQuality(quality: String) {
        prefs.edit().putString("player_video_quality", quality).apply()
        _playerVideoQuality.value = quality
    }

    fun setPreferredPlayer(player: VideoPlayer) {
        prefs.edit().putString("preferred_player", player.name).apply()
        _preferredPlayer.value = player
    }

    fun setPlayerEngine(engine: PlayerEngine) {
        prefs.edit().putString("player_engine", engine.key).apply()
        _playerEngine.value = engine
    }

    /**
     * Clears all app caches: domain manifests, search results, TMDB items,
     * provider manifests, and Cloudflare cookies.
     * Returns true if successful.
     */
    fun clearCache(): Boolean {
        return try {
            cacheManager.clearAll()
            // Also clear Cloudflare bypass cookies
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.CookieManager.getInstance().flush()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Full factory reset: wipes all SharedPreferences settings and all caches.
     * StateFlows are reset to defaults so the UI reflects the change immediately
     * without requiring an app restart.
     * Returns true if successful.
     */
    fun factoryReset(): Boolean {
        return try {
            // 1. Wipe all SharedPreferences
            prefs.edit().clear().apply()
            // 2. Clear all caches and cookies
            cacheManager.clearAll()
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.CookieManager.getInstance().flush()
            // 3. Reset all StateFlows to defaults so UI updates immediately
            _appTheme.value = "SYSTEM"
            _autoPlayNext.value = true
            _enableSubtitles.value = true
            _subtitleTextColor.value = "#FFFFFF"
            _subtitleBgColor.value = "#80000000"
            _cellularData.value = true
            _developerMode.value = false
            _playerVideoQuality.value = "Auto"
            _preferredPlayer.value = VideoPlayer.SYSTEM
            _playerEngine.value = PlayerEngine.AUTO
            true
        } catch (e: Exception) {
            false
        }
    }
}
