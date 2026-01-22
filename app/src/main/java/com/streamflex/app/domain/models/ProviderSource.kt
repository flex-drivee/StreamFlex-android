package com.streamflex.app.domain.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.InternalSerializationApi // 1. Add Import

@OptIn(InternalSerializationApi::class)

@Serializable
data class ProviderSource(
    val host: String,          // e.g., "StreamSB", "HDHub4u"
    val embedUrl: String,      // iframe / redirect url
    val quality: String? = null,
    val isDirect: Boolean = false
)
