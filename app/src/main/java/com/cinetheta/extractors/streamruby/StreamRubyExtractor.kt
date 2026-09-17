package com.cinetheta.extractors.streamruby

import com.cinetheta.core.constants.Constants
import com.cinetheta.core.logger.Logger
import com.cinetheta.core.network.HttpClient
import com.cinetheta.core.network.NetworkResult
import com.cinetheta.core.network.RequestBuilder
import com.cinetheta.domain.models.ExtractionResult
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.domain.models.StreamLink
import com.cinetheta.domain.models.Subtitle
import com.cinetheta.extractors.common.BaseExtractor
import com.cinetheta.extractors.shared.JsUnpacker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI

class StreamRubyExtractor : BaseExtractor() {
    override val hostType = HostType.STREAMRUBY

    companion object {
        private const val TAG = "StreamRubyExtractor"
    }

    override suspend fun extract(source: ProviderSource): ExtractionResult = withContext(Dispatchers.IO) {
        val rawUrl = source.url.trim()
        val baseDomain = try {
            URI(rawUrl).host ?: "rubystm.com"
        } catch (_: Exception) {
            "rubystm.com"
        }

        // Clean file code: e.g. "https://rubystm.com/g6j5ibxc7apl.html" -> "g6j5ibxc7apl"
        val fileCode = rawUrl.substringAfterLast("/")
            .substringBefore(".")
            .removePrefix("e/")
            .trim()

        if (fileCode.isBlank()) {
            Logger.w("[$TAG] Empty file code from $rawUrl")
            return@withContext emptyResult()
        }

        Logger.d("[$TAG] Extracting fileCode '$fileCode' on $baseDomain")

        val candidateStreams = mutableListOf<StreamLink>()
        val foundSubtitles = mutableListOf<Subtitle>()

        fun parsePage(html: String, referer: String) {
            // 1. Check for subtitles
            val subRegex = Regex("""["']file["']\s*:\s*["'](https?://[^"']+\.(?:vtt|srt)[^"']*)["']""")
            for (m in subRegex.findAll(html)) {
                val subUrl = m.groupValues[1]
                foundSubtitles.add(Subtitle(language = "English", url = subUrl, label = "English"))
            }

            // 2. Direct file regex in page
            val m3u8Match = Regex("""file:\s*["'](.*?m3u8.*?)["']""").find(html)?.groupValues?.get(1)
                ?: Regex("""["'](https?://[^"'\s<>]+\.m3u8[^"'\s<>]*)["']""").find(html)?.groupValues?.get(1)

            if (!m3u8Match.isNullOrBlank()) {
                candidateStreams.add(
                    StreamLink(
                        name = "StreamRuby",
                        url = m3u8Match,
                        host = HostType.STREAMRUBY,
                        contentType = com.cinetheta.core.network.detector.ContentType.HLS,
                        adaptive = true,
                        headers = mapOf("Referer" to referer),
                        referer = referer
                    )
                )
                return
            }

            // 3. Unpack any packed JS blocks
            val scriptBlocks = Regex("""(?s)<script[^>]*>(.*?)</script>""").findAll(html)
                .map { it.groupValues[1] }
                .toList()
                .ifEmpty { listOf(html) }

            for (script in scriptBlocks) {
                val unpacked = if (script.contains("function(p,a,c,k,e,d)") || script.contains("function(p,a,c,k,e,r)")) {
                    JsUnpacker.unpack(script) ?: script
                } else {
                    script
                }

                // Subtitles in unpacked
                val subMatch = subRegex.find(unpacked)?.groupValues?.get(1)
                if (subMatch != null) {
                    foundSubtitles.add(Subtitle(language = "English", url = subMatch, label = "English"))
                }

                val fileMatch = Regex("""file\s*:\s*["']([^"']+)["']""").find(unpacked)?.groupValues?.get(1)
                    ?: Regex("""["'](https?://[^"'\s<>]+\.m3u8[^"'\s<>]*)["']""").find(unpacked)?.groupValues?.get(1)

                if (!fileMatch.isNullOrBlank() && (fileMatch.contains(".m3u8") || fileMatch.contains(".mp4"))) {
                    candidateStreams.add(
                        StreamLink(
                            name = "StreamRuby",
                            url = fileMatch,
                            host = HostType.STREAMRUBY,
                            contentType = if (fileMatch.contains(".m3u8")) com.cinetheta.core.network.detector.ContentType.HLS else com.cinetheta.core.network.detector.ContentTypeDetector.detect(fileMatch),
                            adaptive = fileMatch.contains(".m3u8"),
                            headers = mapOf("Referer" to referer),
                            referer = referer
                        )
                    )
                    return
                }
            }
        }

        // Strategy 1: Fetch embed URL (player page)
        val embedUrl = "https://$baseDomain/e/$fileCode"
        val embedReq = RequestBuilder()
            .url(embedUrl)
            .header("Referer", rawUrl)
            .header("User-Agent", Constants.DEFAULT_USER_AGENT)
            .build()
        when (val res = HttpClient.execute(embedReq)) {
            is NetworkResult.Success -> parsePage(res.data.bodyAsString(), embedUrl)
            else -> {}
        }

        // Strategy 2: If embed didn't yield streams, fetch raw URL / direct page
        if (candidateStreams.isEmpty()) {
            val pageReq = RequestBuilder()
                .url(rawUrl)
                .header("Referer", "https://$baseDomain/")
                .header("User-Agent", Constants.DEFAULT_USER_AGENT)
                .build()
            when (val res = HttpClient.execute(pageReq)) {
                is NetworkResult.Success -> parsePage(res.data.bodyAsString(), rawUrl)
                else -> {}
            }
        }

        // Strategy 3: Fallback POST to /dl endpoint with sanitized fileCode
        if (candidateStreams.isEmpty()) {
            val dlUrl = "https://$baseDomain/dl"
            val payload = "op=embed&file_code=$fileCode&auto=1&referer="
            val postReq = RequestBuilder()
                .url(dlUrl)
                .post(payload.toByteArray(Charsets.UTF_8))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Referer", rawUrl)
                .header("User-Agent", Constants.DEFAULT_USER_AGENT)
                .build()
            when (val res = HttpClient.execute(postReq)) {
                is NetworkResult.Success -> parsePage(res.data.bodyAsString(), "https://$baseDomain/")
                else -> {}
            }
        }

        if (candidateStreams.isNotEmpty()) {
            val distinctSubs = foundSubtitles.distinctBy { it.url }
            val finalStreams = candidateStreams.map { it.copy(subtitles = distinctSubs) }
            Logger.i("[$TAG] Successfully extracted ${finalStreams.size} stream(s), ${distinctSubs.size} subtitle(s)")
            return@withContext result(finalStreams)
        }

        Logger.w("[$TAG] Failed to extract streams for $rawUrl")
        emptyResult()
    }
}
