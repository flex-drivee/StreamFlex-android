package com.streamflex.app.domain.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.InternalSerializationApi // 1. Add Import

@OptIn(InternalSerializationApi::class)

@Serializable
data class Episode(
    val id: String,
    val title: String,
    val episodeNumber: Int,
    val overview: String? = null,
    val airDate: String? = null,
    val runtime: Int? = null,
    val providerSources: List<ProviderSource> = emptyList(),
    val streams: List<VideoStream> = emptyList()
)
