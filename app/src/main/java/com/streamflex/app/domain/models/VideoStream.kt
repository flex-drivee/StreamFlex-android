package com.streamflex.app.domain.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.InternalSerializationApi // 1. Add Import

@OptIn(InternalSerializationApi::class)
@Serializable
data class VideoStream(
    val url: String,                   // final playable url (m3u8/mp4)
    val quality: String? = null,
    val headers: Map<String, String> = emptyMap()
)
