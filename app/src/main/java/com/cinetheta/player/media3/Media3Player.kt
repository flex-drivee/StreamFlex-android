@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.cinetheta.player.media3

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaSession
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer

import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.cinetheta.domain.models.StreamLink
import com.cinetheta.player.core.PlayerError
import com.cinetheta.player.core.PlayerEvent
import com.cinetheta.player.core.PlayerState
import com.cinetheta.player.core.StreamPlayer
import com.cinetheta.player.quality.QualityOption
import com.cinetheta.player.tracks.AudioTrack
import com.cinetheta.player.tracks.SubtitleTrack
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class Media3Player(
    private val context: Context,
    private val decoderMode: com.cinetheta.player.core.DecoderMode = com.cinetheta.player.core.DecoderMode.AUTO
) : StreamPlayer {

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PlayerEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<PlayerEvent> = _events.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var seekJob: Job? = null
    private var pendingSeekPosition: Long? = null
    
    private val trackSelector = DefaultTrackSelector(context)
    
    private val renderersFactory = decoderMode.createRenderersFactory(context)

    private val defaultExtractorsFactory = androidx.media3.extractor.DefaultExtractorsFactory()
        .setConstantBitrateSeekingEnabled(true)
        .setMatroskaExtractorFlags(androidx.media3.extractor.mkv.MatroskaExtractor.FLAG_DISABLE_SEEK_FOR_CUES)

    private val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            /* minBufferMs = */ 15_000,
            /* maxBufferMs = */ 60_000,
            /* bufferForPlaybackMs = */ 1_000,
            /* bufferForPlaybackAfterRebufferMs = */ 1_500
        )
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()

    private val livePlaybackSpeedControl = androidx.media3.exoplayer.DefaultLivePlaybackSpeedControl.Builder()
        .setFallbackMaxPlaybackSpeed(1.20f)
        .setFallbackMinPlaybackSpeed(0.95f)
        .build()

    internal val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setRenderersFactory(renderersFactory)
        .setTrackSelector(trackSelector)
        .setLoadControl(loadControl)
        .setLivePlaybackSpeedControl(livePlaybackSpeedControl)
        .setSeekParameters(androidx.media3.exoplayer.SeekParameters.CLOSEST_SYNC)
        .setMediaSourceFactory(DefaultMediaSourceFactory(context, defaultExtractorsFactory))
        .build()

    private val mediaSession: MediaSession = MediaSession.Builder(context, exoPlayer).build()
    private val prefs by lazy { context.getSharedPreferences("cinetheta_settings", android.content.Context.MODE_PRIVATE) }

    init {
        val enableSubtitles = prefs.getBoolean("enable_subtitles", true)
        val defaultQuality = prefs.getString("player_video_quality", "Auto") ?: "Auto"
        
        var builder = trackSelector.buildUponParameters()
        if (!enableSubtitles) {
            builder = builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        }
        if (defaultQuality != "Auto") {
            val height = defaultQuality.replace("p", "").toIntOrNull()
            if (height != null) {
                builder = builder.setMaxVideoSize(Int.MAX_VALUE, height)
            }
        }
        val preferredAudioLang = prefs.getString("preferred_audio_language", null)
        if (!preferredAudioLang.isNullOrBlank()) {
            builder = builder.setPreferredAudioLanguage(preferredAudioLang)
        }
        trackSelector.setParameters(builder)

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
                // --- LIVE STREAM BEHIND-WINDOW AUTO RECOVERY ---
                // If network jitter causes live playback to fall behind the sliding window,
                // ExoPlayer throws BehindLiveWindowException.
                // Standard Media3 recovery: jump back to live edge and re-prepare seamlessly.
                if (error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW ||
                    error.cause is androidx.media3.exoplayer.source.BehindLiveWindowException) {
                    com.cinetheta.core.utils.StreamLogger.warn("Media3Player", "Behind live window detected. Re-seeking to live edge...")
                    exoPlayer.seekToDefaultPosition()
                    exoPlayer.prepare()
                    return
                }

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
                            val cleanLabel = format.label?.takeIf { it.isNotBlank() && !it.contains(".tv", true) && !it.contains(".com", true) && !it.contains("hdhub4u", true) }
                            
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
                        
                        val lang = format.language?.takeIf { it != "und" }
                        val readableName = lang?.let { java.util.Locale.forLanguageTag(it).displayLanguage }
                        val cleanLabel = format.label?.takeIf { it.isNotBlank() && !it.contains(".tv", true) && !it.contains(".com", true) && !it.contains("hdhub4u", true) }
                        
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
        
        val distinctQualities = qualities.groupBy { it.resolution }.map { entry ->
            entry.value.maxByOrNull { it.bitrate } ?: entry.value.first()
        }.sortedByDescending { it.resolution }

        val distinctAudios = audios.distinctBy { it.id }
        val preferredLang = prefs.getString("preferred_audio_language", null)
        if (!preferredLang.isNullOrBlank() && distinctAudios.isNotEmpty()) {
            val matched = findMatchingAudio(distinctAudios, preferredLang)
            if (matched != null && (currentAudio == null || matched.id != currentAudio.id)) {
                currentAudio = matched
                applyTrackOverride(C.TRACK_TYPE_AUDIO, matched.id)
            }
        }

        _state.value = _state.value.copy(
            availableQualities = distinctQualities,
            currentQuality = currentQuality,
            audioTracks = distinctAudios,
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
        val currentPos = pendingSeekPosition ?: exoPlayer.currentPosition
        val dur = exoPlayer.duration.takeIf { it > 0 } ?: 0L
        val buf = exoPlayer.bufferedPosition

        _state.value = _state.value.copy(
            isPlaying = exoPlayer.isPlaying,
            isBuffering = exoPlayer.playbackState == Player.STATE_BUFFERING,
            positionMs = currentPos,
            durationMs = dur,
            bufferedPositionMs = buf,
            playbackSpeed = exoPlayer.playbackParameters.speed
        )
    }

    override fun load(stream: StreamLink) {
        seekJob?.cancel()
        seekJob = null
        pendingSeekPosition = null
        _state.value = PlayerState(isBuffering = true) 
        val prefs = context.getSharedPreferences("cinetheta_settings", android.content.Context.MODE_PRIVATE)
        val enableSubtitles = prefs.getBoolean("enable_subtitles", true)
        val defaultQuality = prefs.getString("player_video_quality", "Auto") ?: "Auto"
        
        var builder = trackSelector.buildUponParameters().clearOverrides()
        
        if (!enableSubtitles) {
            builder = builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        } else {
            builder = builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        }
        
        if (defaultQuality != "Auto") {
            val height = defaultQuality.replace("p", "").toIntOrNull()
            if (height != null) {
                builder = builder.setMaxVideoSize(Int.MAX_VALUE, height)
            }
        } else {
            builder = builder.clearVideoSizeConstraints()
        }
        
        trackSelector.setParameters(builder)

        // 1. Resolve User-Agent and default streaming headers
        val userAgent = stream.headers.entries.find { it.key.equals("User-Agent", ignoreCase = true) }?.value
            ?: com.cinetheta.core.constants.Constants.DEFAULT_USER_AGENT

        val requestHeaders = mutableMapOf<String, String>()
        requestHeaders["User-Agent"] = userAgent
        requestHeaders["Accept"] = "*/*"

        val lowerUrl = stream.url.lowercase()
        val isMovieBoxWebCdn = lowerUrl.contains("sbcdnw.hakunaymatata.com") ||
                               lowerUrl.contains("bcdnxw.hakunaymatata.com") ||
                               lowerUrl.contains("movie-box.co") ||
                               lowerUrl.contains("movieboxhd.net")

        val isMovieBoxDirectCdn = lowerUrl.contains("bcdn.hakunaymatata.com") ||
                                  lowerUrl.contains("sbcdn.hakunaymatata.com") ||
                                  lowerUrl.contains("macdn.aoneroom.com")

        val isDirectCdn = lowerUrl.contains("googleusercontent.com") ||
                          lowerUrl.contains("googlevideo.com") ||
                          lowerUrl.contains("pixeldrain.com") ||
                          lowerUrl.contains("buzzheavier.com") ||
                          lowerUrl.contains("publit.io") ||
                          lowerUrl.contains(".guru/") ||
                          lowerUrl.contains(".buzz/") ||
                          lowerUrl.contains("mega.nz") ||
                          isMovieBoxDirectCdn

        // Attach Referer only if not a direct CDN stream that rejects hotlinking
        if (!isDirectCdn) {
            val referer = stream.referer ?: stream.headers.entries.find { it.key.equals("Referer", ignoreCase = true) }?.value
            if (!referer.isNullOrBlank()) {
                requestHeaders["Referer"] = referer
            }
        }

        // Attach all stream headers (skipping Referer and Origin for direct CDNs)
        stream.headers.forEach { (k, v) ->
            if (v.isNotBlank()) {
                if (isDirectCdn && (k.equals("Referer", ignoreCase = true) || k.equals("Origin", ignoreCase = true))) {
                    // omit referer/origin for direct CDNs that reject hotlinking
                } else if (!k.equals("Connection", ignoreCase = true)) {
                    requestHeaders[k] = v
                }
            }
        }

        // Safeguard: explicitly ensure direct CDNs do not have Referer or Origin attached
        if (isMovieBoxDirectCdn) {
            requestHeaders.remove("Referer")
            requestHeaders.remove("Origin")
        }

        // MovieBox Web CDN safeguard: requires Referer: https://movie-box.co to prevent HTTP 429
        if (isMovieBoxWebCdn && requestHeaders["Referer"].isNullOrBlank()) {
            requestHeaders["Referer"] = com.cinetheta.providers.moviebox.MovieBoxConfig.STREAM_REFERER
        }

        // MovieBox Direct CDN safeguard: bcdn.hakunaymatata.com returns HTTP 428 if browser UA (Mozilla/5.0) is sent
        if (isMovieBoxDirectCdn && (requestHeaders["User-Agent"]?.startsWith("Mozilla/5.0") == true || userAgent.startsWith("Mozilla/5.0"))) {
            requestHeaders["User-Agent"] = com.cinetheta.providers.moviebox.MovieBoxCrypto.userAgent
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
            com.cinetheta.core.network.CineThetaHttpClient.streamingOkHttpClient.newBuilder()
                .addInterceptor { chain ->
                    val request = chain.request()
                    if (request.url.toString().contains(".m3u8")) {
                        // Do NOT overwrite the Cookie header here!
                        // requestHeaders already contains the correct t_hash_t and ott cookies.
                        val response = chain.proceed(request)
                        val body = response.body
                        if (body != null && response.isSuccessful) {
                            val bodyString = body.string()
                            
                            // Log the original M3U8
                            com.cinetheta.core.utils.StreamLogger.debug("Media3Player", "Original M3U8:\n$bodyString")
                            
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
            com.cinetheta.core.network.CineThetaHttpClient.streamingOkHttpClient
        }

        val effectiveUserAgent = requestHeaders["User-Agent"] ?: userAgent
        val httpDataSourceFactory = androidx.media3.datasource.okhttp.OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent(effectiveUserAgent)
            .setDefaultRequestProperties(requestHeaders)

        val isContentUri = stream.url.startsWith("content://")
        val isLocalFile = stream.url.startsWith("/") || stream.url.startsWith("file:")

        // 3. Wrap in DefaultDataSource.Factory to support file://, content://, rawresource:// as well as http/https
        val dataSourceFactory = androidx.media3.datasource.DefaultDataSource.Factory(context, httpDataSourceFactory)

        // 4. Build MediaSourceFactory with MatroskaExtractor.FLAG_DISABLE_SEEK_FOR_CUES
        // Critical for progressive MKV HTTP streams (e.g. Google Drive/Usercontent, Driveseed, GDFlix).
        // By default, MatroskaExtractor seeks to the end of multi-gigabyte MKV files to parse Cues.
        // If the HTTP server does not support HTTP Range requests (returns HTTP 200 instead of 206),
        // OkHttpDataSource attempts to skip/download the entire file sequentially (e.g. 8+ GB),
        // causing 1.6-2 MB/s network saturation and indefinite buffering without playback starting.
        val extractorsFactory = androidx.media3.extractor.DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)
            .setMatroskaExtractorFlags(
                if (!isLocalFile && !isContentUri) {
                    androidx.media3.extractor.mkv.MatroskaExtractor.FLAG_DISABLE_SEEK_FOR_CUES
                } else {
                    0
                }
            )

        val mediaSourceFactory = DefaultMediaSourceFactory(context, extractorsFactory)
            .setDataSourceFactory(dataSourceFactory)

        // 5. Detect container / MIME type for links without standard file extensions (e.g. Pixeldrain, BuzzServer, Live IPTV)
        val mimeType = when (stream.contentType) {
            com.cinetheta.core.network.detector.ContentType.M3U8,
            com.cinetheta.core.network.detector.ContentType.HLS -> androidx.media3.common.MimeTypes.APPLICATION_M3U8
            com.cinetheta.core.network.detector.ContentType.DASH -> androidx.media3.common.MimeTypes.APPLICATION_MPD
            com.cinetheta.core.network.detector.ContentType.VIDEO -> {
                val lower = stream.url.lowercase()
                when {
                    lower.contains(".m3u8") || lower.contains(".m3u") -> androidx.media3.common.MimeTypes.APPLICATION_M3U8
                    lower.contains(".mpd") -> androidx.media3.common.MimeTypes.APPLICATION_MPD
                    lower.contains(".mp4") -> androidx.media3.common.MimeTypes.APPLICATION_MP4
                    lower.contains(".mkv") -> androidx.media3.common.MimeTypes.APPLICATION_MATROSKA
                    lower.contains(".ts") -> androidx.media3.common.MimeTypes.VIDEO_MP2T
                    else -> null
                }
            }
            else -> {
                val lower = stream.url.lowercase()
                when {
                    lower.contains(".m3u8") || lower.contains(".m3u") -> androidx.media3.common.MimeTypes.APPLICATION_M3U8
                    lower.contains(".mpd") -> androidx.media3.common.MimeTypes.APPLICATION_MPD
                    lower.contains(".mp4") -> androidx.media3.common.MimeTypes.APPLICATION_MP4
                    lower.contains(".mkv") -> androidx.media3.common.MimeTypes.APPLICATION_MATROSKA
                    lower.contains(".ts") -> androidx.media3.common.MimeTypes.VIDEO_MP2T
                    else -> null
                }
            }
        }

        val mediaUri = when {
            isContentUri -> android.net.Uri.parse(stream.url)   // content:// must be parsed as-is, NOT via fromFile
            isLocalFile -> {
                val cleanPath = stream.url.removePrefix("file://")
                android.net.Uri.fromFile(java.io.File(cleanPath))
            }
            else -> android.net.Uri.parse(stream.url)
        }

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(mediaUri)

        // For local files, NEVER force a MIME type!
        // Allow ExoPlayer's DefaultExtractorsFactory to sniff and auto-detect whether the file is MP4, TS, MKV, etc.
        // For remote streams, attach mimeType if detected.
        if (!isLocalFile && !isContentUri && mimeType != null) {
            mediaItemBuilder.setMimeType(mimeType)
        }

        // Live HLS streams: configure a safe 8-second buffer cushion behind the live edge
        // and allow slight playback speed adjustment (0.95x - 1.20x) to avoid falling behind the live window.
        val isLiveOrAdaptive = stream.adaptive || mimeType == androidx.media3.common.MimeTypes.APPLICATION_M3U8
        if (isLiveOrAdaptive && !isLocalFile && !isContentUri) {
            val liveConfig = MediaItem.LiveConfiguration.Builder()
                .setMaxPlaybackSpeed(1.20f)
                .setMinPlaybackSpeed(0.95f)
                .setTargetOffsetMs(8_000L)
                .setMinOffsetMs(2_000L)
                .setMaxOffsetMs(30_000L)
                .build()
            mediaItemBuilder.setLiveConfiguration(liveConfig)
        }

        // Reset track selector overrides from previous stream/server
        trackSelector.setParameters(
            trackSelector.buildUponParameters()
                .clearOverrides()
        )
        _state.value = _state.value.copy(
            subtitleTracks = emptyList(),
            selectedSubtitle = null,
            audioTracks = emptyList(),
            selectedAudio = null,
            availableQualities = emptyList()
        )

        // 6. Attach external subtitles if present
        if (stream.subtitles.isNotEmpty()) {
            val subtitleConfigs = stream.subtitles.map { sub ->
                val subCleanUrl = sub.url.substringBefore('?')
                val subMime = if (subCleanUrl.endsWith(".vtt", ignoreCase = true) || sub.url.contains(".vtt", ignoreCase = true)) {
                    androidx.media3.common.MimeTypes.TEXT_VTT
                } else {
                    androidx.media3.common.MimeTypes.APPLICATION_SUBRIP
                }
                MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(sub.url))
                    .setMimeType(subMime)
                    .setLanguage(sub.language)
                    .setLabel(sub.label)
                    .setSelectionFlags(0)
                    .build()
            }
            mediaItemBuilder.setSubtitleConfigurations(subtitleConfigs)
        }

        val mediaItem = mediaItemBuilder.build()
        // Let DefaultMediaSourceFactory handle everything:
        // It correctly builds HlsMediaSource for M3U8.
        // It also seamlessly injects SubtitleConfigurations via MergingMediaSource and SubtitleExtractor
        // without crashing due to legacy decoding being disabled.
        var source: androidx.media3.exoplayer.source.MediaSource = mediaSourceFactory.createMediaSource(mediaItem)
        
        // 7. Attach external audio tracks if present
        if (stream.audioTracks.isNotEmpty() && stream.audioTracks.any { it.url.isNotBlank() }) {
            val audioSources = stream.audioTracks.filter { it.url.isNotBlank() }.map { audioTrack ->
                val audioItem = MediaItem.Builder()
                    .setUri(android.net.Uri.parse(audioTrack.url))
                    .build()
                mediaSourceFactory.createMediaSource(audioItem)
            }
            if (audioSources.isNotEmpty()) {
                val allSources = arrayOf(source) + audioSources.toTypedArray()
                source = androidx.media3.exoplayer.source.MergingMediaSource(*allSources)
            }
        }
        
        exoPlayer.setMediaSource(source)
        exoPlayer.prepare()
    }

    override fun play() { exoPlayer.play() }
    override fun pause() { exoPlayer.pause() }
    override fun seekTo(positionMs: Long) {
        val duration = exoPlayer.duration
        val clamped = if (duration > 0) positionMs.coerceIn(0L, duration) else positionMs.coerceAtLeast(0L)
        pendingSeekPosition = clamped
        _state.value = _state.value.copy(positionMs = clamped)

        seekJob?.cancel()
        seekJob = scope.launch {
            delay(100L)
            val target = pendingSeekPosition ?: clamped
            pendingSeekPosition = null
            exoPlayer.seekTo(target)
            updateState()
        }
    }

    override fun seekForward(ms: Long) {
        val basePos = pendingSeekPosition ?: exoPlayer.currentPosition
        seekTo(basePos + ms)
    }

    override fun seekBackward(ms: Long) {
        val basePos = pendingSeekPosition ?: exoPlayer.currentPosition
        seekTo((basePos - ms).coerceAtLeast(0L))
    }

    override fun setVolume(volume: Float) {
        exoPlayer.volume = volume
    }

    override fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
        updateState()
    }

    override fun release() {
        seekJob?.cancel()
        seekJob = null
        pendingSeekPosition = null
        stopProgressTracking()
        scope.cancel()
        mediaSession.release()
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
        val lang = track.language?.takeIf { it.isNotBlank() && it != "und" } ?: track.label
        if (!lang.isNullOrBlank()) {
            prefs.edit().putString("preferred_audio_language", lang).apply()
            com.cinetheta.core.utils.StreamLogger.debug("Media3Player", "Saved preferred audio language: $lang")
        }
    }

    private fun findMatchingAudio(tracks: List<AudioTrack>, preferred: String): AudioTrack? {
        val p = preferred.trim().lowercase()
        return tracks.find { track ->
            val lang = (track.language ?: "").trim().lowercase()
            val label = (track.label ?: "").trim().lowercase()

            if (lang == p || label == p) return@find true
            if (p.startsWith("hin") || p.contains("hindi")) {
                return@find lang.startsWith("hi") || lang.startsWith("hin") || label.contains("hindi")
            }
            if (p.startsWith("eng") || p.contains("english")) {
                return@find lang.startsWith("en") || lang.startsWith("eng") || label.contains("english")
            }
            if (p.startsWith("jap") || p.contains("japanese")) {
                return@find lang.startsWith("ja") || lang.startsWith("jp") || label.contains("japanese")
            }
            (lang.isNotBlank() && (lang.startsWith(p) || p.startsWith(lang))) || label.contains(p)
        }
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
    override fun Surface(
        modifier: androidx.compose.ui.Modifier,
        isFullScreen: Boolean,
        resizeMode: Int
    ) {
        val playerState = state.collectAsState()
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                android.widget.FrameLayout(ctx).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    val playerView = androidx.media3.ui.PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_NEVER)
                        this.resizeMode = resizeMode
                        // Hide built-in subtitle view so it doesn't get scaled/cropped
                        subtitleView?.visibility = android.view.View.GONE
                    }
                    
                    addView(playerView, android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    ))
                    
                    val customSubtitleView = androidx.media3.ui.SubtitleView(ctx).apply {
                        val prefs = ctx.getSharedPreferences("cinetheta_settings", android.content.Context.MODE_PRIVATE)
                        val textColorHex = prefs.getString("subtitle_text_color", "#FFFFFF") ?: "#FFFFFF"
                        val bgColorHex = prefs.getString("subtitle_bg_color", "#80000000") ?: "#80000000"
                        val textColor = try { android.graphics.Color.parseColor(textColorHex) } catch (e: Exception) { android.graphics.Color.WHITE }
                        val bgColor = try { android.graphics.Color.parseColor(bgColorHex) } catch (e: Exception) { android.graphics.Color.TRANSPARENT }

                        val customStyle = androidx.media3.ui.CaptionStyleCompat(
                            textColor,
                            bgColor,
                            android.graphics.Color.TRANSPARENT,
                            androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                            android.graphics.Color.BLACK,
                            null
                        )
                        setStyle(customStyle)
                        setUserDefaultTextSize()
                        setBottomPaddingFraction(0.08f)
                    }
                    
                    addView(customSubtitleView, android.widget.FrameLayout.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    ))
                    
                    val listener = object : androidx.media3.common.Player.Listener {
                        override fun onCues(cueGroup: androidx.media3.common.text.CueGroup) {
                            customSubtitleView.setCues(cueGroup.cues)
                        }
                    }
                    exoPlayer.addListener(listener)
                    
                    // We also need to remove the listener when this view is detached
                    addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(v: android.view.View) {}
                        override fun onViewDetachedFromWindow(v: android.view.View) {
                            exoPlayer.removeListener(listener)
                        }
                    })
                }
            },
            update = { frameLayout ->
                val playerView = frameLayout.getChildAt(0) as androidx.media3.ui.PlayerView
                playerView.resizeMode = resizeMode
                if (frameLayout.childCount > 1) {
                    val subView = frameLayout.getChildAt(1) as? androidx.media3.ui.SubtitleView
                    if (subView != null) {
                        val prefs = subView.context.getSharedPreferences("cinetheta_settings", android.content.Context.MODE_PRIVATE)
                        val textColorHex = prefs.getString("subtitle_text_color", "#FFFFFF") ?: "#FFFFFF"
                        val bgColorHex = prefs.getString("subtitle_bg_color", "#80000000") ?: "#80000000"
                        val textColor = try { android.graphics.Color.parseColor(textColorHex) } catch (e: Exception) { android.graphics.Color.WHITE }
                        val bgColor = try { android.graphics.Color.parseColor(bgColorHex) } catch (e: Exception) { android.graphics.Color.TRANSPARENT }
                        subView.setStyle(
                            androidx.media3.ui.CaptionStyleCompat(
                                textColor,
                                bgColor,
                                android.graphics.Color.TRANSPARENT,
                                androidx.media3.ui.CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                                android.graphics.Color.BLACK,
                                null
                            )
                        )
                    }
                }
                playerView.keepScreenOn = true
            },
            modifier = modifier
        )
    }
}
