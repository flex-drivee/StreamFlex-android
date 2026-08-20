package com.streamflex.providers.animedekho

import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.Quality
import com.streamflex.domain.models.SearchResult

object AnimeDekhoMapper {

    fun toSearchResult(
        title     : String,
        detailUrl : String,
        poster    : String?,
        mediaType : MediaType,
        year      : Int? = null
    ): SearchResult = SearchResult(
        id           = detailUrl.hashCode().toString(),
        url          = detailUrl,
        providerId   = AnimeDekhoConfig.PROVIDER_ID,
        providerName = AnimeDekhoConfig.PROVIDER_NAME,
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
        provider = AnimeDekhoConfig.PROVIDER_NAME,
        host     = hostType.name.lowercase(),
        hostType = hostType,
        url      = iframeUrl,
        quality  = quality,
        referer  = referer,
        headers  = mapOf("Referer" to referer),
        metadata = metadata
    )
}
