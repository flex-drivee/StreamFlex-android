package com.cinetheta.extractors.googlevideo

import com.cinetheta.core.network.detector.ContentTypeDetector
import com.cinetheta.domain.models.ExtractionResult
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.extractors.common.BaseExtractor
import com.cinetheta.extractors.shared.ExtractorUtils

/**
 * Google Video extractor.
 *
 * Google Video links are already direct playable streams.
 *
 * No additional extraction is required.
 */
class GoogleVideoExtractor : BaseExtractor() {

    override val hostType = HostType.GOOGLE_VIDEO

    override suspend fun extract(
        source: ProviderSource
    ): ExtractionResult {

        if (!supports(source)) {
            return ExtractionResult.EMPTY
        }

        val url = source.url.trim()

        if (url.isBlank()) {
            return ExtractionResult.EMPTY
        }

        // Accept Google's direct media links even if
        // the URL doesn't end with a video extension.
        val playable =

            url.contains("googlevideo.com", true) ||
                    url.contains("googleusercontent.com", true) ||
                    ExtractorUtils.isVideoUrl(url)

        if (!playable) {
            return ExtractionResult.EMPTY
        }

        val stream = createStream(
            source = source,
            url = url
        )

        return ExtractionResult.streams(
            stream
        )
    }
}