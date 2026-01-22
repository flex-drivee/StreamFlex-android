package com.streamflex.app.domain.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.InternalSerializationApi // 1. Add Import

@OptIn(InternalSerializationApi::class)

@Serializable
data class Movie(
    val id: String,
    val title: String,
    val overview: String? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val year: Int? = null,
    val rating: Double? = null,
    val runtime: Int? = null,
    val genres: List<String> = emptyList(),
    val providerSources: List<ProviderSource> = emptyList(),
    val streams: List<VideoStream> = emptyList()
)
