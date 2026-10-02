package com.cinetheta.extractors.vidstack

import com.cinetheta.core.network.HttpClient
import com.cinetheta.core.network.NetworkResult
import com.cinetheta.core.network.RequestBuilder
import com.cinetheta.core.utils.StreamLogger
import com.cinetheta.domain.models.ExtractionResult
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.domain.models.StreamLink
import com.cinetheta.extractors.common.BaseExtractor

class VidStackExtractor : BaseExtractor() {
    override val hostType = HostType.VIDSTACK
    
    companion object {
        private const val TAG = "VidStackExtractor"
    }

    override suspend fun extract(source: ProviderSource): ExtractionResult {
        StreamLogger.info(TAG, "Extracting VidStack: ${source.url}")
        
        val request = RequestBuilder()
            .url(source.url)
            .header("Referer", source.referer ?: source.url)
            .build()
            
        val response = HttpClient.execute(request)
        if (response is NetworkResult.Error) {
            StreamLogger.error(TAG, "Failed to load VidStack page: ${response.message}")
            return emptyResult()
        }
        val html = (response as NetworkResult.Success).data.bodyAsString()
        
        val streams = mutableListOf<StreamLink>()
        
        // VidStack players usually have <source src="..."> or a JSON config
        val sourceRegex = Regex("""<source[^>]+src=["']([^"']+)["']""")
        val hlsRegex = Regex("""["']?(https?://[^"']+\.m3u8[^"']*)["']?""")
        val mp4Regex = Regex("""["']?(https?://[^"']+\.mp4[^"']*)["']?""")

        // 1. Check for standard <source src="..."> tags
        val sourceMatch = sourceRegex.find(html)
        if (sourceMatch != null) {
            streams += createStream(
                source = source,
                url = sourceMatch.groupValues[1]
            )
        }
        
        // 2. Fallback: scan the entire HTML for any embedded .m3u8 URL
        if (streams.isEmpty()) {
            val hlsMatch = hlsRegex.find(html)
            if (hlsMatch != null) {
                streams += createStream(
                    source = source,
                    url = hlsMatch.groupValues[1]
                )
            }
        }
        
        // 3. Fallback: scan for any embedded .mp4 URL
        if (streams.isEmpty()) {
            val mp4Match = mp4Regex.find(html)
            if (mp4Match != null) {
                streams += createStream(
                    source = source,
                    url = mp4Match.groupValues[1]
                )
            }
        }

        return result(streams = streams)
    }
}
