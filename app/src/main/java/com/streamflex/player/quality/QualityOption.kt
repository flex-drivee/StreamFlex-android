package com.streamflex.player.quality

data class QualityOption(
    val id: String,
    val name: String,
    val resolution: Int = -1,
    val bitrate: Int = -1,
    val mimeType: String? = null,
    val codecs: String? = null,
    val isAuto: Boolean = false
)
