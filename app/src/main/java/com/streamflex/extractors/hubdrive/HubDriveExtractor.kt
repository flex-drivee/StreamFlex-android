package com.streamflex.extractors.hubdrive

import com.streamflex.core.network.detector.HostDetector
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.extractors.shared.ExtractorHelper

/**
 * HubDrive normally does not contain playable streams.
 *
 * It redirects to another host such as:
 *
 * - HubCloud
 * - HubCDN
 * - HBLinks
 * - HubStream
 * - Google Video
 * - Direct video files
 *
 * Those URLs are returned as ProviderSources so the
 * ExtractorManager can continue the extraction pipeline.
 */
class HubDriveExtractor : BaseExtractor() {

    override val hostType = HostType.HUBDRIVE

    override suspend fun extract(
        source: ProviderSource
    ): ExtractionResult {

        val document = ExtractorHelper.fetchDocument(
            source.url,
            source.headers
        )

        val links = linkedSetOf<String>()

        // Prefer download buttons, then fall back to every anchor.
        document.select(
            "a.btn, a[class*=btn], a[href]"
        ).forEach { element ->

            val url = element.absUrl("href")

            if (url.isNotBlank()) {
                links += url
            }
        }

        if (links.isEmpty()) {
            return ExtractionResult()
        }

        val keywords = listOf(
            "hubcloud",
            "hubdrive",
            "hubcdn",
            "hubstream",
            "hblinks",
            "googlevideo",
            "googleusercontent"
        )

        val nextSources = mutableListOf<ProviderSource>()

        links.forEach { url ->

            val lower = url.lowercase()

            // Skip invalid/self links
            if (
                lower == source.url.lowercase() ||
                lower.startsWith("javascript:") ||
                lower == "#" ||
                lower.isBlank()
            ) {
                return@forEach
            }

            // Skip obvious garbage
            if (
                lower.contains("facebook") ||
                lower.contains("twitter") ||
                lower.contains("telegram") ||
                lower.contains("discord") ||
                lower.contains("imdb") ||
                lower.contains("/category/") ||
                lower.contains("/tag/") ||
                lower.contains("/page/") ||
                lower.contains("/sign") ||
                lower.contains("/login") ||
                lower.contains("/register") ||
                lower.contains("/contact") ||
                lower.contains("/about") ||
                lower.endsWith(".tips/") ||
                lower.endsWith(".tips") ||
                lower.endsWith(".fans/") ||
                lower.endsWith(".fans") ||
                lower.endsWith(".cx/") ||
                lower.endsWith(".cx") ||
                lower.endsWith(".co/") ||
                lower.endsWith(".co")
            ) {
                return@forEach
            }

            val useful =
                keywords.any(lower::contains) ||
                        lower.endsWith(".m3u8") ||
                        lower.endsWith(".mp4") ||
                        lower.endsWith(".mkv") ||
                        lower.endsWith(".mpd")

            if (!useful) {
                return@forEach
            }

            nextSources += source.copy(
                url = url,
                hostType = HostDetector.detect(url)
            )
        }

        return ExtractionResult(
            sources = nextSources.distinctBy { it.url }
        )
    }
}