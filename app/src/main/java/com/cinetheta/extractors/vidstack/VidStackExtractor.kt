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

        val subtitles = mutableListOf<com.cinetheta.domain.models.Subtitle>()
        
        // 1. Scan for <track ...> HTML tags
        val trackTagRegex = Regex("""<track\s+([^>]+)>""", RegexOption.IGNORE_CASE)
        val srcAttrRegex = Regex("""src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val labelAttrRegex = Regex("""label=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val srclangAttrRegex = Regex("""srclang=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

        trackTagRegex.findAll(html).forEach { match ->
            val attrs = match.groupValues[1]
            val subUrl = srcAttrRegex.find(attrs)?.groupValues?.get(1)
            if (!subUrl.isNullOrBlank()) {
                val resolvedUrl = if (subUrl.startsWith("http")) subUrl else try {
                    java.net.URI(source.url).resolve(subUrl).toString()
                } catch (_: Exception) { subUrl }
                val label = labelAttrRegex.find(attrs)?.groupValues?.get(1) ?: "Subtitle"
                val srclang = srclangAttrRegex.find(attrs)?.groupValues?.get(1) ?: label
                subtitles.add(
                    com.cinetheta.domain.models.Subtitle(
                        url = resolvedUrl,
                        language = srclang,
                        label = label
                    )
                )
            }
        }

        // 2. Scan for JSON tracks: [...]
        val jsonTrackRegex = Regex("""["']?file["']?\s*:\s*["']([^"']+\.(?:vtt|srt)[^"']*)["'](?:\s*,\s*["']?label["']?\s*:\s*["']([^"']+)["'])?""", RegexOption.IGNORE_CASE)
        jsonTrackRegex.findAll(html).forEach { match ->
            val subUrl = match.groupValues[1]
            val label = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() } ?: "Subtitle"
            val resolvedUrl = if (subUrl.startsWith("http")) subUrl else try {
                java.net.URI(source.url).resolve(subUrl).toString()
            } catch (_: Exception) { subUrl }
            if (subtitles.none { it.url == resolvedUrl }) {
                subtitles.add(
                    com.cinetheta.domain.models.Subtitle(
                        url = resolvedUrl,
                        language = label,
                        label = label
                    )
                )
            }
        }

        // 1. Check for standard <source src="..."> tags
        val sourceMatch = sourceRegex.find(html)
        if (sourceMatch != null) {
            streams += createStream(
                source = source,
                url = sourceMatch.groupValues[1],
                subtitles = subtitles
            )
        }
        
        // 2. Fallback: scan the entire HTML for any embedded .m3u8 URL
        if (streams.isEmpty()) {
            val hlsMatch = hlsRegex.find(html)
            if (hlsMatch != null) {
                streams += createStream(
                    source = source,
                    url = hlsMatch.groupValues[1],
                    subtitles = subtitles
                )
            }
        }
        
        // 3. Fallback: scan for any embedded .mp4 URL
        if (streams.isEmpty()) {
            val mp4Match = mp4Regex.find(html)
            if (mp4Match != null) {
                streams += createStream(
                    source = source,
                    url = mp4Match.groupValues[1],
                    subtitles = subtitles
                )
            }
        }

        return result(streams = streams)
    }
}
