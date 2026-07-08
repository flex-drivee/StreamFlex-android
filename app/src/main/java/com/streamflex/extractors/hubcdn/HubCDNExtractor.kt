package com.streamflex.extractors.hubcdn

import android.util.Base64
import com.streamflex.core.network.detector.HostDetector
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.extractors.shared.ExtractorHelper
import com.streamflex.extractors.shared.ExtractorUtils

/**
 * HubCDN extractor.
 *
 * HubCDN usually stores the next URL inside a Base64
 * encoded JavaScript variable:
 *
 *     var reurl = "...?r=BASE64"
 *
 * After decoding it usually points to:
 *
 * • Google Video
 * • HubCloud
 * • HubDrive
 * • Redirect page
 * • Direct MP4
 * • HLS (.m3u8)
 */
class HubCDNExtractor : BaseExtractor() {

    override val hostType = HostType.HUBCDN

    override suspend fun extract(
        source: ProviderSource
    ): ExtractionResult {

        val document = ExtractorHelper.fetchDocument(
            source.url,
            source.headers
        )

        val nextSources = mutableListOf<ProviderSource>()

        // ----------------------------------------------------
        // Look for every script block
        // ----------------------------------------------------

        document.select("script")
            .forEach { script ->

                val data = script.data()

                if (data.isBlank()) {
                    return@forEach
                }

                // --------------------------------------------
                // Find reurl="..."
                // --------------------------------------------

                val encoded = Regex(
                    """reurl\s*=\s*["']([^"']+)["']"""
                )
                    .find(data)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.substringAfter("?r=")

                if (encoded.isNullOrBlank()) {
                    return@forEach
                }

                // --------------------------------------------
                // Decode Base64
                // --------------------------------------------

                val decoded = runCatching {

                    String(
                        Base64.decode(
                            encoded,
                            Base64.DEFAULT
                        )
                    )

                }.getOrNull() ?: return@forEach

                // --------------------------------------------
                // Sometimes HubCDN prefixes "link="
                // --------------------------------------------

                val finalUrl = decoded
                    .substringAfterLast("link=")
                    .trim()

                if (finalUrl.isBlank()) {
                    return@forEach
                }

                nextSources += source.copy(

                    url = finalUrl,

                    hostType =
                        HostDetector.detect(finalUrl)

                )
            }

        // ----------------------------------------------------
        // Fallback:
        // Search the whole HTML for URLs
        // ----------------------------------------------------

        if (nextSources.isEmpty()) {

            ExtractorUtils
                .allMatches(
                    """https?:\/\/[^\s"'<>\\]+""",
                    document.html()
                )
                .distinct()
                .forEach { url ->

                    nextSources += source.copy(

                        url = url,

                        hostType =
                            HostDetector.detect(url)

                    )
                }
        }

        return ExtractionResult(

            sources =
                nextSources.distinctBy {

                    it.url

                }
        )
    }
}