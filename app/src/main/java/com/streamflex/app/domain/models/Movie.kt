package com.streamflex.app.domain.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.InternalSerializationApi

@Serializable
data class CastMember(
    val name: String,
    val character: String?,
    val imageUrl: String?
)

@Serializable
data class Trailer(
    val key: String,
    val name: String
)

@Serializable
data class ProductionCompany(
    val name: String,
    val logoUrl: String?
)


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
    val streams: List<VideoStream> = emptyList(),
    val cast: List<CastMember> = emptyList(),
    val trailers: List<Trailer> = emptyList(),
    val productionCompanies: List<ProductionCompany> = emptyList()
)
