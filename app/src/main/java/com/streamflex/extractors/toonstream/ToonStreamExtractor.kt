package com.streamflex.extractors.toonstream

import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.network.detector.HostDetector
import com.streamflex.core.parser.HtmlParser
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.providers.toonstream.ToonStreamConfig
import com.streamflex.providers.toonstream.ToonStreamMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

class ToonStreamExtractor : BaseExtractor() {

    override val hostType = HostType.TOONSTREAM

    override suspend fun extract(source: ProviderSource): ExtractionResult = withContext(Dispatchers.IO) {
        val pageUrl = source.url
        val baseUrl = source.metadata["baseUrl"] ?: ToonStreamConfig.DEFAULT_DOMAIN

        val req = RequestBuilder().url(pageUrl).header("Referer", baseUrl).build()
        val html = when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> res.data.body?.toString(Charsets.UTF_8)
            else -> null
        } ?: return@withContext emptyResult()

        val doc = HtmlParser.parse(html)
        
        val embedUrls = doc.select("iframe")
            .mapNotNull { it.attr("src").takeIf { s -> s.isNotBlank() } ?: it.attr("data-src").takeIf { s -> s.isNotBlank() } }
            .filter { it.contains("/embed/") }
            .map { if (it.startsWith("http")) it else "$baseUrl$it" }
            .distinct()

        val extractedSources = mutableListOf<ProviderSource>()
        
        for ((index, embedUrl) in embedUrls.withIndex()) {
            if (index > 0) delay(200) // Delay to avoid Toonstream 502 Rate Limit
            
            val embedHtml = fetchHtml(embedUrl, baseUrl) ?: continue
            val embedDoc = HtmlParser.parse(embedHtml)
            val realIframe = embedDoc.selectFirst("iframe")?.attr("src") ?: continue
            
            // Let the Engine's HostDetector classify the URL!
            val detectedType = HostDetector.detect(realIframe)
            // If it couldn't detect it, we pass it as REDIRECT so RedirectExtractor can try its best
            val finalType = if (detectedType == HostType.UNKNOWN) HostType.REDIRECT else detectedType

            val providerSource = ToonStreamMapper.toProviderSource(
                iframeUrl = realIframe,
                hostType  = finalType,
                referer   = embedUrl,
                metadata  = mapOf("server" to (index + 1).toString())
            )
            extractedSources.add(providerSource)
        }

        return@withContext result(streams = emptyList(), sources = extractedSources)
    }

    private suspend fun fetchHtml(url: String, referer: String): String? {
        val req = RequestBuilder()
            .url(url)
            .header("Referer", referer)
            .build()
        return when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> res.data.body?.toString(Charsets.UTF_8)
            else -> null
        }
    }
}
