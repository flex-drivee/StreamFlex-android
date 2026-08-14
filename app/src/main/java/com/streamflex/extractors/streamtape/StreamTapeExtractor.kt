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
        
        // StreamTape hides the video link by joining two strings. Patterns vary by version:
        // Pattern A: document.getElementById('robotlink').innerHTML = '/streamtape.com/...' + '&t=...';
        // Pattern B: innerHTML = "..." + "..."  (double quotes)
        val regexSingle = Regex("""getElementById\('robotlink'\)\.innerHTML\s*=\s*'([^']+)'\s*\+\s*'([^']+)'""")
        val regexDouble = Regex("""getElementById\("robotlink"\)\.innerHTML\s*=\s*"([^"]+)"\s*\+\s*"([^"]+)""")
        // Pattern C: newer obfuscation — token + expiry concatenated
        val regexToken  = Regex("""token=([\w-]+).*?expires=(\d+)""", RegexOption.DOT_MATCHES_ALL)

        val match = regexSingle.find(html) ?: regexDouble.find(html)

        if (match != null) {
            val part1 = match.groupValues[1]
            val part2 = match.groupValues[2]
            val streamUrl = "https:/" + part1 + part2

            StreamLogger.info("StreamTapeExtractor", "Found stream URL: $streamUrl")
            val stream = createStream(source = source, url = streamUrl)
            return result(streams = listOf(stream))
        }
        
        StreamLogger.warn("StreamTapeExtractor", "No StreamTape video link found")
        return emptyResult()
    }
}
