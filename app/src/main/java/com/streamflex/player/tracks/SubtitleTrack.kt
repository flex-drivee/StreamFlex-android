package com.streamflex.player.tracks

data class SubtitleTrack(
    val id: String,
    val language: String?,
    val label: String?,
    val mimeType: String? = null,
    val url: String? = null,
    val isEmbedded: Boolean = true,
    val isSelected: Boolean = false
)
