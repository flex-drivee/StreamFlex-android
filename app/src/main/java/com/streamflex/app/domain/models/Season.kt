package com.streamflex.app.domain.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.InternalSerializationApi // 1. Add Import

@OptIn(InternalSerializationApi::class)

@Serializable
data class Season(
    val seasonNumber: Int,
    val episodes: List<Episode> = emptyList()
)
