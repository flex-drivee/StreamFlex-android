package com.cinetheta.extractors.vidmoly

import com.cinetheta.core.logger.Logger
import com.cinetheta.core.network.HttpClient
import com.cinetheta.core.network.NetworkResult
import com.cinetheta.core.network.RequestBuilder
import com.cinetheta.domain.models.ExtractionResult
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.extractors.common.BaseExtractor

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

                val subtitles = mutableListOf<com.cinetheta.domain.models.Subtitle>()
                val trackRegex = Regex("""["']?file["']?\s*:\s*["']([^"']+\.(?:vtt|srt)[^"']*)["'](?:\s*,\s*["']?label["']?\s*:\s*["']([^"']+)["'])?""", RegexOption.IGNORE_CASE)
                trackRegex.findAll(html).forEach { match ->
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
                
                if (m3u8Url != null) {
                    Logger.d("[Vidmoly] Found stream: $m3u8Url")
                    result(listOf(createStream(source, m3u8Url, subtitles = subtitles)))
                } else {
                    Logger.w("[Vidmoly] No m3u8 found in HTML")
                    emptyResult()
                }
            }
            else -> emptyResult()
        }
    }
}
