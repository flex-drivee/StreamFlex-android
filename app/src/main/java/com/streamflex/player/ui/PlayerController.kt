package com.streamflex.player.ui

import android.util.Log
import com.streamflex.domain.models.StreamLink
import com.streamflex.player.StreamStateHolder
import com.streamflex.player.core.PlayerEvent
import com.streamflex.player.core.PlayerState
import com.streamflex.player.core.StreamPlayer
import com.streamflex.player.episodes.IntroSegment
import com.streamflex.player.episodes.NextEpisodeManager
import com.streamflex.player.resume.PlaybackProgressManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerController(
    val player: StreamPlayer,
    private val progressManager: PlaybackProgressManager,
    private val mediaId: String,
    private val scope: CoroutineScope
) {
    val state: StateFlow<PlayerState> = player.state
    
    val nextEpisodeManager = NextEpisodeManager(scope)
    
    private val _allStreams = MutableStateFlow<List<StreamLink>>(emptyList())
    val allStreams: StateFlow<List<StreamLink>> = _allStreams.asStateFlow()
    
    private val _currentStreamIndex = MutableStateFlow(0)
    val currentStreamIndex: StateFlow<Int> = _currentStreamIndex.asStateFlow()

    private val _introSegments = MutableStateFlow<List<IntroSegment>>(emptyList())
    
    private val _activeSkipSegment = MutableStateFlow<IntroSegment?>(null)
    val activeSkipSegment: StateFlow<IntroSegment?> = _activeSkipSegment.asStateFlow()

    private var lastSavedPosition = 0L

    init {
        scope.launch {
            player.events.collect { event ->
                if (event is PlayerEvent.PlaybackEnded) {
                    val nextEp = StreamStateHolder.getNextEpisode()
                    if (nextEp != null) {
                        nextEpisodeManager.triggerNextEpisodeCountdown {
                            StreamStateHolder.onEpisodeSelected?.invoke(nextEp)
                        }
                    }
                } else if (event is PlayerEvent.Error) {
                    Log.e("PlayerController", "Stream failed, trying fallback.")
                    val nextIndex = _currentStreamIndex.value + 1
                    if (nextIndex < _allStreams.value.size) {
                        _currentStreamIndex.value = nextIndex
                        loadCurrentStream()
                    }
                }
            }
        }
        
        scope.launch {
            player.state.collect { st ->
                val currentSeg = _introSegments.value.firstOrNull { st.positionMs in it.startMs..it.endMs }
                if (_activeSkipSegment.value != currentSeg) {
                    _activeSkipSegment.value = currentSeg
                }

                // Periodically save progress every ~10 seconds of playback
                if (st.isPlaying && (st.positionMs - lastSavedPosition > 10000L || st.positionMs < lastSavedPosition)) {
                    progressManager.saveProgress(mediaId, st.positionMs, st.durationMs)
                    lastSavedPosition = st.positionMs
                }
            }
        }
    }

    fun setStreams(streams: List<StreamLink>) {
        if (streams.isEmpty()) return
        val isNew = _allStreams.value.isEmpty()
        _allStreams.value = streams
        if (isNew) {
            _currentStreamIndex.value = 0
            loadCurrentStream()
        }
    }
    
    fun setIntroSegments(segments: List<IntroSegment>) {
        _introSegments.value = segments
    }
    
    fun skipSegment(segment: IntroSegment) {
        seekTo(segment.endMs)
    }
    
    
    fun selectStream(index: Int) {
        if (index in _allStreams.value.indices && index != _currentStreamIndex.value) {
            // Save progress of current stream before switching
            progressManager.saveProgress(mediaId, state.value.positionMs, state.value.durationMs)
            _currentStreamIndex.value = index
            loadCurrentStream()
        }
    }
    
    private fun loadCurrentStream() {
        val index = _currentStreamIndex.value
        val streams = _allStreams.value
        if (index in streams.indices) {
            Log.d("PlayerController", "Loading stream $index: ${streams[index].url}")
            player.load(streams[index])
            
            // Restore progress
            val savedPos = progressManager.getProgress(mediaId)
            if (savedPos > 0) {
                player.seekTo(savedPos)
                lastSavedPosition = savedPos
            }
            
            player.play()
        }
    }

    fun play() = player.play()
    fun pause() = player.pause()
    fun seekTo(positionMs: Long) = player.seekTo(positionMs)
    fun seekForward() = player.seekForward()
    fun seekBackward() = player.seekBackward()
    fun togglePlayPause() {
        if (state.value.isPlaying) pause() else play()
    }
    
    fun retry() {
        loadCurrentStream()
    }
    
    fun tryNextServer() {
        val nextIndex = _currentStreamIndex.value + 1
        if (nextIndex < _allStreams.value.size) {
            _currentStreamIndex.value = nextIndex
            loadCurrentStream()
        }
    }
    
    fun hasNextServer(): Boolean {
        return _currentStreamIndex.value + 1 < _allStreams.value.size
    }
    
    fun release() {
        // Save one final time
        progressManager.saveProgress(mediaId, state.value.positionMs, state.value.durationMs)
        player.release()
    }
}
