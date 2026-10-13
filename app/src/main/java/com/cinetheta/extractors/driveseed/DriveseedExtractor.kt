package com.cinetheta.extractors.driveseed

import android.util.Log
import com.cinetheta.domain.models.ExtractionResult
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.domain.models.Quality
import com.cinetheta.domain.models.StreamLink
import com.cinetheta.extractors.common.BaseExtractor
import com.cinetheta.core.network.CineThetaHttpClient
import com.cinetheta.core.network.HttpMethod
import com.cinetheta.core.network.NetworkRequest
import com.cinetheta.core.network.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URI

open class DriveseedExtractor : BaseExtractor() {
    override val hostType: HostType = HostType.DRIVESEED
    open val name: String = "Driveseed"

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    override suspend fun extract(source: ProviderSource): ExtractionResult = withContext(Dispatchers.IO) {
        val streams = mutableListOf<StreamLink>()
        try {
            Log.d("STREAM_PIPELINE", "[$name] Starting extraction for: ${source.url}")

            val uri = try {
                URI(source.url)
            } catch (e: Exception) {
                Log.e("STREAM_PIPELINE", "[$name] Invalid URL: ${source.url}", e)
                return@withContext emptyResult()
            }

            val scheme = uri.scheme ?: "https"
            val host = uri.host ?: "driveseed.org"
            val baseUrl = "$scheme://$host"

            val res = CineThetaHttpClient.get(
                url = source.url,
                headers = mapOf("User-Agent" to userAgent),
                referer = source.referer ?: baseUrl
            )

            if (res !is NetworkResult.Success) {
                Log.e("STREAM_PIPELINE", "[$name] GET request failed: $res")
                return@withContext emptyResult()
            }

            val html = res.data.bodyAsString()
            Log.d("STREAM_PIPELINE", "[$name] HTML length: ${html.length}")
            val doc = Jsoup.parse(html, baseUrl)

            // 1. Direct video source in HTML if present
            val videoSrc = doc.selectFirst("video source")?.attr("src")
            if (!videoSrc.isNullOrBlank()) {
                val fullVideoSrc = fixUrl(videoSrc, baseUrl)
                Log.d("STREAM_PIPELINE", "[$name] Found direct video tag: $fullVideoSrc")
                streams.add(makeStream(source, fullVideoSrc, baseUrl))
            }

            // 2. ONLY for intermediate redirect pages (e.g. driveseed.org/r?key=... -> window.location.replace("/file/..."))
            // We MUST NOT run this on standard /file/ pages, otherwise any script containing "replace('/file')" breaks extraction!
            if (source.url.contains("r?key=")) {
                val replaceScript = doc.selectFirst("script:containsData(replace)")?.data()
                    ?: doc.selectFirst("script")?.data()
                    ?: html
                val replaceMatch = Regex("""replace\(\s*['"]([^'"]+)['"]\s*\)""").find(replaceScript)
                    ?: Regex("""replace\(\s*['"]([^'"]+)""").find(replaceScript)
                if (replaceMatch != null) {
                    val redirectPath = replaceMatch.groupValues[1].replace("\\/", "/")
                    if (redirectPath.isNotBlank() && redirectPath != "/file" && redirectPath != "/file/") {
                        val fullRedirectUrl = fixUrl(redirectPath, baseUrl)
                        Log.d("STREAM_PIPELINE", "[$name] Found JS replace redirect to: $fullRedirectUrl")
                        return@withContext next(source.copy(url = fullRedirectUrl, hostType = hostType))
                    }
                }
            }

            // 3. File page handling: inspect action buttons
            val qualityText = doc.selectFirst("li.list-group-item:contains(Name), ul > li.list-group-item")?.text().orEmpty()
            val detectedQuality = detectQuality(qualityText)

            val buttons = doc.select("div.text-center > a, a.btn, a.btn-success, a.btn-warning, a.btn-danger, a.btn-primary, a.download-btn, a[href*=download]")
            Log.d("STREAM_PIPELINE", "[$name] Found ${buttons.size} action buttons")

            for (btn in buttons) {
                val text = btn.text().trim()
                val href = btn.attr("href").trim()
                if (href.isBlank() || href.startsWith("#") || href.startsWith("javascript:")) continue
                if (text.contains("Login", ignoreCase = true) || href.contains("/login")) continue
                val fullHref = fixUrl(href, baseUrl)
                Log.d("STREAM_PIPELINE", "[$name] Processing button: '$text' -> $fullHref")

                // A. Instant Download
                if (text.contains("Instant", ignoreCase = true)) {
                    val instantStreamUrl = resolveInstantLink(fullHref, source.url, userAgent)
                    if (!instantStreamUrl.isNullOrBlank()) {
                        Log.d("STREAM_PIPELINE", "[$name] Instant download resolved: $instantStreamUrl")
                        streams.add(makeStream(source, instantStreamUrl, baseUrl, detectedQuality))
                    }
                }
                // B. Resume Worker Bot
                else if ((text.contains("Resume", ignoreCase = true) && text.contains("Bot", ignoreCase = true)) || text.contains("Worker", ignoreCase = true)) {
                    val resumeBotUrl = resolveResumeBot(fullHref, userAgent)
                    if (!resumeBotUrl.isNullOrBlank()) {
                        Log.d("STREAM_PIPELINE", "[$name] Resume Worker Bot resolved: $resumeBotUrl")
                        streams.add(makeStream(source, resumeBotUrl, baseUrl, detectedQuality))
                    }
                }
                // C. Direct Links (CFType1)
                else if (text.contains("Direct", ignoreCase = true)) {
                    val directLinks = resolveDirectLinks(fullHref, baseUrl, source.url, userAgent)
                    for (dLink in directLinks) {
                        Log.d("STREAM_PIPELINE", "[$name] Direct Links resolved: $dLink")
                        streams.add(makeStream(source, dLink, baseUrl, detectedQuality))
                    }
                }
                // D. Resume Cloud
                else if (text.contains("Resume", ignoreCase = true)) {
                    val resumeStreamUrl = resolveResumeCloudLink(fullHref, baseUrl, source.url, userAgent)
                    if (!resumeStreamUrl.isNullOrBlank()) {
                        Log.d("STREAM_PIPELINE", "[$name] Resume Cloud resolved: $resumeStreamUrl")
                        streams.add(makeStream(source, resumeStreamUrl, baseUrl, detectedQuality))
                    }
                }
                // E. Fast / Cloud Download Links
                else if (text.contains("Cloud", ignoreCase = true) || text.contains("Fast", ignoreCase = true)) {
                    if (fullHref.endsWith(".mkv") || fullHref.endsWith(".mp4") || fullHref.endsWith(".m3u8")) {
                        Log.d("STREAM_PIPELINE", "[$name] Direct media link found: $fullHref")
                        streams.add(makeStream(source, fullHref, baseUrl, detectedQuality))
                    } else {
                        val cloudStreamUrl = resolveResumeCloudLink(fullHref, baseUrl, source.url, userAgent)
                            ?: resolveInstantLink(fullHref, source.url, userAgent)
                        if (!cloudStreamUrl.isNullOrBlank()) {
                            Log.d("STREAM_PIPELINE", "[$name] Cloud resolved: $cloudStreamUrl")
                            streams.add(makeStream(source, cloudStreamUrl, baseUrl, detectedQuality))
                        }
                    }
                }
            }

            // 4. Fallback: check for API fetch logic directly on page
            if (streams.isEmpty()) {
                val fallbackStreamUrl = resolveDownloadApiFromHtml(html, baseUrl, source.url, userAgent)
                if (!fallbackStreamUrl.isNullOrBlank()) {
                    Log.d("STREAM_PIPELINE", "[$name] Fallback API resolved: $fallbackStreamUrl")
                    streams.add(makeStream(source, fallbackStreamUrl, baseUrl, detectedQuality))
                }
            }

            // 5. Fallback: try mfile instant API using file key
            if (streams.isEmpty()) {
                val mainKeyMatch = Regex("""key["']?\s*:\s*["']([a-zA-Z0-9]+)["']""").find(html)
                    ?: Regex("""/file/([a-zA-Z0-9]+)""").find(source.url)
                if (mainKeyMatch != null) {
                    val fileKey = mainKeyMatch.groupValues[1]
                    val mfileUrl = "$baseUrl/mfile/$fileKey"
                    Log.d("STREAM_PIPELINE", "[$name] Trying fallback mfile API: $mfileUrl")
                    val mfileRes = CineThetaHttpClient.postForm(
                        url = mfileUrl,
                        form = mapOf(
                            "action" to "instant",
                            "key" to fileKey,
                            "action_token" to ""
                        ),
                        headers = mapOf(
                            "User-Agent" to userAgent,
                            "x-token" to host,
                            "Referer" to source.url
                        ),
                        referer = source.url
                    )
                    if (mfileRes is NetworkResult.Success) {
                        val json = mfileRes.data.bodyAsString()
                        val directUrl = JSONObject(json).optString("url").takeIf { it.isNotBlank() }
                            ?: JSONObject(json).optString("visit_url").takeIf { it.isNotBlank() }
                        if (!directUrl.isNullOrBlank()) {
                            streams.add(makeStream(source, directUrl.replace("\\/", ""), baseUrl, detectedQuality))
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("STREAM_PIPELINE", "[$name] Exception caught", e)
        }

        if (streams.isNotEmpty()) {
            Log.d("STREAM_PIPELINE", "[$name] Returning ${streams.size} streams")
            result(streams)
        } else {
            Log.d("STREAM_PIPELINE", "[$name] Returning emptyResult")
            emptyResult()
        }
    }

    private fun detectQuality(str: String): Quality {
        val q = Regex("""(\d{3,4})[pP]""").find(str)?.groupValues?.getOrNull(1)?.toIntOrNull()
        return when (q) {
            2160 -> Quality.P2160
            1440 -> Quality.P1440
            1080 -> Quality.P1080
            720 -> Quality.P720
            480 -> Quality.P480
            360 -> Quality.P360
            else -> com.cinetheta.core.network.detector.QualityDetector.detect(str)
        }
    }

    private fun makeStream(source: ProviderSource, streamUrl: String, baseUrl: String, quality: Quality? = null): StreamLink {
        val uri = try { URI(streamUrl) } catch (_: Exception) { null }
        val streamHost = uri?.host ?: ""
        val streamReferer = if (streamHost.contains("driveseed") || streamHost.contains("driveleech")) baseUrl else null
        val headers = mutableMapOf("User-Agent" to userAgent)
        if (!streamReferer.isNullOrBlank()) {
            headers["Referer"] = streamReferer
        }
        return createStream(source, streamUrl, quality = quality).copy(
            headers = headers,
            referer = streamReferer
        )
    }

    private suspend fun resolveDirectLinks(url: String, baseUrl: String, referer: String, ua: String): List<String> {
        return runCatching {
            val results = mutableListOf<String>()
            for (type in listOf("1", "2")) {
                val targetUrl = if (url.contains("?")) "$url&type=$type" else "$url?type=$type"
                val res = CineThetaHttpClient.get(targetUrl, headers = mapOf("User-Agent" to ua), referer = referer)
                if (res is NetworkResult.Success) {
                    val doc = Jsoup.parse(res.data.bodyAsString(), baseUrl)
                    doc.select("a.btn-success").forEach {
                        val href = it.attr("href")
                        if (href.startsWith("http")) results.add(href)
                    }
                }
            }
            results
        }.getOrElse { emptyList() }
    }

    private suspend fun resolveInstantLink(btnUrl: String, referer: String, ua: String): String? {
        try {
            Log.d("STREAM_PIPELINE", "[$name] Resolving Instant link: $btnUrl")
            if (btnUrl.endsWith(".mkv") || btnUrl.endsWith(".mp4") || btnUrl.endsWith(".m3u8")) {
                Log.d("STREAM_PIPELINE", "[$name] Instant URL is direct media link: $btnUrl")
                return btnUrl
            }

            val followRes = CineThetaHttpClient.get(
                url = btnUrl,
                headers = mapOf("User-Agent" to ua),
                referer = referer
            )
            if (followRes !is NetworkResult.Success) {
                Log.w("STREAM_PIPELINE", "[$name] Failed to fetch Instant link: $btnUrl")
                return null
            }

            val finalUrl = followRes.data.url
            val body = followRes.data.bodyAsString()
            Log.d("STREAM_PIPELINE", "[$name] Instant finalUrl: $finalUrl, bodyLength: ${body.length}")

            if (finalUrl.endsWith(".mkv") || finalUrl.endsWith(".mp4") || finalUrl.endsWith(".m3u8")) {
                return finalUrl
            }

            var targetUrl: String? = if (finalUrl.contains("?url=")) finalUrl else null

            if (targetUrl == null) {
                val jsMatch = Regex("""replace\(\s*["']([^"']+)["']""").find(body)?.groupValues?.getOrNull(1)
                    ?: Regex("""(?:window\.)?location(?:\.href)?\s*=\s*["']([^"']+)["']""").find(body)?.groupValues?.getOrNull(1)
                    ?: Regex("""<meta[^>]*http-equiv=["']refresh["'][^>]*content=["'][^"']*url=([^"']+)["']""", RegexOption.IGNORE_CASE).find(body)?.groupValues?.getOrNull(1)

                if (!jsMatch.isNullOrBlank()) {
                    val fixed = fixUrl(jsMatch, getBaseUrl(finalUrl))
                    Log.d("STREAM_PIPELINE", "[$name] Instant found redirect in body: $fixed")
                    if (fixed.contains("?url=")) {
                        targetUrl = fixed
                    } else if (fixed.endsWith(".mkv") || fixed.endsWith(".mp4") || fixed.endsWith(".m3u8")) {
                        return fixed
                    }
                }

                if (targetUrl == null) {
                    val doc = Jsoup.parse(body, finalUrl)
                    val linkElem = doc.selectFirst("a[href*='?url='], a.btn[href*='http'], a[href*='download'], a[href*='video-']")
                    val href = linkElem?.attr("href")
                    if (!href.isNullOrBlank()) {
                        val fixedHref = fixUrl(href, getBaseUrl(finalUrl))
                        if (fixedHref.contains("?url=")) {
                            targetUrl = fixedHref
                        } else if (fixedHref.endsWith(".mkv") || fixedHref.endsWith(".mp4") || fixedHref.endsWith(".m3u8")) {
                            return fixedHref
                        }
                    }
                }
            }

            if (targetUrl == null) {
                Log.w("STREAM_PIPELINE", "[$name] Could not find ?url= or video in Instant link: $btnUrl")
                return null
            }

            val extracted = targetUrl.substringAfter("?url=", "").substringBefore("&")
            if (extracted.startsWith("http://") || extracted.startsWith("https://")) {
                Log.d("STREAM_PIPELINE", "[$name] Instant direct link extracted: $extracted")
                return extracted
            }

            if (extracted.isNotBlank()) {
                val domain = when {
                    targetUrl.contains("video-leech") -> "video-leech.xyz"
                    targetUrl.contains("video-plex") -> "video-plex.xyz"
                    targetUrl.contains("video-gen") -> "video-gen.xyz"
                    else -> "video-seed.xyz"
                }
                val apiUrl = "https://$domain/api"
                Log.d("STREAM_PIPELINE", "[$name] Calling Instant API: $apiUrl with token: $extracted")

                val apiRes = CineThetaHttpClient.postForm(
                    url = apiUrl,
                    form = mapOf("keys" to extracted),
                    headers = mapOf(
                        "x-token" to domain,
                        "User-Agent" to ua,
                        "Referer" to targetUrl,
                        "Origin" to "https://$domain",
                        "X-Requested-With" to "XMLHttpRequest"
                    ),
                    referer = targetUrl
                )

                if (apiRes is NetworkResult.Success) {
                    val json = apiRes.data.bodyAsString()
                    Log.d("STREAM_PIPELINE", "[$name] Instant API response: $json")
                    val directUrl = JSONObject(json).optString("url").replace("\\/", "")
                    if (directUrl.isNotBlank() && directUrl.startsWith("http")) return directUrl
                }
            }
        } catch (e: Exception) {
            Log.e("STREAM_PIPELINE", "[$name] resolveInstantLink error", e)
        }
        return null
    }

    private suspend fun resolveResumeBot(url: String, ua: String): String? {
        try {
            Log.d("STREAM_PIPELINE", "[$name] Resolving ResumeBot link: $url")
            val res = CineThetaHttpClient.get(url, headers = mapOf("User-Agent" to ua), referer = url)
            if (res !is NetworkResult.Success) return null
            val html = res.data.bodyAsString()
            val ssid = res.data.cookies["PHPSESSID"].orEmpty()
            val token = Regex("""formData\.append\(['"]token['"],\s*['"]([a-f0-9]+)['"]\)""").find(html)?.groupValues?.getOrNull(1).orEmpty()
            val path = Regex("""fetch\(['"]/download\?id=([a-zA-Z0-9/+]+)['"]""").find(html)?.groupValues?.getOrNull(1).orEmpty()
            val baseUrl = url.substringBefore("/download")

            if (token.isEmpty() || path.isEmpty()) {
                Log.w("STREAM_PIPELINE", "[$name] ResumeBot missing token ($token) or path ($path)")
                return null
            }

            Log.d("STREAM_PIPELINE", "[$name] Calling ResumeBot download API: $baseUrl/download?id=$path")
            val headers = mutableMapOf(
                "Accept" to "*/*",
                "Origin" to baseUrl,
                "Sec-Fetch-Site" to "same-origin",
                "User-Agent" to ua
            )
            if (ssid.isNotEmpty()) {
                headers["Cookie"] = "PHPSESSID=$ssid"
            }
            val postRes = CineThetaHttpClient.postForm(
                url = "$baseUrl/download?id=$path",
                form = mapOf("token" to token),
                headers = headers,
                referer = url
            )
            if (postRes is NetworkResult.Success) {
                val json = JSONObject(postRes.data.bodyAsString())
                val streamUrl = json.optString("url").replace("\\/", "")
                if (streamUrl.startsWith("http")) {
                    Log.d("STREAM_PIPELINE", "[$name] ResumeBot resolved: $streamUrl")
                    return streamUrl
                }
            }
        } catch (e: Exception) {
            Log.e("STREAM_PIPELINE", "[$name] resolveResumeBot error", e)
        }
        return null
    }

    private suspend fun resolveResumeCloudLink(resumeUrl: String, baseUrl: String, referer: String, ua: String): String? {
        try {
            Log.d("STREAM_PIPELINE", "[$name] Resolving Resume Cloud link: $resumeUrl")
            val fullResumeUrl = fixUrl(resumeUrl, baseUrl)
            val uri = try { URI(fullResumeUrl) } catch (_: Exception) { null }
            val resolvedBase = if (uri != null) "${uri.scheme ?: "https"}://${uri.host ?: "driveseed.org"}" else baseUrl
            val host = uri?.host ?: "driveseed.org"

            // Single GET with redirects followed
            val res = CineThetaHttpClient.get(
                url = fullResumeUrl,
                headers = mapOf("User-Agent" to ua),
                referer = referer
            )
            if (res !is NetworkResult.Success) return null

            val finalUrl = res.data.url
            if (finalUrl.startsWith("http") && !finalUrl.contains("/zfile/") && !finalUrl.contains("/file/")) {
                if (finalUrl.endsWith(".mp4") || finalUrl.endsWith(".mkv") || finalUrl.endsWith(".m3u8")) {
                    Log.d("STREAM_PIPELINE", "[$name] Resume Cloud redirected directly to: $finalUrl")
                    return finalUrl
                }
            }

            val html = res.data.bodyAsString()
            val doc = Jsoup.parse(html, resolvedBase)

            // A. Look for direct download link element in DOM (matching CloudStream: doc.selectFirst("a.btn-success")?.attr("href"))
            val directBtn = doc.selectFirst("a.btn-success, a.download-btn, a[href*='download'], a[href*='worker']")
            val directHref = directBtn?.attr("href")
            if (!directHref.isNullOrBlank()) {
                val fullDirect = fixUrl(directHref, resolvedBase)
                if (fullDirect.startsWith("http") && !fullDirect.contains("/zfile/") && !fullDirect.contains("/file/")) {
                    Log.d("STREAM_PIPELINE", "[$name] Direct download link resolved: $fullDirect")
                    return fullDirect
                }
            }

            // A2. Look for Instant Download button on Resume Cloud page
            val instantBtn = doc.selectFirst("a:contains(Instant)")
            val instantHref = instantBtn?.attr("href")
            if (!instantHref.isNullOrBlank()) {
                val fullInstant = fixUrl(instantHref, resolvedBase)
                val resolvedInstant = resolveInstantLink(fullInstant, fullResumeUrl, ua)
                if (!resolvedInstant.isNullOrBlank()) {
                    Log.d("STREAM_PIPELINE", "[$name] Resume Cloud resolved via Instant button: $resolvedInstant")
                    return resolvedInstant
                }
            }

            // B. Look for generate("cloud") POST API on the page
            val keyMatch = Regex("""formData\.append\(['"]key['"],\s*['"]([^'"]+)['"]\)""").find(html)
                ?: Regex("""['"]key['"]\s*:\s*['"]([^'"]+)['"]""").find(html)
            val actionMatch = Regex("""formData\.append\(['"]action['"],\s*(?:action|['"]([^'"]+)['"])\)""").find(html)
                ?: Regex("""generate\(['"]([^'"]+)['"]\)""").find(html)

            if (keyMatch != null) {
                val key = keyMatch.groupValues[1]
                val action = actionMatch?.groupValues?.get(1)?.takeIf { it.isNotBlank() } ?: "cloud"
                Log.d("STREAM_PIPELINE", "[$name] Calling Resume Cloud generate API: action=$action, key=$key")

                val apiRes = CineThetaHttpClient.postForm(
                    url = fullResumeUrl,
                    form = mapOf(
                        "action" to action,
                        "key" to key,
                        "action_token" to ""
                    ),
                    headers = mapOf(
                        "User-Agent" to ua,
                        "x-token" to host,
                        "Referer" to fullResumeUrl,
                        "Origin" to resolvedBase,
                        "X-Requested-With" to "XMLHttpRequest"
                    ),
                    referer = fullResumeUrl
                )

                if (apiRes is NetworkResult.Success) {
                    val json = apiRes.data.bodyAsString()
                    Log.d("STREAM_PIPELINE", "[$name] Resume Cloud API response: $json")
                    val jsonObj = JSONObject(json)
                    val directUrl = jsonObj.optString("url").takeIf { it.isNotBlank() }
                        ?: jsonObj.optString("visit_url").takeIf { it.isNotBlank() }
                    if (!directUrl.isNullOrBlank()) {
                        return directUrl.replace("\\/", "")
                    }
                }
            }

            // C. Fallback: resolveDownloadApiFromHtml
            val apiStream = resolveDownloadApiFromHtml(html, resolvedBase, fullResumeUrl, ua)
            if (!apiStream.isNullOrBlank()) return apiStream

        } catch (e: Exception) {
            Log.e("STREAM_PIPELINE", "[$name] resolveResumeCloudLink error", e)
        }
        return null
    }

    private suspend fun resolveDownloadApiFromHtml(
        html: String,
        baseUrl: String,
        referer: String,
        ua: String
    ): String? {
        try {
            val idMatch = Regex("""fetch\(['"]/download\?id=([a-zA-Z0-9/+]+)['"]""").find(html)
                ?: Regex("""fetch\(['"]/api/download\?id=([a-zA-Z0-9/+]+)['"]""").find(html)
                ?: Regex("""id\s*:\s*['"]([a-zA-Z0-9/+]+)['"]""").find(html)

            val tokenMatch = Regex("""formData\.append\(['"]token['"],\s*['"]([a-f0-9]+)['"]\)""").find(html)
                ?: Regex("""['"]token['"]\s*:\s*['"]([a-f0-9]+)['"]""").find(html)
                ?: Regex("""['"]x-token['"]\s*:\s*['"]([^'"]+)['"]""").find(html)

            if (idMatch != null && tokenMatch != null) {
                val id = idMatch.groupValues[1]
                val token = tokenMatch.groupValues[1]
                val fetchUrl = "$baseUrl/download?id=$id"
                Log.d("STREAM_PIPELINE", "[$name] Calling download API: $fetchUrl with token: $token")

                val apiRes = CineThetaHttpClient.postForm(
                    url = fetchUrl,
                    form = mapOf("token" to token),
                    headers = mapOf(
                        "User-Agent" to ua,
                        "x-token" to token,
                        "Referer" to referer
                    ),
                    referer = referer
                )

                if (apiRes is NetworkResult.Success) {
                    val json = apiRes.data.bodyAsString()
                    Log.d("STREAM_PIPELINE", "[$name] Download API response: $json")
                    val directUrl = JSONObject(json).optString("url").replace("\\/", "")
                    if (directUrl.isNotBlank()) return directUrl
                }
            }
        } catch (e: Exception) {
            Log.e("STREAM_PIPELINE", "[$name] resolveDownloadApiFromHtml error", e)
        }
        return null
    }

    private fun fixUrl(url: String, baseUrl: String): String {
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        val base = baseUrl.removeSuffix("/")
        val path = if (url.startsWith("/")) url else "/$url"
        return "$base$path"
    }

    private fun getBaseUrl(url: String): String {
        return try {
            val uri = URI(url)
            "${uri.scheme ?: "https"}://${uri.host}"
        } catch (_: Exception) {
            url
        }
    }
}
