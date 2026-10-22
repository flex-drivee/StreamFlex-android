package com.cinetheta.player.vlc

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.cinetheta.core.logger.Logger
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.StreamLink
import com.cinetheta.player.core.PlayerError
import com.cinetheta.player.core.PlayerEvent
import com.cinetheta.player.core.PlayerState
import com.cinetheta.player.core.StreamPlayer
import com.cinetheta.player.quality.QualityOption
import com.cinetheta.player.tracks.AudioTrack
import com.cinetheta.player.tracks.SubtitleTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.util.VLCVideoLayout

/**
 * VlcPlayer
 *
 * LibVLC implementation of [StreamPlayer].
 * Designed specifically for Live TV / Live Sports (IPTV, MPEG-TS, RTSP)
 * and multi-audio providers (such as Castle TV) where native FFmpeg demuxing
 * and resilient network buffering outperform strict ExoPlayer pipelines.
 */
class VlcPlayer(
    private val context: Context
) : StreamPlayer {

    companion object {
        private const val TAG = "VlcPlayer"

        // Standard, verified LibVLC CLI options tuned for low-latency live streams & multi-audio demuxing
        // SurfaceView overlay + predictive adaptive logic + http auto-reconnect prevents ANRs & demux crashes.
        // NOTE: Never pass invalid options like --http-timeout or stream-specific referrers here;
        // per-media options belong on the Media instance in load().
        private val VLC_OPTIONS = arrayListOf(
            "--network-caching=2000",
            "--live-caching=2000",
            "--file-caching=2000",
            "--sout-mux-caching=2000",
            "--http-reconnect",
            "--rtsp-tcp",
            "--audio-time-stretch",
            "--adaptive-logic=rate"
        )
    }

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PlayerEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<PlayerEvent> = _events.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var seekJob: Job? = null
    private var pendingSeekPosition: Long? = null
    private var hasStartedPlayback = false
    private var isUserPaused = false
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0

    private val isLiveStream: Boolean
        get() {
            val stream = currentStream ?: return false
            if (stream.adaptive) return true
            val len = mediaPlayer.length
            val isVoD = len > 30000L && mediaPlayer.isSeekable
            if (isVoD) return false
            return len <= 0L ||
                   !mediaPlayer.isSeekable ||
                   stream.contentType == com.cinetheta.core.network.detector.ContentType.HLS ||
                   stream.contentType == com.cinetheta.core.network.detector.ContentType.M3U8 ||
                   stream.contentType == com.cinetheta.core.network.detector.ContentType.DASH ||
                   stream.url.contains(".m3u8", ignoreCase = true) ||
                   stream.url.contains(".ts", ignoreCase = true)
        }

    private fun scheduleLiveReconnect() {
        if (isUserPaused) return
        if (reconnectAttempts >= 8) {
            Logger.w("Live stream exceeded max reconnect attempts (8)", TAG)
            val err = PlayerError.InvalidSource("Live stream disconnected")
            _state.value = _state.value.copy(
                isPlaying = false,
                isBuffering = false,
                error = err
            )
            emitEvent(PlayerEvent.Error(err))
            return
        }

        if (reconnectJob?.isActive == true) return

        _state.value = _state.value.copy(
            isBuffering = true
        )

        reconnectJob = scope.launch {
            reconnectAttempts++
            val delayMs = if (reconnectAttempts <= 2) 1000L else 2500L
            Logger.d("Scheduling live reconnect attempt #$reconnectAttempts in ${delayMs}ms", TAG)
            delay(delayMs)
            if (!isUserPaused) {
                currentStream?.let { stream ->
                    internalLoad(stream, isReconnect = true)
                }
            }
        }
    }

    private val libVLC: LibVLC = try {
        LibVLC(context, VLC_OPTIONS)
    } catch (e: Throwable) {
        Logger.e("Failed to initialize LibVLC with options, falling back to basic options: ${e.message}", TAG)
        try {
            LibVLC(context, arrayListOf("--http-reconnect", "--network-caching=4000", "--live-caching=3000"))
        } catch (e2: Throwable) {
            Logger.e("Failed to initialize LibVLC with basic options, falling back to default: ${e2.message}", TAG)
            LibVLC(context)
        }
    }
    private val mediaPlayer: MediaPlayer = MediaPlayer(libVLC)

    private var currentVideoLayout: VLCVideoLayout? = null
    private var currentStream: StreamLink? = null
    private val prefs by lazy { context.getSharedPreferences("cinetheta_settings", Context.MODE_PRIVATE) }

    init {
        mediaPlayer.setEventListener { event ->
            scope.launch {
                handleVlcEvent(event)
            }
        }
    }

    private fun handleVlcEvent(event: MediaPlayer.Event) {
        when (event.type) {
            MediaPlayer.Event.Buffering -> {
                val bufferingPercent = event.buffering
                val isBuffering = bufferingPercent < 100f
                _state.value = _state.value.copy(
                    isBuffering = isBuffering
                )
            }
            MediaPlayer.Event.Playing -> {
                hasStartedPlayback = true
                reconnectAttempts = 0
                reconnectJob?.cancel()
                reconnectJob = null
                val stillBuffering = mediaPlayer.time <= 0L && _state.value.isBuffering
                _state.value = _state.value.copy(
                    isPlaying = true,
                    isBuffering = stillBuffering,
                    durationMs = mediaPlayer.length.coerceAtLeast(0L),
                    error = null
                )
                startProgressTracking()
                emitEvent(PlayerEvent.PlaybackStarted)
                updateTracks()
            }
            MediaPlayer.Event.Paused -> {
                _state.value = _state.value.copy(
                    isPlaying = false,
                    isBuffering = false
                )
                stopProgressTracking()
                emitEvent(PlayerEvent.PlaybackPaused)
            }
            MediaPlayer.Event.Stopped -> {
                stopProgressTracking()
                if (isLiveStream && hasStartedPlayback && !isUserPaused) {
                    Logger.d("Live stream stopped unexpectedly, auto-reconnecting...", TAG)
                    scheduleLiveReconnect()
                } else {
                    _state.value = _state.value.copy(
                        isPlaying = false,
                        isBuffering = false
                    )
                    if (!hasStartedPlayback && !isUserPaused) {
                        val err = PlayerError.InvalidSource("Stream disconnected or failed to connect")
                        _state.value = _state.value.copy(error = err)
                        emitEvent(PlayerEvent.Error(err))
                    }
                }
            }
            MediaPlayer.Event.EndReached -> {
                stopProgressTracking()
                if (isLiveStream && hasStartedPlayback && !isUserPaused) {
                    Logger.d("Live stream reached end unexpectedly, auto-reconnecting...", TAG)
                    scheduleLiveReconnect()
                } else {
                    _state.value = _state.value.copy(
                        isPlaying = false,
                        isBuffering = false
                    )
                    if (!hasStartedPlayback) {
                        val err = PlayerError.InvalidSource("Live stream ended or unreachable")
                        _state.value = _state.value.copy(error = err)
                        emitEvent(PlayerEvent.Error(err))
                    } else {
                        emitEvent(PlayerEvent.PlaybackEnded)
                    }
                }
            }
            MediaPlayer.Event.EncounteredError -> {
                Logger.e("VLC encountered playback error", TAG)
                if (isLiveStream && hasStartedPlayback && !isUserPaused && reconnectAttempts < 5) {
                    Logger.d("Live stream error encountered, auto-reconnecting...", TAG)
                    scheduleLiveReconnect()
                } else {
                    val err = PlayerError.InvalidSource("VLC Stream Playback Failed")
                    _state.value = _state.value.copy(
                        isPlaying = false,
                        isBuffering = false,
                        error = err
                    )
                    stopProgressTracking()
                    emitEvent(PlayerEvent.Error(err))
                }
            }
            MediaPlayer.Event.Vout -> {
                _state.value = _state.value.copy(isBuffering = false)
            }
            MediaPlayer.Event.TimeChanged -> {
                if (_state.value.isBuffering) {
                    _state.value = _state.value.copy(isBuffering = false)
                }
                if (pendingSeekPosition == null) {
                    val newPos = mediaPlayer.time.coerceAtLeast(0L)
                    val curPos = _state.value.positionMs
                    if (kotlin.math.abs(newPos - curPos) >= 300L) {
                        _state.value = _state.value.copy(
                            positionMs = newPos,
                            durationMs = mediaPlayer.length.coerceAtLeast(0L)
                        )
                    }
                }
            }
            MediaPlayer.Event.ESAdded,
            MediaPlayer.Event.ESDeleted,
            MediaPlayer.Event.ESSelected -> {
                updateTracks()
            }
        }
    }

    private fun updateTracks() {
        // ── 1. Audio Tracks ───────────────────────────────────────────────────
        val currentAudioId = mediaPlayer.audioTrack
        val rawAudioTracks = mediaPlayer.audioTracks
        val parsedAudioTracks = if (rawAudioTracks != null) {
            rawAudioTracks.filter { it.id != -1 }.map { trackDesc ->
                AudioTrack(
                    id = trackDesc.id.toString(),
                    language = trackDesc.name,
                    label = trackDesc.name,
                    isSelected = trackDesc.id == currentAudioId
                )
            }
        } else {
            emptyList()
        }

        val selectedAudio = parsedAudioTracks.find { it.isSelected }

        // ── 2. Subtitle (SPU) Tracks ──────────────────────────────────────────
        val currentSpuId = mediaPlayer.spuTrack
        val rawSpuTracks = mediaPlayer.spuTracks
        val parsedSubtitleTracks = mutableListOf<SubtitleTrack>()

        if (rawSpuTracks != null) {
            rawSpuTracks.filter { it.id != -1 }.forEach { trackDesc ->
                val matchedSub = currentStream?.subtitles?.find { sub ->
                    trackDesc.name.contains(sub.language, ignoreCase = true) ||
                    trackDesc.name.contains(sub.label, ignoreCase = true)
                }
                parsedSubtitleTracks.add(
                    SubtitleTrack(
                        id = trackDesc.id.toString(),
                        language = matchedSub?.language ?: trackDesc.name,
                        label = matchedSub?.label ?: trackDesc.name,
                        url = matchedSub?.url,
                        isEmbedded = matchedSub == null,
                        isSelected = trackDesc.id == currentSpuId
                    )
                )
            }
        }

        // Merge any stream external subtitles that are not already present in SPU list
        currentStream?.subtitles?.forEachIndexed { idx, sub ->
            val exists = parsedSubtitleTracks.any {
                it.url == sub.url ||
                (it.label?.equals(sub.label, ignoreCase = true) == true && !it.isEmbedded) ||
                it.language?.equals(sub.language, ignoreCase = true) == true
            }
            if (!exists) {
                parsedSubtitleTracks.add(
                    SubtitleTrack(
                        id = "ext_${idx}",
                        language = sub.language,
                        label = sub.label,
                        url = sub.url,
                        isEmbedded = false,
                        isSelected = sub.isDefault && currentSpuId == -1
                    )
                )
            }
        }

        val selectedSub = parsedSubtitleTracks.find { it.isSelected }

        _state.value = _state.value.copy(
            audioTracks = parsedAudioTracks,
            selectedAudio = selectedAudio,
            subtitleTracks = parsedSubtitleTracks,
            selectedSubtitle = selectedSub
        )

        // Auto-select preferred subtitle language if enabled and not already selected
        val enableSubtitles = prefs.getBoolean("enable_subtitles", true)
        if (!enableSubtitles) {
            if (currentSpuId != -1) {
                mediaPlayer.spuTrack = -1
            }
        } else if (selectedSub == null && parsedSubtitleTracks.isNotEmpty()) {
            val preferredSub = prefs.getString("preferred_subtitle_language", null)
            val match = if (!preferredSub.isNullOrBlank()) {
                parsedSubtitleTracks.find {
                    it.label?.contains(preferredSub, ignoreCase = true) == true ||
                    it.language?.contains(preferredSub, ignoreCase = true) == true
                }
            } else {
                parsedSubtitleTracks.firstOrNull { it.label?.contains("English", ignoreCase = true) == true }
                    ?: parsedSubtitleTracks.firstOrNull()
            }
            if (match != null) {
                setSubtitleTrack(match)
            }
        }

        // Auto-select preferred audio language if not already explicitly matched
        if (selectedAudio == null && parsedAudioTracks.isNotEmpty()) {
            val preferredLang = prefs.getString("preferred_audio_language", null)
            if (!preferredLang.isNullOrBlank()) {
                val match = parsedAudioTracks.find {
                    it.label?.contains(preferredLang, ignoreCase = true) == true ||
                    it.language?.contains(preferredLang, ignoreCase = true) == true
                }
                if (match != null) {
                    setAudioTrack(match)
                }
            }
        }
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (mediaPlayer.isPlaying && pendingSeekPosition == null) {
                    _state.value = _state.value.copy(
                        positionMs = mediaPlayer.time.coerceAtLeast(0L),
                        durationMs = mediaPlayer.length.coerceAtLeast(0L)
                    )
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun emitEvent(event: PlayerEvent) {
        scope.launch {
            _events.emit(event)
        }
    }

    override fun load(stream: StreamLink) {
        reconnectAttempts = 0
        isUserPaused = false
        internalLoad(stream, isReconnect = false)
    }

    private fun internalLoad(stream: StreamLink, isReconnect: Boolean) {
        if (!isReconnect) {
            reconnectJob?.cancel()
            reconnectJob = null
        }
        currentStream = stream
        hasStartedPlayback = false
        seekJob?.cancel()
        seekJob = null
        pendingSeekPosition = null
        stopProgressTracking()

        try {
            val uri = Uri.parse(stream.url)
            val media = Media(libVLC, uri)

            // Pass headers (User-Agent, Referer, Cookies, etc.)
            val customHeaders = StringBuilder()
            var hasUserAgent = false
            stream.headers.forEach { (key, value) ->
                when {
                    key.equals("user-agent", ignoreCase = true) -> {
                        hasUserAgent = true
                        media.addOption(":http-user-agent=$value")
                    }
                    key.equals("referer", ignoreCase = true) -> {
                        media.addOption(":http-referrer=$value")
                    }
                    else -> {
                        customHeaders.append("$key: $value\r\n")
                    }
                }
            }
            if (!hasUserAgent) {
                media.addOption(":http-user-agent=Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36")
            }
            if (customHeaders.isNotEmpty()) {
                media.addOption(":http-custom-headers=${customHeaders.toString().trimEnd()}")
            }

            // Stream caching & resiliency parameters
            // Castle TV serves VOD HLS from Google Edge Cache (fast CDN) — use 1500ms caching
            // to avoid the 4s unnecessary pre-buffering delay that occurs with live-stream caching values.
            // Live TV / IPTV / Sports still use 4000ms for jitter resilience.
            val isCastleVodStream = stream.host == HostType.CASTLETV ||
                stream.name.contains("castle", ignoreCase = true) ||
                stream.url.contains("hscow.com", ignoreCase = true) ||
                stream.url.contains("fcxmb.com", ignoreCase = true) ||
                stream.url.contains("castletv", ignoreCase = true)
            val networkCaching = if (isCastleVodStream) 1500 else 4000
            val liveCaching   = if (isCastleVodStream) 1500 else 3000

            media.addOption(":adaptive-logic=rate")
            media.addOption(":http-reconnect")
            media.addOption(":network-caching=$networkCaching")
            media.addOption(":live-caching=$liveCaching")
            media.addOption(":file-caching=3000")
            media.addOption(":sout-mux-caching=3000")
            media.addOption(":rtsp-tcp")
            // Suppress "sub-original-fps does not exist" log noise from LibVLC subtitle slave processing
            media.addOption(":no-subtitle-autodetect")

            // Attach external subtitles if present
            stream.subtitles.forEach { sub ->
                try {
                    media.addSlave(
                        IMedia.Slave(
                            IMedia.Slave.Type.Subtitle,
                            if (sub.isDefault) 4 else 2,
                            sub.url
                        )
                    )
                } catch (e: Throwable) {
                    Logger.w("Failed to add media subtitle slave: ${e.message}", TAG)
                }
            }

            mediaPlayer.media = media
            media.release()

            _state.value = _state.value.copy(
                isBuffering = true,
                error = null,
                positionMs = 0L,
                durationMs = 0L,
                audioTracks = emptyList(),
                subtitleTracks = emptyList()
            )

            mediaPlayer.play()
        } catch (e: Exception) {
            Logger.e("Failed to load stream in VLC: ${e.message}", TAG)
            val err = PlayerError.InvalidSource("VLC Load Error: ${e.localizedMessage}")
            _state.value = _state.value.copy(
                isBuffering = false,
                error = err
            )
            emitEvent(PlayerEvent.Error(err))
        }
    }

    override fun play() {
        isUserPaused = false
        val state = mediaPlayer.playerState
        if (state == org.videolan.libvlc.interfaces.IMedia.State.Ended ||
            state == org.videolan.libvlc.interfaces.IMedia.State.Stopped ||
            state == org.videolan.libvlc.interfaces.IMedia.State.Error ||
            !mediaPlayer.hasMedia()) {
            currentStream?.let { load(it) }
            return
        }
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.play()
        }
    }

    override fun pause() {
        isUserPaused = true
        reconnectJob?.cancel()
        reconnectJob = null
        try {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
            }
        } catch (e: Exception) {
            Logger.w("Error pausing VLC: ${e.message}", TAG)
        }
    }

    override fun seekTo(positionMs: Long) {
        val duration = mediaPlayer.length
        if (duration <= 0L || !mediaPlayer.isSeekable) {
            return
        }

        val clamped = positionMs.coerceIn(0L, duration)
        pendingSeekPosition = clamped
        _state.value = _state.value.copy(positionMs = clamped)

        // Debounce rapid seek requests by 150ms to prevent buffer exhaustion and MediaCodec surface disconnection
        // when scrubbing the timeline or repeatedly tapping skip forward/backward
        seekJob?.cancel()
        seekJob = scope.launch {
            delay(150L)
            val target = pendingSeekPosition ?: clamped
            pendingSeekPosition = null
            try {
                // fast = false: flushes the decoder pipeline and resynchronizes the hardware video
                // decoder with the audio clock, preventing frozen video frames while audio continues.
                mediaPlayer.setTime(target, false)
            } catch (e: Exception) {
                Logger.w("Failed to seek VLC player: ${e.message}", TAG)
            }
        }
    }

    override fun seekForward(ms: Long) {
        val basePos = pendingSeekPosition ?: _state.value.positionMs
        seekTo(basePos + ms)
    }

    override fun seekBackward(ms: Long) {
        val basePos = pendingSeekPosition ?: _state.value.positionMs
        seekTo((basePos - ms).coerceAtLeast(0L))
    }

    override fun setVolume(volume: Float) {
        val vlcVol = (volume.coerceIn(0f, 1f) * 100).toInt()
        mediaPlayer.volume = vlcVol
    }

    override fun setPlaybackSpeed(speed: Float) {
        mediaPlayer.rate = speed
        _state.value = _state.value.copy(playbackSpeed = speed)
    }

    override fun setQuality(quality: QualityOption) {
        _state.value = _state.value.copy(currentQuality = quality)
    }

    override fun setAudioTrack(track: AudioTrack) {
        val trackId = track.id.toIntOrNull() ?: return
        mediaPlayer.audioTrack = trackId
        _state.value = _state.value.copy(
            selectedAudio = track,
            audioTracks = _state.value.audioTracks.map { it.copy(isSelected = it.id == track.id) }
        )
    }

    override fun setSubtitleTrack(track: SubtitleTrack?) {
        if (track == null) {
            mediaPlayer.spuTrack = -1
            _state.value = _state.value.copy(
                selectedSubtitle = null,
                subtitleTracks = _state.value.subtitleTracks.map { it.copy(isSelected = false) }
            )
        } else {
            val trackId = track.id.toIntOrNull()
            if (trackId != null && trackId != -1) {
                mediaPlayer.spuTrack = trackId
            } else if (!track.url.isNullOrBlank()) {
                try {
                    mediaPlayer.addSlave(
                        IMedia.Slave.Type.Subtitle,
                        Uri.parse(track.url),
                        true
                    )
                } catch (e: Throwable) {
                    Logger.w("Failed to add slave subtitle: ${e.message}", TAG)
                }
            }
            _state.value = _state.value.copy(
                selectedSubtitle = track,
                subtitleTracks = _state.value.subtitleTracks.map { it.copy(isSelected = it.id == track.id) }
            )
        }
    }

    override fun release() {
        isUserPaused = true
        reconnectJob?.cancel()
        reconnectJob = null
        seekJob?.cancel()
        seekJob = null
        pendingSeekPosition = null
        stopProgressTracking()
        scope.cancel()

        try {
            if (mediaPlayer.vlcVout.areViewsAttached()) {
                mediaPlayer.detachViews()
            }
        } catch (e: Exception) {
            Logger.w("Error detaching views: ${e.message}", TAG)
        }
        currentVideoLayout = null

        // Stop and release native LibVLC asynchronously on Dispatchers.IO.
        // libvlc_media_player_stop() & release() wait for native demuxer and TLS threads to join.
        // When streams timeout or disconnect, calling this on the UI thread blocks for up to 25.5s (Davey/ANR).
        // Executing in background ensures instant UI teardown with zero ANR stalls.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
                libVLC.release()
            } catch (e: Exception) {
                Logger.w("Error asynchronously releasing VLC player: ${e.message}", TAG)
            }
        }
    }

    @Composable
    override fun Surface(
        modifier: Modifier,
        isFullScreen: Boolean,
        resizeMode: Int
    ) {
        DisposableEffect(Unit) {
            onDispose {
                try {
                    if (mediaPlayer.vlcVout.areViewsAttached()) {
                        mediaPlayer.detachViews()
                    }
                    currentVideoLayout = null
                } catch (e: Exception) {
                    Logger.w("Error detaching views on dispose: ${e.message}", TAG)
                }
            }
        }

        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    keepScreenOn = true
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    val videoLayout = VLCVideoLayout(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    addView(videoLayout)
                    currentVideoLayout = videoLayout

                    // Delay attachViews() until the FrameLayout has been measured by the Compose layout
                    // pass and has a non-zero width/height. Calling attachViews() on a 0×0 surface causes
                    // "Invalid surface size" errors in LibVLC, which triggers SurfaceView recreation and
                    // adds 3-6 seconds of startup delay before playback begins.
                    viewTreeObserver.addOnGlobalLayoutListener(
                        object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                if (width > 0 && height > 0) {
                                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                                    if (!mediaPlayer.vlcVout.areViewsAttached()) {
                                        // 3rd arg is subtitles = true: enables native surface_subtitles ViewStub
                                        // 4th arg is useTextureView = false: enables native SurfaceView hardware overlay
                                        // Bypasses OpenGLRenderer pipeline, eliminating Davey stalls and UI thread ANRs
                                        mediaPlayer.attachViews(videoLayout, null, true, false)
                                    }
                                }
                            }
                        }
                    )
                }
            },
            update = { frameLayout ->
                // Do NOT detach or reattach views on every Compose frame!
                // Re-attaching views on Compose recompositions freezes the UI thread.
                val videoLayout = frameLayout.getChildAt(0) as? VLCVideoLayout
                if (videoLayout != null && !mediaPlayer.vlcVout.areViewsAttached() &&
                    frameLayout.width > 0 && frameLayout.height > 0) {
                    currentVideoLayout = videoLayout
                    mediaPlayer.attachViews(videoLayout, null, true, false)
                }
                // Handle video scaling without re-attaching views
                when (resizeMode) {
                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL -> {
                        mediaPlayer.videoScale = MediaPlayer.ScaleType.SURFACE_FILL
                    }
                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> {
                        mediaPlayer.videoScale = MediaPlayer.ScaleType.SURFACE_FIT_SCREEN
                    }
                    else -> {
                        mediaPlayer.videoScale = MediaPlayer.ScaleType.SURFACE_BEST_FIT
                    }
                }
            }
        )
    }
}
