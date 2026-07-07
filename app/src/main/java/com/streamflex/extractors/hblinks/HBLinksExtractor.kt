package com.streamflex.extractors.hblinks

import com.streamflex.core.network.detector.HostDetector
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.extractors.shared.ExtractorHelper

/**
 * HBLinks is an intermediate redirect page.
 *
 * It usually forwards to:
 *
 * - HubCloud
 * - HubDrive
 * - HubCDN
 * - HubStream
 * - Google Video
 *
 * This extractor simply discovers those URLs and lets
 * ExtractorManager continue the extraction chain.
 */
class HBLinksExtractor : BaseExtractor() {

    override val hostType = HostType.HBLINKS

    override suspend fun extract(
        source: ProviderSource
    ): ExtractionResult {

        val document = ExtractorHelper.fetchDocument(
            source.url,
            source.headers
        )

        val links = linkedSetOf<String>()

        // Prefer obvious download buttons first.
        document.select(
            "h3 a, h5 a, div.entry-content a, a.btn, a[class*=btn], a[href]"
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

            // Ignore invalid/self links
            if (
                lower == source.url.lowercase() ||
                lower.startsWith("javascript:") ||
                lower == "#" ||
                lower.isBlank()
            ) {
                return@forEach
            }

            // Ignore social/navigation links
            if (
                lower.contains("facebook") ||
                lower.contains("twitter") ||
                lower.contains("telegram") ||
                lower.contains("discord") ||
                lower.contains("imdb") ||
                lower.contains("/category/") ||
                lower.contains("/tag/")
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