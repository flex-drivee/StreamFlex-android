package com.streamflex.extractors.hdstream4u

import com.streamflex.core.constants.Constants
import com.streamflex.core.network.detector.QualityDetector
import com.streamflex.core.parser.HtmlParser
import com.streamflex.core.utils.StreamLogger
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.StreamLink
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.extractors.shared.ExtractorHelper
import com.streamflex.extractors.shared.JsUnpacker
import java.net.URI

/**
 * HDStream4u / HubStream / VidHide online watch video extractor.
 *
 * Extracts direct HLS (.m3u8) streams from player embed pages by
 * unpacking packed JavaScript and parsing player sources.
 */
class HdStream4uExtractor : BaseExtractor() {

    override val hostType = HostType.HDSTREAM4U

    companion object {
        private const val TAG = "HdStream4uExtractor"
        private val M3U8_REGEX = Regex("""["'](https?://[^"'\s<>]+\.m3u8(?:[^"'\s<>]*)?)["']""")
        private val FILE_REGEX = Regex("""file\s*:\s*["']([^"']+)["']""")
    }

    override suspend fun extract(source: ProviderSource): ExtractionResult {
        StreamLogger.info(TAG, "Extracting HDStream4u: ${source.url}")

        val embedUrl = getEmbedUrl(source.url)
        val origin = try {
            val uri = URI(embedUrl)
            "${uri.scheme}://${uri.host}"
        } catch (_: Exception) {
            "https://hdstream4u.com"
        }

        val headers = mapOf(
            "Referer" to (source.referer ?: origin),
            "Origin" to origin,
            "User-Agent" to Constants.DEFAULT_USER_AGENT,
            "Sec-Fetch-Dest" to "empty",
            "Sec-Fetch-Mode" to "cors",
            "Sec-Fetch-Site" to "cross-site"
        )

        val html = ExtractorHelper.getHtml(embedUrl, headers)
        if (html.isBlank()) {
            StreamLogger.warn(TAG, "Empty HTML received from: $embedUrl")
            return emptyResult()
        }

        val streams = mutableListOf<StreamLink>()
        val doc = HtmlParser.parse(html)
        val scripts = doc.select("script")

        for (script in scripts) {
            val scriptData = script.data()
            if (scriptData.isBlank()) continue

            // Unpack if packed JS is detected
            val unpacked = JsUnpacker.unpack(scriptData) ?: scriptData

            // Search for direct m3u8 or video file inside script
            val m3u8Matches = M3U8_REGEX.findAll(unpacked).map { it.groupValues[1] }.toList()
            val fileMatches = FILE_REGEX.findAll(unpacked).map { it.groupValues[1] }.toList()

            val candidates = (m3u8Matches + fileMatches).distinct().filter { 
                it.startsWith("http") && (it.contains(".m3u8") || it.contains(".mp4"))
            }

            for (streamUrl in candidates) {
                val cleanUrl = streamUrl.replace("\\/", "/")
                val detectedQuality = QualityDetector.detect(cleanUrl)
                val finalQuality = if (detectedQuality != com.streamflex.domain.models.Quality.UNKNOWN) {
                    detectedQuality
                } else {
                    source.quality
                }

                streams += createStream(
                    source = source.copy(
                        referer = embedUrl,
                        headers = mapOf(
                            "Referer" to embedUrl,
                            "Origin" to origin,
                            "User-Agent" to Constants.DEFAULT_USER_AGENT
                        )
                    ),
                    url = cleanUrl,
                    quality = finalQuality
                )
            }
        }

        // Direct regex on raw HTML in case scripts weren't in standard <script> tags
        if (streams.isEmpty()) {
            val rawM3u8 = M3U8_REGEX.findAll(html).map { it.groupValues[1].replace("\\/", "/") }
                .filter { it.startsWith("http") }
                .distinct()
                .toList()

            for (url in rawM3u8) {
                streams += createStream(
                    source = source.copy(
                        referer = embedUrl,
                        headers = mapOf(
                            "Referer" to embedUrl,
                            "Origin" to origin,
                            "User-Agent" to Constants.DEFAULT_USER_AGENT
                        )
                    ),
                    url = url
                )
            }
        }

        StreamLogger.info(TAG, "HDStream4u resolved ${streams.size} stream(s)")
        return result(streams.distinctBy { it.url })
    }

    private fun getEmbedUrl(url: String): String {
        return when {
            url.contains("/file/") -> url.replace("/file/", "/v/")
            url.contains("/download/") -> url.replace("/download/", "/v/")
            url.contains("/d/") -> url.replace("/d/", "/v/")
            url.contains("/f/") -> url.replace("/f/", "/v/")
            else -> url
        }
    }
}
