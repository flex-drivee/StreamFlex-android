package com.streamflex.player.media3

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.datasource.DefaultHttpDataSource
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
    private val context: Context
) : StreamPlayer {

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PlayerEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<PlayerEvent> = _events.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    
    private val trackSelector = DefaultTrackSelector(context)
    
    internal val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
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
                val playerError = PlayerError(error.errorCode, error.message ?: "Unknown error", error)
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
                                bitrate = format.bitrate
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
                            val option = AudioTrack(
                                id = "${group.mediaTrackGroup.hashCode()}_$i",
                                language = format.language,
                                label = format.label ?: format.language?.uppercase() ?: "Audio ${i + 1}"
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
                            val option = SubtitleTrack(
                                id = "${group.mediaTrackGroup.hashCode()}_$i",
                                language = format.language,
                                label = format.label ?: format.language?.uppercase() ?: "Subtitle ${i + 1}",
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
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true) // Disable subtitles by default initially
        )
        
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(stream.headers)
            
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        
        val mediaItem = MediaItem.fromUri(stream.url)
        val source = mediaSourceFactory.createMediaSource(mediaItem)
        
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
}
