package com.streamflex.player.tracks

data class AudioTrack(
    val id: String,
    val language: String?,
    val label: String?,
    val isSelected: Boolean = false
)
