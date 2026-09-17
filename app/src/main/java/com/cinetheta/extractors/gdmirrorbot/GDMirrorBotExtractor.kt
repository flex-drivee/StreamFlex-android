package com.cinetheta.extractors.gdmirrorbot

import android.util.Base64
import com.cinetheta.core.constants.Constants
import com.cinetheta.core.logger.Logger
import com.cinetheta.core.network.HttpClient
import com.cinetheta.core.network.NetworkResult
import com.cinetheta.core.network.RequestBuilder
import com.cinetheta.core.network.detector.HostDetector
import com.cinetheta.domain.models.ExtractionResult
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.domain.models.StreamLink
import com.cinetheta.extractors.common.BaseExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URI

/**
 * GDMirrorBot Extractor (and derived domains: stream.techinmind.space, filesforever.link, pro.iqsmartgames.com).
 *
 * Implements the full CloudStream reference flow:
 * 1. GET player page HTML to extract FinalID, myKey, idType, baseUrl
 * 2. Query /myseriesapi (for TV) or /mymovieapi (for Movie) to obtain fileslug/sid
 * 3. POST to /embedhelper.php with "sid=$sid" to receive siteUrls, siteFriendlyNames, and mresult
 * 4. Base64-decode mresult to reconstruct mirror URLs (StreamHG, EarnVids, UpnShare, etc.)
 */
class GDMirrorBotExtractor : BaseExtractor() {

    override val hostType = HostType.GDMIRRORBOT

    companion object {
        private const val TAG = "GDMirrorBotExtractor"
        private val FINAL_ID_REGEX = Regex("""FinalID\s*=\s*['"]([^'"]+)['"]""")
        private val MY_KEY_REGEX   = Regex("""myKey\s*=\s*['"]([^'"]+)['"]""")
        private val ID_TYPE_REGEX  = Regex("""idType\s*=\s*['"]([^'"]+)['"]""")
        private val BASE_URL_REGEX = Regex("""(?:let|var|const)?\s*baseUrl\s*=\s*['"]([^'"]+)['"]""")
        private val TV_SEASON_REGEX = Regex("""/tv/\d+/(\d+)/""")
        private val TV_EP_REGEX     = Regex("""/tv/\d+/\d+/(\d+)""")
    }

    override suspend fun extract(source: ProviderSource): ExtractionResult = withContext(Dispatchers.IO) {
        val url = source.url.trim()
        val hostBaseUrl = try {
            val uri = URI(url)
            "${uri.scheme}://${uri.host}"
        } catch (_: Exception) {
            "https://gdmirrorbot.nl"
        }

        Logger.d("[$TAG] Extracting from: $url")

        // Step 1: Fetch player page
        val pageReq = RequestBuilder()
            .url(url)
            .header("Referer", source.referer ?: hostBaseUrl)
            .header("User-Agent", Constants.DEFAULT_USER_AGENT)
            .build()

        val pageHtml = when (val res = HttpClient.execute(pageReq)) {
            is NetworkResult.Success -> res.data.bodyAsString()
            else -> {
                Logger.w("[$TAG] Failed to load page: $url")
                return@withContext emptyResult()
            }
        }

        val finalId = FINAL_ID_REGEX.find(pageHtml)?.groupValues?.get(1)
        val myKey = MY_KEY_REGEX.find(pageHtml)?.groupValues?.get(1)
        val idType = ID_TYPE_REGEX.find(pageHtml)?.groupValues?.get(1) ?: "imdbid"
        val apiBaseUrl = BASE_URL_REGEX.find(pageHtml)?.groupValues?.get(1)?.trimEnd('/') ?: hostBaseUrl

        if (finalId.isNullOrBlank() || myKey.isNullOrBlank()) {
            Logger.w("[$TAG] Missing FinalID or myKey in $url")
            val innerIframe = Regex("""<iframe[^>]+(?:src|data-src)=['"]([^'"]+)['"]""").find(pageHtml)?.groupValues?.get(1)
            if (!innerIframe.isNullOrBlank()) {
                val resolvedInner = if (innerIframe.startsWith("//")) "https:$innerIframe" else innerIframe
                val innerHost = HostDetector.detect(resolvedInner)
                return@withContext result(emptyList(), listOf(
                    ProviderSource(
                        provider = source.provider.ifBlank { "Filesforever" },
                        host = innerHost.name,
                        hostType = innerHost,
                        url = resolvedInner,
                        referer = url
                    )
                ))
            }
            // Fallback: check if pageHtml contains direct m3u8
            val m3u8Match = Regex("""["'](https?://[^"'\s<>]+\.m3u8[^"'\s<>]*)["']""").find(pageHtml)?.groupValues?.get(1)
            return@withContext if (m3u8Match != null) {
                result(listOf(StreamLink(name = "GDMirror Direct", url = m3u8Match, host = HostType.GDMIRRORBOT, referer = url)))
            } else {
                emptyResult()
            }
        }

        // Step 2: Determine API endpoint
        val apiUrl = if (url.contains("/tv/")) {
            val season = TV_SEASON_REGEX.find(url)?.groupValues?.get(1) ?: "1"
            val epname = TV_EP_REGEX.find(url)?.groupValues?.get(1) ?: "1"
            "$apiBaseUrl/myseriesapi?tmdbid=$finalId&season=$season&epname=$epname&key=$myKey"
        } else {
            "$apiBaseUrl/mymovieapi?$idType=$finalId&key=$myKey"
        }

        Logger.d("[$TAG] Fetching metadata API: $apiUrl")
        val apiReq = RequestBuilder()
            .url(apiUrl)
            .header("Referer", url)
            .header("User-Agent", Constants.DEFAULT_USER_AGENT)
            .build()

        val apiJsonStr = when (val res = HttpClient.execute(apiReq)) {
            is NetworkResult.Success -> res.data.bodyAsString()
            else -> {
                Logger.w("[$TAG] Metadata API request failed: $apiUrl")
                return@withContext emptyResult()
            }
        }

        val sid = try {
            val root = JSONObject(apiJsonStr)
            val dataArr = root.optJSONArray("data")
            if (dataArr != null && dataArr.length() > 0) {
                val item = dataArr.getJSONObject(0)
                item.optString("fileslug").ifBlank { item.optString("sid") }
            } else {
                root.optString("fileslug").ifBlank { root.optString("sid") }
            }
        } catch (e: Exception) {
            Logger.w("[$TAG] Failed to parse API JSON: ${e.message}")
            ""
        }

        if (sid.isBlank()) {
            Logger.w("[$TAG] No sid/fileslug found in API response")
            return@withContext emptyResult()
        }

        // Step 3: POST to /embedhelper.php with sid
        val helperUrl = "$apiBaseUrl/embedhelper.php"
        val postBody = "sid=$sid"
        val helperReq = RequestBuilder()
            .url(helperUrl)
            .post(postBody.toByteArray(Charsets.UTF_8))
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .header("X-Requested-With", "XMLHttpRequest")
            .header("Referer", url)
            .header("User-Agent", Constants.DEFAULT_USER_AGENT)
            .build()

        val helperJsonStr = when (val res = HttpClient.execute(helperReq)) {
            is NetworkResult.Success -> res.data.bodyAsString()
            else -> {
                Logger.w("[$TAG] embedhelper.php request failed")
                return@withContext emptyResult()
            }
        }

        // Step 4: Parse siteUrls, siteFriendlyNames, and decode mresult
        val streams = mutableListOf<StreamLink>()
        val nextSources = mutableListOf<ProviderSource>()

        try {
            val root = JSONObject(helperJsonStr)
            val siteUrls = root.optJSONObject("siteUrls")
            val siteFriendlyNames = root.optJSONObject("siteFriendlyNames")
            val mresultRaw = root.opt("mresult")

            val decodedMresult: JSONObject? = when (mresultRaw) {
                is JSONObject -> mresultRaw
                is String -> {
                    try {
                        val decodedBytes = Base64.decode(mresultRaw, Base64.DEFAULT)
                        JSONObject(String(decodedBytes, Charsets.UTF_8))
                    } catch (e: Exception) {
                        Logger.w("[$TAG] Failed to base64 decode mresult: ${e.message}")
                        null
                    }
                }
                else -> null
            }

            if (siteUrls != null && decodedMresult != null) {
                val keys = siteUrls.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (!decodedMresult.has(key)) continue

                    val base = siteUrls.optString(key, "").trimEnd('/')
                    val path = decodedMresult.optString(key, "").trimStart('/')
                    if (base.isBlank() || path.isBlank()) continue

                    val fullUrl = "$base/$path"
                    val friendlyName = siteFriendlyNames?.optString(key, "")?.ifBlank { key } ?: key

                    Logger.d("[$TAG] Found mirror: $friendlyName -> $fullUrl")

                    // Direct video stream (.m3u8 or .mp4)
                    if (fullUrl.contains(".m3u8") || fullUrl.contains(".mp4")) {
                        streams.add(
                            StreamLink(
                                name = "$friendlyName",
                                url = fullUrl,
                                quality = source.quality,
                                host = HostType.DIRECT,
                                referer = url
                            )
                        )
                        continue
                    }

                    // Map known mirrors to correct extractors
                    val hostType = when {
                        friendlyName.equals("EarnVids", ignoreCase = true) -> HostType.HDSTREAM4U
                        friendlyName.equals("StreamHG", ignoreCase = true) -> HostType.VIDSTACK
                        friendlyName.equals("StreamP2p", ignoreCase = true) -> HostType.VIDSTACK
                        friendlyName.equals("UpnShare", ignoreCase = true) -> HostType.VIDSTACK
                        friendlyName.equals("RpmShare", ignoreCase = true) -> HostType.VIDSTACK
                        else -> {
                            val detected = HostDetector.detect(fullUrl)
                            if (detected != HostType.UNKNOWN) detected else HostType.REDIRECT
                        }
                    }

                    nextSources.add(
                        ProviderSource(
                            provider = source.provider.ifBlank { "GDMirror" },
                            host     = friendlyName,
                            hostType = hostType,
                            url      = fullUrl,
                            quality  = source.quality,
                            referer  = url,
                            metadata = source.metadata + mapOf("mirror" to friendlyName)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Logger.e("[$TAG] Error parsing embedhelper response: ${e.message}")
        }

        Logger.i("[$TAG] Resolved ${streams.size} stream(s) and ${nextSources.size} mirror source(s)")
        result(streams = streams, sources = nextSources)
    }
}
