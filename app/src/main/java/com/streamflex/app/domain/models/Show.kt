package com.streamflex.app.domain.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.InternalSerializationApi

@OptIn(InternalSerializationApi::class)
@Serializable
data class Show(
    val id: String,
    val title: String,
    val overview: String? = null,
    val poster: String? = null,
    val backdrop: String? = null,
    val year: Int? = null,
    val rating: Double? = null,
    val genres: List<String> = emptyList(),
    val seasons: List<Season> = emptyList(),
    val cast: List<CastMember> = emptyList(),
    val trailers: List<Trailer> = emptyList(),
    val productionCompanies: List<ProductionCompany> = emptyList()
)
