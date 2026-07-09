package com.streamflex.domain.models

/**
 * Represents one episode scraped from a provider.
 */
data class ProviderEpisode(

    val number: Int,

    val title: String,

    val overview: String? = null,

    val thumbnail: String? = null,

    val sources: List<ProviderSource> = emptyList()
)