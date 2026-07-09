package com.streamflex.domain.models

/**
 * Represents one season scraped from a provider.
 */
data class ProviderSeason(

    val number: Int,

    val title: String = "Season $number",

    val poster: String? = null,

    val episodes: List<ProviderEpisode> = emptyList()
)