package com.streamflex.providers.fourkhdhub

import com.streamflex.core.network.detector.HostDetector
import com.streamflex.core.network.detector.QualityDetector
import com.streamflex.core.parser.HtmlParser
import com.streamflex.core.parser.SourceParser
import com.streamflex.core.parser.TransportResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import org.jsoup.nodes.Document

class FourKHDHubSourceParser : SourceParser {

    companion object {
        private const val PROVIDER_NAME = "4KHDHub"
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

        val accordions = document.select("button.accordion-button")
        
        for (accordion in accordions) {
            val titleElement = accordion.selectFirst(".flex-1")
            
            var isHevc = false
            var qualityStr = ""
            
            if (titleElement != null) {
                val fullTitle = titleElement.text()
                qualityStr = fullTitle
                if (fullTitle.contains("HEVC", ignoreCase = true) || fullTitle.contains("x265", ignoreCase = true)) {
                    isHevc = true
                }
            }
            
            val quality = QualityDetector.detect(qualityStr)

            val targetId = accordion.attr("aria-controls")
            val contentDiv = document.getElementById(targetId)
            
            if (contentDiv != null) {
                val links = contentDiv.select("a[href]")
                for (link in links) {
                    var url = HtmlParser.absUrl(link, "href")
                    if (url.isBlank()) url = HtmlParser.attr(link, "href")
                    
                    if (url.isBlank() || !visited.add(url)) continue
                    if (shouldSkipUrl(url)) continue

                    var hostType = HostDetector.detect(url)
                    if (hostType == HostType.UNKNOWN && url.contains("drive", ignoreCase = true)) {
                        hostType = HostType.REDIRECT 
                    }
                    if (hostType == HostType.UNKNOWN) continue
                    
                    val metadata = mapOf("codec" to if (isHevc) "HEVC" else "H.264")

                    results += FourKHDHubMapper.toProviderSource(
                        provider = PROVIDER_NAME,
                        host = hostType.name,
                        hostType = hostType,
                        url = url,
                        quality = quality,
                        referer = sourceUrl,
                        headers = mapOf("Referer" to sourceUrl),
                        metadata = metadata
                    )
                }
            }
        }
        
        if (results.isEmpty()) {
            val elements = document.select("a[href]")
            for (element in elements) {
                var url = HtmlParser.absUrl(element, "href")
                if (url.isBlank()) url = HtmlParser.attr(element, "href")
                
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

                results += FourKHDHubMapper.toProviderSource(
                    provider = PROVIDER_NAME,
                    host = hostType.name,
                    hostType = hostType,
                    url = url,
                    quality = quality,
                    referer = sourceUrl,
                    headers = mapOf("Referer" to sourceUrl)
                )
            }
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
