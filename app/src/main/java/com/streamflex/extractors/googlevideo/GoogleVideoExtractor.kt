package com.streamflex.extractors.googlevideo

import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.StreamLink
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.extractors.shared.ExtractorUtils

/**
 * Extractor for Google Video direct links.
 *
 * These URLs are already playable and usually require
 * no additional extraction.
 */
class GoogleVideoExtractor : BaseExtractor() {

    override val hostType = HostType.GOOGLE_VIDEO

    override suspend fun extract(
        source: ProviderSource
    ): List<StreamLink> {

        if (!supports(source)) {
            return emptyList()
        }

        val url = source.url

        if (url.isBlank()) {
            return emptyList()
        }

        if (!ExtractorUtils.isVideoUrl(url)) {
            return emptyList()
        }

        return listOf(
            createStream(
                source = source,
                url = url
            )
        )
    }
}