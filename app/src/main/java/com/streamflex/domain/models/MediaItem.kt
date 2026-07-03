package com.streamflex.domain.models

/**
 * Common contract shared by all media content.
 */
interface MediaItem {

    val id: String

    val title: String

    val originalTitle: String?

    val mediaType: MediaType

    val poster: String?

    val backdrop: String?

    val overview: String?

    val language: String?

    val year: Int?

    val rating: Double?

    val imdbId: String?

    val tmdbId: Int?
}