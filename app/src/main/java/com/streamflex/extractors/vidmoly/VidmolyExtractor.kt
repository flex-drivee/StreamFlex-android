package com.streamflex.extractors.vidmoly

import com.streamflex.core.logger.Logger
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.extractors.common.BaseExtractor

class VidmolyExtractor : BaseExtractor() {
    override val hostType = HostType.VIDMOLY

    override suspend fun extract(source: ProviderSource): ExtractionResult {
        val req = RequestBuilder()
            .url(source.url)
            .header("Referer", "https://vidmoly.me/")
            .build()
            
        return when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> {
                val html = res.data.body?.toString(Charsets.UTF_8) ?: return emptyResult()
                val m3u8Regex = Regex("""https?://[^\s"'<>]+\.m3u8[^\s"'<>]*""")
                val m3u8Url = m3u8Regex.find(html)?.value
                
                if (m3u8Url != null) {
                    Logger.d("[Vidmoly] Found stream: $m3u8Url")
                    result(listOf(createStream(source, m3u8Url)))
                } else {
                    Logger.w("[Vidmoly] No m3u8 found in HTML")
                    emptyResult()
                }
            }
            else -> emptyResult()
        }
    }
}
