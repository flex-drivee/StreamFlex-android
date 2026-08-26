package com.streamflex.extractors.streamtape

import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.StreamLink
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.extractors.shared.ExtractorHelper
import com.streamflex.extractors.shared.ExtractorUtils
import com.streamflex.core.utils.StreamLogger

/**
 * Extractor for StreamTape.
 */
class StreamTapeExtractor : BaseExtractor() {

    override val hostType = HostType.STREAMTAPE

    override suspend fun extract(
        source: ProviderSource
    ): ExtractionResult {
        
        // StreamTape URL format: /v/VIDEO_ID/filename.mkv or /v/VIDEO_ID
        // Embed URL must be: /e/VIDEO_ID  (no filename)
        val rawUrl = source.url
        val videoId = rawUrl
            .replace(Regex("https?://[^/]+"), "")  // strip domain
            .removePrefix("/v/")
            .removePrefix("/e/")
            .split("/").first()                       // keep only the ID segment

        val baseHost = rawUrl
            .let { Regex("https?://[^/]+").find(it)?.value }
            ?: "https://streamtape.com"

        val url = "$baseHost/e/$videoId"

        StreamLogger.info("StreamTapeExtractor", "Extracting StreamTape page: $url")

        val document = ExtractorHelper.fetchDocument(
            url,
            source.headers
        )
        
        val html = document.html()
        
        // Find script line with innerHTML assignment to StreamTape video link container
        val targetLine = html.lines().firstOrNull { line ->
            (line.contains("botlink", ignoreCase = true) || 
             line.contains("robotlink", ignoreCase = true) || 
             line.contains("norobotlink", ignoreCase = true) || 
             line.contains("ideoolink", ignoreCase = true) ||
             line.contains("crypted", ignoreCase = true)) &&
            line.contains("innerHTML", ignoreCase = true) &&
            line.contains("get_video")
        }

        var streamUrl: String? = null

        if (targetLine != null) {
            val rhs = targetLine.substringAfter("innerHTML").substringAfter("=")
            val stringParts = Regex("""['"]([^'"]+)['"]""").findAll(rhs).map { it.groupValues[1] }.toList()
            if (stringParts.isNotEmpty()) {
                val combined = stringParts.joinToString("")
                streamUrl = when {
                    combined.startsWith("http") -> combined
                    combined.startsWith("//") -> "https:$combined"
                    combined.startsWith("/") -> "https://streamtape.com$combined"
                    else -> "https://$combined"
                }
            }
        }

        // Fallback regex patterns
        if (streamUrl == null) {
            val regexSingle = Regex("""getElementById\(['"][^'"]*(?:botlink|ideoolink)[^'"]*['"]\)\.innerHTML\s*=\s*'([^']+)'\s*\+\s*'([^']+)'""")
            val regexDouble = Regex("""getElementById\(['"][^'"]*(?:botlink|ideoolink)[^'"]*['"]\)\.innerHTML\s*=\s*"([^"]+)"\s*\+\s*"([^"]+)""")
            val match = regexSingle.find(html) ?: regexDouble.find(html)
            if (match != null) {
                val combined = match.groupValues[1] + match.groupValues[2]
                streamUrl = if (combined.startsWith("//")) "https:$combined" else if (combined.startsWith("http")) combined else "https://$combined"
            }
        }

        if (!streamUrl.isNullOrBlank() && (streamUrl.contains("get_video") || streamUrl.contains("streamtape"))) {
            val finalUrl = if (!streamUrl.contains("stream=1")) {
                if (streamUrl.contains("?")) "$streamUrl&stream=1" else "$streamUrl?stream=1"
            } else streamUrl

            StreamLogger.info("StreamTapeExtractor", "Found stream URL: $finalUrl")
            val stream = createStream(
                source = source.copy(referer = url),
                url = finalUrl
            )
            return result(streams = listOf(stream))
        }
        
        StreamLogger.warn("StreamTapeExtractor", "No StreamTape video link found")
        return emptyResult()
    }
}
