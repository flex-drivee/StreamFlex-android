package com.cinetheta.player.core

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.cinetheta.core.logger.Logger
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.StreamLink
import com.cinetheta.player.media3.Media3PlayerFactory
import com.cinetheta.player.quality.QualityOption
import com.cinetheta.player.tracks.AudioTrack
import com.cinetheta.player.tracks.SubtitleTrack
import com.cinetheta.player.vlc.VlcPlayerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * HybridPlayer
 *
 * Automatically routes playback between LibVLC and Media3 (ExoPlayer):
 * - Castle TV & Multi-Audio streams -> LibVLC (full elementary stream track detection & multi-voice support)
 * - Live Sports & Live Channels -> LibVLC (jitter-free 3000ms network caching & IPTV demuxing)
 * - VOD Movies & TV Shows -> Media3 (keyframe-accurate fast seeking & DASH .mpd support)
 * - Explicit user setting ("player_engine" = "VLC" or "MEDIA3") takes top priority.
 */
class HybridPlayer(
    private val context: Context,
    private val mediaId: String = "",
    private val initialIsLiveOrCastle: Boolean = false
) : StreamPlayer {

    companion object {
        private const val TAG = "HybridPlayer"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var _vlcPlayer: StreamPlayer? = null
    private var _media3Player: StreamPlayer? = null

    val vlcPlayer: StreamPlayer
        get() {
            if (_vlcPlayer == null) {
                _vlcPlayer = try {
                    VlcPlayerFactory.create(context)
                } catch (e: Throwable) {
                    Logger.e("HybridPlayer: Failed to instantiate VLC player, falling back to Media3: ${e.message}", TAG)
                    media3Player
                }
            }
            return _vlcPlayer!!
        }

    val media3Player: StreamPlayer
        get() {
            if (_media3Player == null) {
                _media3Player = Media3PlayerFactory.create(context)
            }
            return _media3Player!!
        }

    private fun determineInitialEngine(): String {
        val prefs = context.getSharedPreferences("cinetheta_settings", Context.MODE_PRIVATE)
        val enginePref = prefs.getString("player_engine", "AUTO") ?: "AUTO"
        return when {
            enginePref.equals("VLC", ignoreCase = true) -> "VLC"
            enginePref.equals("MEDIA3", ignoreCase = true) || enginePref.equals("EXO", ignoreCase = true) -> "MEDIA3"
            mediaId.contains("castle", ignoreCase = true) || (initialIsLiveOrCastle && mediaId.contains("castle", ignoreCase = true)) -> "VLC"
            else -> "MEDIA3"
        }
    }

    private val _activeEngineName = MutableStateFlow(determineInitialEngine())
    val activeEngineName: StateFlow<String> = _activeEngineName.asStateFlow()

    private var activePlayer: StreamPlayer = try {
        if (_activeEngineName.value == "VLC") vlcPlayer else media3Player
    } catch (e: Throwable) {
        Logger.e("HybridPlayer: Failed to activate initial player, falling back to Media3: ${e.message}", TAG)
        media3Player
    }

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PlayerEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<PlayerEvent> = _events.asSharedFlow()

    private var stateForwardJob: Job? = null
    private var eventsForwardJob: Job? = null

    init {
        bindPlayer(activePlayer)
    }

    private var currentStream: StreamLink? = null
    private var playbackStartedOnCurrentStream = false
    private var isFallbackAttempted = false

    private fun bindPlayer(player: StreamPlayer) {
        stateForwardJob?.cancel()
        eventsForwardJob?.cancel()

        stateForwardJob = scope.launch {
            player.state.collect { st ->
                _state.value = st
            }
        }

        eventsForwardJob = scope.launch {
            player.events.collect { ev ->
                if (ev is PlayerEvent.PlaybackStarted) {
                    playbackStartedOnCurrentStream = true
                }
                if (ev is PlayerEvent.Error && currentStream != null && !playbackStartedOnCurrentStream && !isFallbackAttempted) {
                    val fallbackStream = currentStream
                    val prefs = context.getSharedPreferences("cinetheta_settings", Context.MODE_PRIVATE)
                    val enginePref = prefs.getString("player_engine", "AUTO") ?: "AUTO"
                    if (fallbackStream != null) {
                        if (player == _vlcPlayer && !enginePref.equals("VLC", ignoreCase = true)) {
                            isFallbackAttempted = true
                            Logger.w("VLC failed to start playback for '${fallbackStream.name}'. Falling back automatically to Media3...", TAG)
                            try {
                                player.pause()
                            } catch (e: Exception) {}
                            activePlayer = media3Player
                            _activeEngineName.value = "MEDIA3"
                            _state.value = media3Player.state.value
                            bindPlayer(media3Player)
                            media3Player.load(fallbackStream)
                            return@collect
                        } else if (player == _media3Player && !enginePref.equals("MEDIA3", ignoreCase = true) && !enginePref.equals("EXO", ignoreCase = true)) {
                            isFallbackAttempted = true
                            Logger.w("Media3 failed to start playback for '${fallbackStream.name}'. Falling back automatically to VLC...", TAG)
                            try {
                                player.pause()
                            } catch (e: Exception) {}
                            activePlayer = vlcPlayer
                            _activeEngineName.value = "VLC"
                            _state.value = vlcPlayer.state.value
                            bindPlayer(vlcPlayer)
                            vlcPlayer.load(fallbackStream)
                            return@collect
                        }
                    }
                }
                _events.emit(ev)
            }
        }
    }

    private fun isCastleStream(stream: StreamLink): Boolean {
        val selectedProvider = try {
            com.cinetheta.app.di.ProviderModule.repository.selectedProviderId
        } catch (e: Exception) {
            null
        }

        return stream.name.contains("castle", ignoreCase = true) ||
               stream.host == HostType.CASTLETV ||
               stream.url.contains("castletv", ignoreCase = true) ||
               selectedProvider?.equals("castletv", ignoreCase = true) == true ||
               mediaId.contains("castletv", ignoreCase = true)
    }

    private fun isLiveStream(stream: StreamLink): Boolean {
        // DASH (.mpd) and MovieBox are NEVER live IPTV streams; they are VOD and require Media3
        if (stream.host == HostType.DASH ||
            stream.host == HostType.MOVIEBOX ||
            stream.url.contains(".mpd", ignoreCase = true) ||
            stream.contentType == com.cinetheta.core.network.detector.ContentType.DASH ||
            stream.name.contains("MovieBox", ignoreCase = true)) {
            return false
        }

        return initialIsLiveOrCastle ||
               mediaId.startsWith("live_sport_") ||
               mediaId.startsWith("live_") ||
               mediaId.contains("channel", ignoreCase = true) ||
               mediaId.contains("sport", ignoreCase = true) ||
               stream.name.startsWith("Server ", ignoreCase = true) ||
               stream.name.contains("Live", ignoreCase = true) ||
               stream.name.contains("Channel", ignoreCase = true) ||
               stream.name.contains("Sports", ignoreCase = true) ||
               stream.name.contains("IPTV", ignoreCase = true) ||
               stream.url.contains("playlist.m3u8", ignoreCase = true) ||
               stream.url.contains(":8000/play/", ignoreCase = true) ||
               stream.url.contains(":2122/play/", ignoreCase = true) ||
               stream.url.contains("bozztv.com", ignoreCase = true)
    }

    override fun load(stream: StreamLink) {
        val prefs = context.getSharedPreferences("cinetheta_settings", Context.MODE_PRIVATE)
        val enginePref = prefs.getString("player_engine", "AUTO") ?: "AUTO"

        val isDashOrMovieBox = stream.host == HostType.DASH ||
                               stream.host == HostType.MOVIEBOX ||
                               stream.url.contains(".mpd", ignoreCase = true) ||
                               stream.contentType == com.cinetheta.core.network.detector.ContentType.DASH ||
                               stream.name.contains("MovieBox", ignoreCase = true)

        val isDamiTv = stream.url.contains("damitv.st", ignoreCase = true)

        val shouldRunVlc = when {
            // Explicit user setting takes top priority
            enginePref.equals("VLC", ignoreCase = true) -> true
            enginePref.equals("MEDIA3", ignoreCase = true) || enginePref.equals("EXO", ignoreCase = true) -> false
            // MovieBox & DASH (.mpd) must ALWAYS use Media3/ExoPlayer (VLC cannot parse MovieBox DASH)
            isDashOrMovieBox -> false
            // damitv streams use TikTok CDN image chunks which VLC demuxer rejects; Media3 handles them smoothly
            isDamiTv -> false
            // Castle TV uses VLC for multi-audio support
            isCastleStream(stream) -> true
            // All other streams (Live HLS channels, Live Sports, VOD) use Media3 by default for rock-solid stability
            else -> false
        }

        val targetPlayer = if (shouldRunVlc) vlcPlayer else media3Player
        val targetName = if (shouldRunVlc) "VLC" else "MEDIA3"

        Logger.i("Routing stream '${stream.name}' (${stream.host}) to $targetName (enginePref=$enginePref)", TAG)

        currentStream = stream
        playbackStartedOnCurrentStream = false
        isFallbackAttempted = false

        if (targetPlayer != activePlayer) {
            try {
                activePlayer.pause()
            } catch (e: Exception) {
                Logger.w("Error pausing previous player: ${e.message}", TAG)
            }
            activePlayer = targetPlayer
            _activeEngineName.value = targetName
            _state.value = targetPlayer.state.value
            bindPlayer(activePlayer)
        }

        activePlayer.load(stream)
    }

    override fun play() = activePlayer.play()
    override fun pause() = activePlayer.pause()
    override fun seekTo(positionMs: Long) = activePlayer.seekTo(positionMs)
    override fun seekForward(ms: Long) = activePlayer.seekForward(ms)
    override fun seekBackward(ms: Long) = activePlayer.seekBackward(ms)
    override fun setVolume(volume: Float) = activePlayer.setVolume(volume)
    override fun setPlaybackSpeed(speed: Float) = activePlayer.setPlaybackSpeed(speed)
    override fun setQuality(quality: QualityOption) = activePlayer.setQuality(quality)
    override fun setAudioTrack(track: AudioTrack) = activePlayer.setAudioTrack(track)
    override fun setSubtitleTrack(track: SubtitleTrack?) = activePlayer.setSubtitleTrack(track)

    override fun release() {
        stateForwardJob?.cancel()
        eventsForwardJob?.cancel()
        scope.cancel()
        try { _media3Player?.release() } catch (e: Exception) {}
        try { _vlcPlayer?.release() } catch (e: Exception) {}
    }

    @Composable
    override fun Surface(
        modifier: Modifier,
        isFullScreen: Boolean,
        resizeMode: Int
    ) {
        val engineName by activeEngineName.collectAsState()
        if (engineName == "VLC") {
            vlcPlayer.Surface(modifier = modifier, isFullScreen = isFullScreen, resizeMode = resizeMode)
        } else {
            media3Player.Surface(modifier = modifier, isFullScreen = isFullScreen, resizeMode = resizeMode)
        }
    }
}
