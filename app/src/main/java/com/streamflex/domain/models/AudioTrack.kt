package com.streamflex.domain.models

data class AudioTrack(

    val language: String,

    val label: String = language,

    val codec: String? = null,

    val channels: String? = null,

    val isDefault: Boolean = false
)