package com.streamflex.player.quality

data class QualityOption(
    val id: String,
    val name: String,
    val resolution: Int = -1,
    val bitrate: Int = -1,
    val isAuto: Boolean = false
)
