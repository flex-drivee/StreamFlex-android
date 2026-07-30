package com.streamflex.providers.hdhub4u

import com.streamflex.core.network.detector.HostDetector
import com.streamflex.core.network.detector.QualityDetector
import com.streamflex.core.parser.HtmlParser
import com.streamflex.core.parser.SourceParser
import com.streamflex.core.parser.TransportResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import org.jsoup.nodes.Document

/**
 * HDHub4U Source Parser implementation (Phase 1.3 & Phase 1.7).
 *
 * Implements [SourceParser]:
 * - Scrapes embed URLs (HubCloud, HubDrive, HDHub redirect links) from HTML.
 * - Consistent with Phase 1.7 roadmap rules:
 *   - Does NOT implement redirect unwrapping (e.g. WP ?id= decoding).
 *   - Does NOT resolve iframes or run extractors.
 *   - ResolverEngine & ExtractorManager own the entire resolution pipeline.
 */
class HDHubSourceParser : SourceParser {

    companion object {
        private const val PROVIDER_NAME = "HDHub4u"
    }

    override fun parse(raw: TransportResult, sourceUrl: String): List<ProviderSource> {
        val html = raw.asString()
        if (html.isBlank()) return emptyList()
        val document = HtmlParser.parse(html)
        return parseDocument(document, sourceUrl)
    }

    fun parseDocument(document: Document, sourceUrl: String): List<ProviderSource> {
        val results = mutableListOf<ProviderSource>()
        val visited = mutableSetOf<String>()

        val elements = document.select("a[href]")
        for (element in elements) {
            var url = HtmlParser.absUrl(element, "href")
            if (url.isBlank()) {
                url = HtmlParser.attr(element, "href")
            }
            if (url.isBlank() || !visited.add(url)) continue
            if (shouldSkipUrl(url)) continue

            var hostType = HostDetector.detect(url)
            if (hostType == HostType.UNKNOWN && url.contains("?id=")) {
                hostType = HostType.REDIRECT
            }
            if (hostType == HostType.UNKNOWN) continue

            val text = buildString {
                append(HtmlParser.text(element))
                append(" ")
                element.parent()?.let { append(HtmlParser.text(it)) }
            }
            val quality = QualityDetector.detect(text)

            results += HDHubMapper.toProviderSource(
                provider = PROVIDER_NAME,
                host = hostType.name,
                hostType = hostType,
                url = url,
                quality = quality,
                referer = sourceUrl
            )
        }

        return results
    }

    fun shouldSkipUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith("javascript:") ||
                lower == "#" ||
                lower.contains("facebook") ||
                lower.contains("twitter") ||
                lower.contains("telegram") ||
                lower.contains("discord") ||
                lower.contains("instagram") ||
                lower.contains("whatsapp")
    }
}
