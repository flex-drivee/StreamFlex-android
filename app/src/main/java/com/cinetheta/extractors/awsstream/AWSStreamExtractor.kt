package com.cinetheta.extractors.awsstream

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
import org.json.JSONObject
import java.net.URI

/**
 * AWSStream / AsCdn Extractor (as-cdn26.top, z.awstream.net).
 *
 * Flow:
 * 1. Fetch initial HTML page to extract packed JS containing subtitles (.srt / .vtt).
 * 2. Extract hash from URL: url.substringAfterLast("/")
 * 3. POST to ${baseUrl}/player/index.php?data=${hash}&do=getVideo
 *    Headers: x-requested-with: XMLHttpRequest
 *    Form data: hash=${hash}&r=${baseUrl}
 * 4. Parse JSON response: { "videoSource": "https://...m3u8..." }
 */
class AWSStreamExtractor : BaseExtractor() {

    override val hostType = HostType.AWS_STREAM

    companion object {
        private const val TAG = "AWSStreamExtractor"
    }

    override suspend fun extract(source: ProviderSource): ExtractionResult = withContext(Dispatchers.IO) {
        val url = source.url.trim()
        val hash = url.substringAfterLast("/").substringBefore("?").trim()
        if (hash.isBlank()) {
            Logger.w("[$TAG] Missing hash in URL: $url")
            return@withContext emptyResult()
        }

        val baseUrl = try {
            val uri = URI(url)
            "${uri.scheme}://${uri.host}"
        } catch (_: Exception) {
            "https://z.awstream.net"
        }

        Logger.d("[$TAG] Extracting hash $hash on $baseUrl")

        // 1. Fetch initial player page to extract subtitles (.srt) from packed JS
        val subtitles = mutableListOf<Subtitle>()
        val pageReq = RequestBuilder()
            .url(url)
            .header("Referer", source.referer ?: baseUrl)
            .header("User-Agent", Constants.DEFAULT_USER_AGENT)
            .build()

        when (val pageRes = HttpClient.execute(pageReq)) {
            is NetworkResult.Success -> {
                val pageHtml = pageRes.data.bodyAsString()
                val scriptBlocks = Regex("""(?s)<script[^>]*>(.*?)</script>""").findAll(pageHtml)
                    .map { it.groupValues[1] }
                    .toList()
                    .ifEmpty { listOf(pageHtml) }

                for (script in scriptBlocks) {
                    val unpacked = if (script.contains("function(p,a,c,k,e,d)") || script.contains("function(p,a,c,k,e,r)")) {
                        JsUnpacker.unpack(script) ?: script
                    } else {
                        script
                    }

                    val captionMatch = Regex("""["']kind["']\s*:\s*["']captions["']\s*,\s*["']file["']\s*:\s*["'](https?://[^"']+\.(?:srt|vtt)[^"']*)["']""").find(unpacked)
                        ?: Regex("""["']file["']\s*:\s*["'](https?://[^"']+\.(?:srt|vtt)[^"']*)["']""").find(unpacked)

                    if (captionMatch != null) {
                        val subUrl = captionMatch.groupValues[1].replace("\\/", "/")
                        Logger.i("[$TAG] Found subtitle: $subUrl")
                        subtitles.add(Subtitle(language = "English", url = subUrl, label = "English"))
                    }
                }
            }
            else -> {
                Logger.w("[$TAG] Failed to load initial player page: $url")
            }
        }

        // 2. Query getVideo endpoint
        val apiEndpoint = "$baseUrl/player/index.php?data=$hash&do=getVideo"
        val postData = "hash=$hash&r=$baseUrl"

        val req = RequestBuilder()
            .url(apiEndpoint)
            .post(postData.toByteArray(Charsets.UTF_8))
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .header("x-requested-with", "XMLHttpRequest")
            .header("Referer", url)
            .header("User-Agent", Constants.DEFAULT_USER_AGENT)
            .build()

        when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> {
                val body = res.data.bodyAsString()
                try {
                    val json = JSONObject(body)
                    val videoSource = json.optString("videoSource").takeIf { it.isNotBlank() }
                    if (videoSource != null) {
                        Logger.i("[$TAG] Found videoSource: $videoSource")
                        val stream = StreamLink(
                            name = "AWSStream",
                            url = videoSource,
                            quality = source.quality,
                            host = HostType.AWS_STREAM,
                            contentType = if (videoSource.contains(".m3u8")) com.cinetheta.core.network.detector.ContentType.HLS else com.cinetheta.core.network.detector.ContentTypeDetector.detect(videoSource),
                            adaptive = videoSource.contains(".m3u8"),
                            subtitles = subtitles.distinctBy { it.url },
                            referer = url
                        )
                        return@withContext result(listOf(stream))
                    } else {
                        Logger.w("[$TAG] No videoSource in response: $body")
                    }
                } catch (e: Exception) {
                    Logger.e("[$TAG] Failed to parse JSON: ${e.message}")
                }
            }
            else -> {
                Logger.w("[$TAG] Request failed for $apiEndpoint")
            }
        }

        emptyResult()
    }
}
