package com.streamflex.player.core

sealed class PlayerEvent {
    object PlaybackStarted : PlayerEvent()
    object PlaybackPaused : PlayerEvent()
    object PlaybackEnded : PlayerEvent()
    data class Error(val error: PlayerError) : PlayerEvent()
    data class VideoSizeChanged(val width: Int, val height: Int) : PlayerEvent()
}
