package com.streamflex.providers.hdhub4u

import com.streamflex.domain.models.HostType
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
            url = detailUrl,
            provider = "HDHub4u",
            title = title,
            mediaType = mediaType,
            year = year,
            poster = poster
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
        overview: String? = null,
        success: Boolean = true,
        error: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): ProviderResult {

        return ProviderResult(
            id = detailUrl,
            provider = provider,
            title = title,
            detailUrl = detailUrl,
            metadata = metadata,
            sources = sources,
            mediaType = mediaType,
            year = year,
            season = season,
            episode = episode,
            poster = poster,
            overview = overview,
            success = success,
            error = error
        )
    }
    /**
     * Create a ProviderSource.
     */
    fun toProviderSource(
        provider: String,
        host: String,
        hostType: HostType,
        url: String,
        quality: Quality = Quality.UNKNOWN,
        headers: Map<String, String> = emptyMap(),
        cookies: Map<String, String> = emptyMap(),
        referer: String? = null,
        metadata: Map<String, String> = emptyMap()
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
            metadata = metadata
        )
    }
}