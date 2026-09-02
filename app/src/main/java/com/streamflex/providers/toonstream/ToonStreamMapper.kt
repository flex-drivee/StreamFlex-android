package com.streamflex.providers.toonstream

import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.Quality
import com.streamflex.domain.models.SearchResult

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
