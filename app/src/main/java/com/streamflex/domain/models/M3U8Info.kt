package com.streamflex.domain.models

data class M3U8Info(

    val isM3U8: Boolean,

    val isMasterPlaylist: Boolean,

    val isMediaPlaylist: Boolean,

    val isLiveStream: Boolean,

    val variants: List<String>,

    val qualities: List<String>
)