package com.streamflex.providers.moviebox

import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.ProviderSeason
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.Quality
import com.streamflex.domain.models.SearchResult

object MovieBoxMapper {

    fun toSearchResult(
        id: String,
        title: String,
        detailUrl: String,
        poster: String?,
        year: Int?,
        mediaType: MediaType
    ): SearchResult {
        return SearchResult(
            id = id,
            url = detailUrl,
            providerId = MovieBoxConfig.PROVIDER_NAME.lowercase(),
            providerName = MovieBoxConfig.PROVIDER_NAME,
            title = title,
            mediaType = mediaType,
            year = year,
            poster = poster
        )
    }

    fun toProviderResult(
        providerId: String,
        title: String,
        detailUrl: String,
        mediaType: MediaType,
        sources: List<ProviderSource> = emptyList(),
        seasons: List<ProviderSeason> = emptyList(),
        year: Int? = null,
        poster: String? = null,
        overview: String? = null,
        success: Boolean = true,
        error: String? = null
    ): ProviderResult {
        return ProviderResult(
            id = detailUrl,
            providerId = providerId,
            title = title,
            detailUrl = detailUrl,
            mediaType = mediaType,
            sources = sources,
            seasons = seasons,
            year = year,
            poster = poster,
            overview = overview,
            success = success,
            error = error
        )
    }
    
    fun toProviderSource(
        url: String,
        quality: Quality = Quality.UNKNOWN
    ): ProviderSource {
        return ProviderSource(
            provider = MovieBoxConfig.PROVIDER_NAME,
            host = MovieBoxConfig.PROVIDER_NAME,
            hostType = HostType.MOVIEBOX,
            url = url,
            quality = quality,
            isDirect = false
        )
    }
}
