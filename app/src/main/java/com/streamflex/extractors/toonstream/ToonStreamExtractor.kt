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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class ToonStreamExtractor : BaseExtractor() {

    override val hostType = HostType.TOONSTREAM

    override suspend fun extract(source: ProviderSource): ExtractionResult = coroutineScope {
        val pageUrl = source.url
        val baseUrl = source.metadata["baseUrl"] ?: ToonStreamConfig.DEFAULT_DOMAIN

        val req = RequestBuilder().url(pageUrl).header("Referer", baseUrl).build()
        val html = when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> res.data.body?.toString(Charsets.UTF_8)
            else -> null
        } ?: return@coroutineScope emptyResult()

        val doc = HtmlParser.parse(html)
        
        val embedUrls = doc.select("iframe")
            .mapNotNull { it.attr("src").takeIf { s -> s.isNotBlank() } ?: it.attr("data-src").takeIf { s -> s.isNotBlank() } }
            .filter { it.contains("/embed/") }
            .map { if (it.startsWith("http")) it else "$baseUrl$it" }
            .distinct()

        val deferredSources = embedUrls.mapIndexed { index, embedUrl ->
            async(Dispatchers.IO) {
                val embedHtml = fetchHtml(embedUrl, baseUrl) ?: return@async null
                val embedDoc = HtmlParser.parse(embedHtml)
                val realIframe = embedDoc.selectFirst("iframe")?.attr("src") ?: return@async null
                
                // Let the Engine's HostDetector classify the URL!
                val detectedType = HostDetector.detect(realIframe)
                // If it couldn't detect it, we pass it as REDIRECT so RedirectExtractor can try its best
                val finalType = if (detectedType == HostType.UNKNOWN) HostType.REDIRECT else detectedType

                ToonStreamMapper.toProviderSource(
                    iframeUrl = realIframe,
                    hostType  = finalType,
                    referer   = embedUrl,
                    metadata  = mapOf("server" to (index + 1).toString())
                )
            }
        }

        val extractedSources = deferredSources.awaitAll().filterNotNull()

        return@coroutineScope result(streams = emptyList(), sources = extractedSources)
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
