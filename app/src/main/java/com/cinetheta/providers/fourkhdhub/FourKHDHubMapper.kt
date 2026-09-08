package com.cinetheta.providers.fourkhdhub

import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.MediaType
import com.cinetheta.domain.models.ProviderResult
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.domain.models.Quality
import com.cinetheta.domain.models.SearchResult

object FourKHDHubMapper {

    fun toSearchResult(
        title: String,
        detailUrl: String,
        poster: String?,
        year: Int?,
        mediaType: MediaType
    ): SearchResult {
        return SearchResult(
            id = detailUrl.hashCode().toString(),
            url = detailUrl,
            providerId = FourKHDHubConfig.PROVIDER_ID,
            providerName = "4KHDHub",
            title = title,
            poster = poster,
            year = year,
            mediaType = mediaType
        )
    }

    fun toProviderSource(
        provider: String,
        host: String,
        hostType: HostType,
        url: String,
        quality: Quality,
        referer: String? = null,
        headers: Map<String, String> = emptyMap(),
        metadata: Map<String, String> = emptyMap()
    ): ProviderSource {
        return ProviderSource(
            provider = provider,
            host = host,
            hostType = hostType,
            url = url,
            quality = quality,
            referer = referer,
            headers = headers,
            metadata = metadata
        )
    }
}
