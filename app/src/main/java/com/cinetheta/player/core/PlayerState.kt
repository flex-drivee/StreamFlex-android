package com.cinetheta.player.core

import com.cinetheta.player.quality.QualityOption
import com.cinetheta.player.tracks.AudioTrack
import com.cinetheta.player.tracks.SubtitleTrack

data class PlayerState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    
    // Progress
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    
    // Tracks & Qualities
    val currentQuality: QualityOption? = null,
    val availableQualities: List<QualityOption> = emptyList(),
    val audioTracks: List<AudioTrack> = emptyList(),
    val subtitleTracks: List<SubtitleTrack> = emptyList(),
    val selectedAudio: AudioTrack? = null,
    val selectedSubtitle: SubtitleTrack? = null,
    
    // UI State
    val showControls: Boolean = true,
    val isFullscreen: Boolean = false,
    
    // Errors
    val error: PlayerError? = null
)
