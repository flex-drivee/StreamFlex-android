package com.streamflex.providers.hdhub4u

import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.Quality
import com.streamflex.domain.models.SearchResult

/**
 * Maps HDHub4u raw data into StreamFlex domain models.
 */
object HDHubMapper {

    /**
     * Create a SearchResult.
     */
    fun toSearchResult(
        title: String,
        detailUrl: String,
        poster: String? = null,
        year: Int? = null,
        mediaType: MediaType = MediaType.MOVIE
    ): SearchResult {

        return SearchResult(
            id = detailUrl,
            title = title,
            poster = poster,
            year = year,
            mediaType = mediaType
        )
    }

    /**
     * Create a ProviderResult.
     */
    fun toProviderResult(
        provider: String,
        title: String,
        detailUrl: String,
        sources: List<ProviderSource>,
        mediaType: MediaType,
        year: Int? = null,
        season: Int? = null,
        episode: Int? = null,
        poster: String? = null,
        overview: String? = null
    ): ProviderResult {

        return ProviderResult(
            id = detailUrl,
            provider = provider,
            title = title,
            detailUrl = detailUrl,
            sources = sources,
            mediaType = mediaType,
            year = year,
            season = season,
            episode = episode,
            poster = poster,
            overview = overview
        )
    }

    /**
     * Create a ProviderSource.
     */
    fun toProviderSource(
        provider: String,
        host: String,
        hostType: com.streamflex.domain.models.HostType,
        url: String,
        quality: Quality = Quality.UNKNOWN,
        headers: Map<String, String> = emptyMap(),
        cookies: Map<String, String> = emptyMap(),
        referer: String? = null,
        priority: Int = 0,
        isDirect: Boolean = false
    ): ProviderSource {

        return ProviderSource(
            provider = provider,
            host = host,
            hostType = hostType,
            url = url,
            quality = quality,
            headers = headers,
            cookies = cookies,
            referer = referer,
            priority = priority,
            isDirect = isDirect
        )
    }
}