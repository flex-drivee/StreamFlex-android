@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.streamflex.player.media3

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer

import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.streamflex.domain.models.StreamLink
import com.streamflex.player.core.PlayerError
import com.streamflex.player.core.PlayerEvent
import com.streamflex.player.core.PlayerState
import com.streamflex.player.core.StreamPlayer
import com.streamflex.player.quality.QualityOption
import com.streamflex.player.tracks.AudioTrack
import com.streamflex.player.tracks.SubtitleTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class Media3Player(
    private val context: Context,
    private val decoderMode: com.streamflex.player.core.DecoderMode = com.streamflex.player.core.DecoderMode.AUTO
) : StreamPlayer {

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PlayerEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<PlayerEvent> = _events.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    
    private val trackSelector = DefaultTrackSelector(context)
    
    private val renderersFactory = decoderMode.createRenderersFactory(context)

    internal val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setRenderersFactory(renderersFactory)
        .setTrackSelector(trackSelector)
        .build()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                updateState()
                when (playbackState) {
                    Player.STATE_ENDED -> {
                        stopProgressTracking()
                        emitEvent(PlayerEvent.PlaybackEnded)
                    }
                    Player.STATE_READY -> {
                        if (exoPlayer.playWhenReady) {
                            startProgressTracking()
                            emitEvent(PlayerEvent.PlaybackStarted)
                        } else {
                            stopProgressTracking()
                        }
                    }
                    else -> stopProgressTracking()
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                updateState()
                if (playWhenReady && exoPlayer.playbackState == Player.STATE_READY) {
                    startProgressTracking()
                    emitEvent(PlayerEvent.PlaybackStarted)
                } else {
                    stopProgressTracking()
                    if (!playWhenReady) emitEvent(PlayerEvent.PlaybackPaused)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                stopProgressTracking()
                
                val playerError = when (error.errorCode) {
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> PlayerError.Timeout()
                    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                    PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
                    PlaybackException.ERROR_CODE_DECODING_FAILED -> PlayerError.Decoder()
                    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> PlayerError.UnsupportedCodec()
                    PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
                    PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
                    PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> PlayerError.InvalidSource()
                    else -> {
                        val httpException = error.cause as? androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
                        if (httpException != null) {
                            PlayerError.Http(httpException.responseCode)
                        } else {
                            PlayerError.Unknown(error.message ?: "Unknown error", error)
                        }
                    }
                }
                
                _state.value = _state.value.copy(error = playerError)
                emitEvent(PlayerEvent.Error(playerError))
            }
            
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                emitEvent(PlayerEvent.VideoSizeChanged(videoSize.width, videoSize.height))
            }
            
            override fun onTracksChanged(tracks: Tracks) {
                extractTracks(tracks)
            }
        })
    }

    private fun extractTracks(tracks: Tracks) {
        val qualities = mutableListOf<QualityOption>()
        val audios = mutableListOf<AudioTrack>()
        val subtitles = mutableListOf<SubtitleTrack>()
        
        val autoOption = QualityOption(id = "auto", name = "Auto", resolution = Int.MAX_VALUE, isAuto = true)
        qualities.add(autoOption)
        
        var currentQuality = autoOption
        var currentAudio: AudioTrack? = null
        var currentSubtitle: SubtitleTrack? = null
        
        for (group in tracks.groups) {
            when (group.type) {
                C.TRACK_TYPE_VIDEO -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        if (group.isTrackSupported(i) && format.height > 0) {
                            val option = QualityOption(
                                id = "${group.mediaTrackGroup.hashCode()}_$i",
                                name = "${format.height}p",
                                resolution = format.height,
                                bitrate = format.bitrate,
                                mimeType = format.sampleMimeType,
                                codecs = format.codecs
                            )
                            qualities.add(option)
                            if (group.isTrackSelected(i) && !trackSelector.parameters.overrides.isEmpty()) {
                                currentQuality = option
                            }
                        }
                    }
                }
                C.TRACK_TYPE_AUDIO -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        if (group.isTrackSupported(i)) {
                            val lang = format.language?.takeIf { it != "und" }
                            val readableName = lang?.let { java.util.Locale.forLanguageTag(it).displayLanguage }
                            val cleanLabel = format.label?.takeIf { !it.contains(".tv", true) && !it.contains(".com", true) && !it.contains("hdhub4u", true) }
                            
                            val finalLabel = readableName ?: cleanLabel ?: lang?.uppercase() ?: "Audio ${i + 1}"
                            
                            val option = AudioTrack(
                                id = "${group.mediaTrackGroup.hashCode()}_$i",
                                language = format.language,
                                label = finalLabel
                            )
                            audios.add(option)
                            if (group.isTrackSelected(i)) currentAudio = option
                        }
                    }
                }
                C.TRACK_TYPE_TEXT -> {
                    for (i in 0 until group.length) {
                        val format = group.getTrackFormat(i)
                        if (group.isTrackSupported(i)) {
                            val lang = format.language?.takeIf { it != "und" }
                            val readableName = lang?.let { java.util.Locale.forLanguageTag(it).displayLanguage }
                            val cleanLabel = format.label?.takeIf { !it.contains(".tv", true) && !it.contains(".com", true) && !it.contains("hdhub4u", true) }
                            
                            val finalLabel = readableName ?: cleanLabel ?: lang?.uppercase() ?: "Subtitle ${i + 1}"
                            
                            val option = SubtitleTrack(
                                id = "${group.mediaTrackGroup.hashCode()}_$i",
                                language = format.language,
                                label = finalLabel,
                                mimeType = format.sampleMimeType,
                                isEmbedded = true
                            )
                            subtitles.add(option)
                            if (group.isTrackSelected(i)) currentSubtitle = option
                        }
                    }
                }
            }
        }
        
        val distinctQualities = qualities.groupBy { it.resolution }.map { entry ->
            entry.value.maxByOrNull { it.bitrate } ?: entry.value.first()
        }.sortedByDescending { it.resolution }
        
        _state.value = _state.value.copy(
            availableQualities = distinctQualities,
            currentQuality = currentQuality,
            audioTracks = audios.distinctBy { it.id },
            selectedAudio = currentAudio,
            subtitleTracks = subtitles.distinctBy { it.id },
            selectedSubtitle = currentSubtitle
        )
    }

    private fun startProgressTracking() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive) {
                updateState()
                delay(500L) 
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun emitEvent(event: PlayerEvent) {
        scope.launch { _events.emit(event) }
    }

    private fun updateState() {
        val currentPos = exoPlayer.currentPosition
        val dur = exoPlayer.duration.takeIf { it > 0 } ?: 0L
        val buf = exoPlayer.bufferedPosition

        _state.value = _state.value.copy(
            isPlaying = exoPlayer.isPlaying,
            isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING,
            positionMs = currentPos,
            durationMs = dur,
            bufferedPositionMs = buf
        )
    }

    override fun load(stream: StreamLink) {
        _state.value = PlayerState() 
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .clearOverrides()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false) // Enable subtitles so they can be probed and listed
        )

        // 1. Resolve User-Agent and default streaming headers
        val userAgent = stream.headers.entries.find { it.key.equals("User-Agent", ignoreCase = true) }?.value
            ?: com.streamflex.core.constants.Constants.DEFAULT_USER_AGENT

        val requestHeaders = mutableMapOf<String, String>()
        requestHeaders["User-Agent"] = userAgent
        requestHeaders["Accept"] = "*/*"

        val lowerUrl = stream.url.lowercase()
        val isDirectCdn = lowerUrl.contains("googleusercontent.com") ||
                          lowerUrl.contains("googlevideo.com") ||
                          lowerUrl.contains("pixeldrain.com") ||
                          lowerUrl.contains("buzzheavier.com") ||
                          lowerUrl.contains("publit.io") ||
                          lowerUrl.contains(".guru/") ||
                          lowerUrl.contains(".buzz/") ||
                          lowerUrl.contains("mega.nz")

        // Attach Referer only if not a direct CDN stream that rejects hotlinking
        if (!isDirectCdn) {
            val referer = stream.referer ?: stream.headers.entries.find { it.key.equals("Referer", ignoreCase = true) }?.value
            if (!referer.isNullOrBlank()) {
                requestHeaders["Referer"] = referer
            }
        }

        // Attach all stream headers (skipping Referer for direct CDNs)
        stream.headers.forEach { (k, v) ->
            if (v.isNotBlank()) {
                if (isDirectCdn && k.equals("Referer", ignoreCase = true)) {
                    // omit referer
                } else if (!k.equals("Connection", ignoreCase = true)) {
                    requestHeaders[k] = v
                }
            }
        }

        // Attach cookies if present
        if (stream.cookies.isNotEmpty()) {
            val cookieHeader = stream.cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }
            if (cookieHeader.isNotBlank()) {
                requestHeaders["Cookie"] = cookieHeader
            }
        }

        // 2. High-performance HTTP Data Source with redirect, fast failover timeout, and custom headers
        val isNetMirror = stream.name.contains("NetMirror", ignoreCase = true)
        val okHttpClient = if (isNetMirror) {
            com.streamflex.core.network.StreamFlexHttpClient.okHttpClient.newBuilder()
                .addInterceptor { chain ->
                    val request = chain.request()
                    if (request.url.toString().contains(".m3u8")) {
                        val newRequest = request.newBuilder().header("Cookie", "hd=on").build()
                        val response = chain.proceed(newRequest)
                        val body = response.body
                        if (body != null && response.isSuccessful) {
                            val bodyString = body.string()
                            
                            // Log the original M3U8
                            com.streamflex.core.utils.StreamLogger.debug("Media3Player", "Original M3U8:\n$bodyString")
                            
                            var rewritten = bodyString
                            if (!rewritten.contains("CODECS=")) {
                                rewritten = rewritten.replace("#EXT-X-STREAM-INF:", "#EXT-X-STREAM-INF:CODECS=\"avc1.42c01e,mp4a.40.2\",")
                            } else if (!rewritten.contains("mp4a")) {
                                rewritten = rewritten.replace(Regex("""CODECS="([^,"]+)""""), """CODECS="$1,mp4a.40.2"""")
                            }
                            
                            val newBody = okhttp3.ResponseBody.create(body.contentType(), rewritten)
                            return@addInterceptor response.newBuilder().body(newBody).build()
                        }
                        response
                    } else {
                        chain.proceed(request)
                    }
                }
                .build()
        } else {
            com.streamflex.core.network.StreamFlexHttpClient.okHttpClient
        }

        val httpDataSourceFactory = androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(userAgent)
            .setDefaultRequestProperties(requestHeaders)

        // 3. Wrap in DefaultDataSource.Factory to support file://, content://, rawresource:// as well as http/https
        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)

        // 4. Build MediaSourceFactory
        val mediaSourceFactory = DefaultMediaSourceFactory(context)
            .setDataSourceFactory(dataSourceFactory)

        // 5. Detect container / MIME type for links without standard file extensions (e.g. Pixeldrain, BuzzServer)
        val mimeType = when (stream.contentType) {
            com.streamflex.core.network.detector.ContentType.M3U8,
            com.streamflex.core.network.detector.ContentType.HLS -> androidx.media3.common.MimeTypes.APPLICATION_M3U8
            com.streamflex.core.network.detector.ContentType.DASH -> androidx.media3.common.MimeTypes.APPLICATION_MPD
            com.streamflex.core.network.detector.ContentType.VIDEO -> {
                val lower = stream.url.lowercase()
                when {
                    lower.contains(".m3u8") -> androidx.media3.common.MimeTypes.APPLICATION_M3U8
                    lower.contains(".mpd") -> androidx.media3.common.MimeTypes.APPLICATION_MPD
                    lower.contains(".mp4") -> androidx.media3.common.MimeTypes.APPLICATION_MP4
                    lower.contains(".mkv") -> androidx.media3.common.MimeTypes.APPLICATION_MATROSKA
                    else -> null
                }
            }
            else -> {
                val lower = stream.url.lowercase()
                when {
                    lower.contains(".m3u8") -> androidx.media3.common.MimeTypes.APPLICATION_M3U8
                    lower.contains(".mpd") -> androidx.media3.common.MimeTypes.APPLICATION_MPD
                    lower.contains(".mp4") -> androidx.media3.common.MimeTypes.APPLICATION_MP4
                    lower.contains(".mkv") -> androidx.media3.common.MimeTypes.APPLICATION_MATROSKA
                    else -> null
                }
            }
        }

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(stream.url)

        if (mimeType != null) {
            mediaItemBuilder.setMimeType(mimeType)
        }

        // 6. Attach external subtitles if present
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(context, "Engine Loaded Subtitles: ${stream.subtitles.size}", android.widget.Toast.LENGTH_LONG).show()
        }
        com.streamflex.core.utils.StreamLogger.error("SUBTITLE_DEBUG", "Starting player with URL: ${stream.url}")
        com.streamflex.core.utils.StreamLogger.error("SUBTITLE_DEBUG", "Subtitles count: ${stream.subtitles.size}")
        for (sub in stream.subtitles) {
            com.streamflex.core.utils.StreamLogger.error("SUBTITLE_DEBUG", "SUB: ${sub.label} -> ${sub.url}")
        }
        if (stream.subtitles.isNotEmpty()) {
            val subtitleConfigs = stream.subtitles.map { sub ->
                val subMime = if (sub.url.endsWith(".vtt", ignoreCase = true)) {
                    androidx.media3.common.MimeTypes.TEXT_VTT
                } else {
                    androidx.media3.common.MimeTypes.APPLICATION_SUBRIP
                }
                MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(sub.url))
                    .setMimeType(subMime)
                    .setLanguage(sub.language)
                    .setLabel(sub.label)
                    .setSelectionFlags(androidx.media3.common.C.SELECTION_FLAG_DEFAULT)
                    .build()
            }
            mediaItemBuilder.setSubtitleConfigurations(subtitleConfigs)
        }

        val mediaItem = mediaItemBuilder.build()
        val source = if (mimeType == androidx.media3.common.MimeTypes.APPLICATION_M3U8) {
            androidx.media3.exoplayer.hls.HlsMediaSource.Factory(dataSourceFactory)
                .setAllowChunklessPreparation(true)
                .createMediaSource(mediaItem)
        } else {
            mediaSourceFactory.createMediaSource(mediaItem)
        }

        exoPlayer.setMediaSource(source)
        exoPlayer.prepare()
    }

    override fun play() { exoPlayer.play() }
    override fun pause() { exoPlayer.pause() }
    override fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        updateState()
    }
    override fun seekForward(ms: Long) { seekTo(exoPlayer.currentPosition + ms) }
    override fun seekBackward(ms: Long) { seekTo((exoPlayer.currentPosition - ms).coerceAtLeast(0)) }
    override fun release() {
        stopProgressTracking()
        scope.cancel()
        exoPlayer.release()
    }

    override fun setQuality(quality: QualityOption) {
        if (quality.isAuto) {
            trackSelector.setParameters(
                trackSelector.buildUponParameters().clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            )
            _state.value = _state.value.copy(currentQuality = quality)
            return
        }
        applyTrackOverride(C.TRACK_TYPE_VIDEO, quality.id)
    }

    override fun setAudioTrack(track: AudioTrack) {
        applyTrackOverride(C.TRACK_TYPE_AUDIO, track.id)
    }

    override fun setSubtitleTrack(track: SubtitleTrack?) {
        if (track == null) {
            trackSelector.setParameters(
                trackSelector.buildUponParameters()
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            )
            _state.value = _state.value.copy(selectedSubtitle = null)
        } else {
            applyTrackOverride(C.TRACK_TYPE_TEXT, track.id)
            // Ensure track type is enabled
            trackSelector.setParameters(
                trackSelector.buildUponParameters().setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            )
        }
    }

    private fun applyTrackOverride(trackType: @C.TrackType Int, id: String) {
        for (group in exoPlayer.currentTracks.groups) {
            if (group.type == trackType) {
                for (i in 0 until group.length) {
                    if ("${group.mediaTrackGroup.hashCode()}_$i" == id) {
                        val override = TrackSelectionOverride(group.mediaTrackGroup, i)
                        trackSelector.setParameters(
                            trackSelector.buildUponParameters()
                                .clearOverridesOfType(trackType)
                                .addOverride(override)
                        )
                        return
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    override fun Surface(modifier: androidx.compose.ui.Modifier, isFullScreen: Boolean) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = if (isFullScreen) androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM else androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                }
            },
            update = { view ->
                view.resizeMode = if (isFullScreen) androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM else androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
            },
            modifier = modifier
        )
    }
}
