package com.streamflex.player.core

import com.streamflex.domain.models.StreamLink
import com.streamflex.player.quality.QualityOption
import com.streamflex.player.tracks.AudioTrack
import com.streamflex.player.tracks.SubtitleTrack
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow

interface StreamPlayer {
    val state: StateFlow<PlayerState>
    val events: SharedFlow<PlayerEvent>
    
    // Lifecycle
    fun load(stream: StreamLink)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun seekForward(ms: Long = 10000L)
    fun seekBackward(ms: Long = 10000L)
    fun release()
    fun setVolume(volume: Float)
    
    // Tracks
    fun setQuality(quality: QualityOption)
    fun setAudioTrack(track: AudioTrack)
    fun setSubtitleTrack(track: SubtitleTrack?) // null for off
    
    // UI
    @androidx.compose.runtime.Composable
    fun Surface(modifier: androidx.compose.ui.Modifier, isFullScreen: Boolean)
}
