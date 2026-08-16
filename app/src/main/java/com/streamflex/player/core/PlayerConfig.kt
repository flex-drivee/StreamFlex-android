package com.streamflex.player.core

data class PlayerConfig(
    val autoPlay: Boolean = true,
    val saveProgress: Boolean = true,
    val showThumbnailPreview: Boolean = false // Reserved for future milestone
)
