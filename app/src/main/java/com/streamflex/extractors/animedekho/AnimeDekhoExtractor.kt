package com.streamflex.extractors.animedekho

import com.streamflex.core.logger.Logger
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.HtmlParser
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.providers.animedekho.AnimeDekhoConfig
import com.streamflex.providers.animedekho.AnimeDekhoMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class AnimeDekhoExtractor : BaseExtractor() {

    override val hostType = HostType.ANIMEDEKHO

    companion object {
        private const val TAG = "AnimeDekhoExtractor"
    }

    override suspend fun extract(source: ProviderSource): ExtractionResult = coroutineScope {
        val pageUrl = source.url
        val isMovie = source.metadata["isMovie"]?.toBoolean() ?: false
        val baseUrl = source.metadata["baseUrl"] ?: AnimeDekhoConfig.DEFAULT_DOMAIN

        val req = RequestBuilder().url(pageUrl).header("Referer", baseUrl).build()
        val html = when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> res.data.body?.toString(Charsets.UTF_8)
            else -> null
        } ?: return@coroutineScope emptyResult()

        val doc = HtmlParser.parse(html)
        val bodyClasses = doc.body()?.classNames() ?: emptySet()
        val termId = bodyClasses
            .firstOrNull { it.startsWith("term-") && it.removePrefix("term-").all(Char::isDigit) }
            ?.removePrefix("term-")
            ?: bodyClasses.firstOrNull { it.startsWith("postid-") && it.removePrefix("postid-").all(Char::isDigit) }
            ?.removePrefix("postid-")
            ?: run {
                Logger.w("[$TAG] term ID or postid not found in body class for $pageUrl")
                return@coroutineScope emptyResult()
            }

        val trtype = if (isMovie) 1 else 2

        val jobs = (1..AnimeDekhoConfig.MAX_SERVERS).map { serverIdx ->
            async(Dispatchers.IO) {
                fetchIframeSource(baseUrl, termId, serverIdx, trtype)
            }
        }

        val extractedSources = jobs.awaitAll()
            .filterNotNull()
            .map { (iframeSrc, serverIdx) ->
                val ht = resolveHostType(iframeSrc)
                Logger.d("[$TAG] Server $serverIdx → $ht : $iframeSrc")
                AnimeDekhoMapper.toProviderSource(
                    iframeUrl = iframeSrc,
                    hostType  = ht,
                    referer   = baseUrl,
                    metadata  = mapOf("server" to serverIdx.toString())
                )
            }

        return@coroutineScope result(streams = emptyList(), sources = extractedSources)
    }

    private suspend fun fetchIframeSource(
        baseUrl   : String,
        termId    : String,
        serverIdx : Int,
        trtype    : Int
    ): Pair<String, Int>? {
        val url = "$baseUrl/?trdekho=$serverIdx&trid=$termId&trtype=$trtype"
        val req = RequestBuilder()
            .url(url)
            .header("Referer", baseUrl)
            .build()

        return when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> {
                val html = res.data.body?.toString(Charsets.UTF_8) ?: return null
                val doc  = HtmlParser.parse(html)
                val src  = doc.selectFirst("iframe[src]")?.attr("src")
                    ?.takeIf { it.isNotBlank() } ?: return null
                src to serverIdx
            }
            else -> null
        }
    }

    private fun resolveHostType(url: String): HostType {
        val lower = url.lowercase()
        return when {
            "abyssplayer.com" in lower || "playhydrax.com" in lower -> HostType.ABYSS
            "vidmoly" in lower                                       -> HostType.VIDMOLY
            "rubystm.com" in lower || "streamruby" in lower         -> HostType.STREAMRUBY
            "gdmirrorbot" in lower                                   -> HostType.GDMIRRORBOT
            "cloudy.upns" in lower                                   -> HostType.CLOUDY
            "turbovidhls" in lower || "emturbovid" in lower         -> HostType.TURBOVID
            "strmup.to" in lower                                     -> HostType.STREAMUP
            "xerver.xyz" in lower                                    -> HostType.XERVER
            "goblin" in lower                                        -> HostType.REDIRECT
            "pixel" in lower                                         -> HostType.REDIRECT
            "vidcloud.upns" in lower                                 -> HostType.CLOUDY
            lower.endsWith(".mp4") || lower.endsWith(".mkv")        -> HostType.DIRECT
            ".m3u8" in lower                                         -> HostType.M3U8
            else                                                     -> HostType.UNKNOWN
        }
    }
}
