package com.cinetheta.extractors.turbovid

import com.cinetheta.core.logger.Logger
import com.cinetheta.core.network.HttpClient
import com.cinetheta.core.network.NetworkResult
import com.cinetheta.core.network.RequestBuilder
import com.cinetheta.domain.models.ExtractionResult
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.extractors.common.BaseExtractor

class TurboVidExtractor : BaseExtractor() {
    override val hostType = HostType.TURBOVID

    override suspend fun extract(source: ProviderSource): ExtractionResult {
        val req = RequestBuilder()
            .url(source.url)
            .header("Referer", "https://turbovidhls.com/")
            .build()
            
        return when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> {
                val html = res.data.body?.toString(Charsets.UTF_8) ?: return emptyResult()
                val m3u8Regex = Regex("""https?://[^\s"'<>]+\.m3u8[^\s"'<>]*""")
                val m3u8Url = m3u8Regex.find(html)?.value
                
                if (m3u8Url != null) {
                    Logger.d("[TurboVid] Found stream: $m3u8Url")
                    result(listOf(createStream(source, m3u8Url)))
                } else {
                    Logger.w("[TurboVid] No m3u8 found in HTML")
                    emptyResult()
                }
            }
            else -> emptyResult()
        }
    }
}
