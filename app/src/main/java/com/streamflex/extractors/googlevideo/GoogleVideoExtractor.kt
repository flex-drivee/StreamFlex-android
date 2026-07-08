package com.streamflex.extractors.googlevideo

import com.streamflex.core.network.detector.ContentTypeDetector
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.extractors.shared.ExtractorUtils

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