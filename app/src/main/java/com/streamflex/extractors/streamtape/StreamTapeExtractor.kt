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
        
        val url = source.url.replace("/v/", "/e/")
        
        StreamLogger.info("StreamTapeExtractor", "Extracting StreamTape page: $url")
        
        val document = ExtractorHelper.fetchDocument(
            url,
            source.headers
        )
        
        val html = document.html()
        
        // StreamTape hides the video link by joining two strings like:
        // document.getElementById('ideoolink').innerHTML = "/streamtape.com/get_video?id=..." + "&expires=...";
        val regex = """document\.getElementById\('robotlink'\)\.innerHTML\s*=\s*'([^']+)'\s*\+\s*'([^']+)';""".toRegex()
        val match = regex.find(html)
        
        if (match != null) {
            val part1 = match.groupValues[1]
            val part2 = match.groupValues[2]
            
            val streamUrl = "https:/" + part1 + part2
            
            val stream = createStream(
                source = source,
                url = streamUrl
            )
            
            return result(
                streams = listOf(stream)
            )
        }
        
        StreamLogger.warn("StreamTapeExtractor", "No StreamTape video link found")
        return emptyResult()
    }
}
