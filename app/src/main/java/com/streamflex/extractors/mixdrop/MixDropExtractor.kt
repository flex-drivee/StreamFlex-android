package com.streamflex.extractors.mixdrop

import com.streamflex.core.utils.StreamLogger
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.extractors.shared.ExtractorHelper
import com.streamflex.extractors.shared.JsUnpacker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MixDrop extractor.
 *
 * Scrapes the MixDrop embed page, extracts the packed JS (eval(function(p,a,c,k,e,d))),
 * unpacks it using JsUnpacker, and searches for the video URL (usually assigned to wurl).
 */
class MixDropExtractor : BaseExtractor() {
    override val hostType = HostType.MIXDROP

    companion object {
        private const val TAG = "MixDropExtractor"
        private val WURL_REGEX = Regex("""wurl\s*=\s*["']([^"']+)["']""")
    }

    override suspend fun extract(source: ProviderSource): ExtractionResult {
        StreamLogger.info(TAG, "Extracting MixDrop: ${source.url}")
        
        // Ensure we are using the embed URL /e/ instead of /f/ (file)
        val embedUrl = source.url.replace("/f/", "/e/")
        
        val html = withContext(Dispatchers.IO) {
            ExtractorHelper.getText(
                url = embedUrl,
                headers = source.headers
            )
        }
        
        if (html.isBlank()) {
            StreamLogger.warn(TAG, "Empty HTML from $embedUrl")
            return emptyResult()
        }

        // MixDrop uses packed JS. We need to find eval(function(p,a,c,k,e,d)...
        val packedMatch = Regex("""eval\s*\(\s*function\s*\(\s*p,a,c,k,e,d\s*\).*?\.split\s*\(\s*["']\|["']\s*\).*?\)\s*\)""").find(html)
            ?: Regex("""eval\s*\(\s*function\s*\(\s*p,a,c,k,e,d\s*\).+?\}\s*\('.+?\.split\s*\(\s*["']\|["']\s*\).*?\)\s*\)""").find(html)
            
        if (packedMatch == null) {
            StreamLogger.warn(TAG, "No packed JS found on $embedUrl")
            return emptyResult()
        }

        val unpacked = JsUnpacker.unpack(packedMatch.value)
        if (unpacked.isNullOrBlank()) {
            StreamLogger.warn(TAG, "Failed to unpack JS")
            return emptyResult()
        }

        val wurlMatch = WURL_REGEX.find(unpacked)
        if (wurlMatch == null) {
            StreamLogger.warn(TAG, "No wurl found in unpacked JS")
            return emptyResult()
        }

        var streamUrl = wurlMatch.groupValues[1]
        if (streamUrl.startsWith("//")) {
            streamUrl = "https:$streamUrl"
        }

        StreamLogger.info(TAG, "MixDrop resolved stream: $streamUrl")

        return result(
            createStream(
                source = source,
                url = streamUrl
            )
        )
    }
}
