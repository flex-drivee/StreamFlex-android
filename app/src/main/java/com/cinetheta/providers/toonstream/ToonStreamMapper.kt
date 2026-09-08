package com.cinetheta.providers.toonstream

import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.MediaType
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.domain.models.Quality
import com.cinetheta.domain.models.SearchResult

object ToonStreamMapper {

    fun toSearchResult(
        title     : String,
        detailUrl : String,
        poster    : String?,
        mediaType : MediaType,
        year      : Int? = null
    ): SearchResult = SearchResult(
        id           = detailUrl.hashCode().toString(),
        url          = detailUrl,
        providerId   = ToonStreamConfig.PROVIDER_ID,
        providerName = ToonStreamConfig.PROVIDER_NAME,
        title        = title,
        poster       = poster,
        year         = year,
        mediaType    = mediaType
    )

    fun toProviderSource(
        iframeUrl : String,
        hostType  : HostType,
        referer   : String,
        quality   : Quality = Quality.UNKNOWN,
        metadata  : Map<String, String> = emptyMap()
    ): ProviderSource = ProviderSource(
        provider = ToonStreamConfig.PROVIDER_NAME,
        host     = hostType.name.lowercase(),
        hostType = hostType,
        url      = iframeUrl,
        quality  = quality,
        referer  = referer,
        headers  = mapOf("Referer" to referer),
        metadata = metadata
    )
}
