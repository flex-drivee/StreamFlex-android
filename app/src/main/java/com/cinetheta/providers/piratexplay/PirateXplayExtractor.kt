package com.cinetheta.providers.piratexplay

import com.cinetheta.core.logger.Logger
import com.cinetheta.core.network.HttpClient
import com.cinetheta.core.network.NetworkResult
import com.cinetheta.core.network.RequestBuilder
import com.cinetheta.core.network.detector.HostDetector
import com.cinetheta.core.parser.HtmlParser
import com.cinetheta.domain.models.ExtractionResult
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.extractors.common.BaseExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.net.URI

class PirateXplayExtractor : BaseExtractor() {

    override val hostType = HostType.PIRATEXPLAY

    companion object {
        private const val TAG = "PirateXplayExtractor"
    }

    override suspend fun extract(source: ProviderSource): ExtractionResult = coroutineScope {
        val pageUrl = source.url
        val baseUrl = source.metadata["baseUrl"] ?: PirateXplayConfig.DEFAULT_DOMAIN

        // 1. Fetch the episode/movie page to find internal player iframes
        val req = RequestBuilder().url(pageUrl).header("Referer", baseUrl).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36").build()
        val html = when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> res.data.body?.toString(Charsets.UTF_8)
            else -> null
        } ?: return@coroutineScope emptyResult()

        val doc = HtmlParser.parse(html)
        
        // Find all internal iframes on the page (public/player)
        val iframeUrls = doc.select("iframe").mapNotNull {
            val src = it.attr("src").takeIf { s -> s.isNotBlank() } ?: it.attr("data-src")
            if (src.contains("public/player")) src else null
        }.distinct()

        if (iframeUrls.isEmpty()) {
            Logger.w("[$TAG] No internal iframes found on $pageUrl")
            return@coroutineScope emptyResult()
        }

        // 2. Fetch all internal iframes to get server options
        val jobs = iframeUrls.map { iframeSrc ->
            async(Dispatchers.IO) {
                fetchServerOptions(iframeSrc, baseUrl)
            }
        }

        val extractedSources = jobs.awaitAll().flatten()

        return@coroutineScope result(streams = emptyList(), sources = extractedSources)
    }

    private suspend fun fetchServerOptions(iframeUrl: String, baseUrl: String): List<ProviderSource> {
        val absoluteUrl = if (iframeUrl.startsWith("http")) iframeUrl else URI(baseUrl).resolve(iframeUrl).toString()
        val req = RequestBuilder().url(absoluteUrl).header("Referer", baseUrl).header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36").build()
        
        val html = when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> res.data.body?.toString(Charsets.UTF_8)
            else -> null
        } ?: return emptyList()

        val doc = HtmlParser.parse(html)
        val serverOptions = doc.select(".server-option[data-link]")
        
        return serverOptions.mapNotNull { option ->
            val dataLink = option.attr("data-link")
            val language = option.attr("data-language").takeIf { it.isNotBlank() } ?: "Unknown"
            
            if (dataLink.isNotBlank()) {
                val resolvedHostType = HostDetector.detect(dataLink)
                ProviderSource(
                    provider = PirateXplayConfig.PROVIDER_NAME,
                    host = "${resolvedHostType.name} ($language)",
                    hostType = resolvedHostType,
                    url = dataLink,
                    referer = baseUrl,
                    metadata = mapOf("language" to language)
                )
            } else {
                null
            }
        }
    }
}
