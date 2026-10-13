package com.cinetheta.extractors.thenaukriadda

import android.util.Log
import com.cinetheta.domain.models.ExtractionResult
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.extractors.common.BaseExtractor
import com.cinetheta.core.network.CineThetaHttpClient
import com.cinetheta.core.network.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.FormBody
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URI

class TheNaukriAddaExtractor : BaseExtractor() {
    override val hostType = HostType.THENAUKRIADDA

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    private data class ManualResponse(
        val code: Int,
        val url: String,
        val headers: Headers,
        val body: String
    ) {
        val document: Document by lazy { Jsoup.parse(body, url) }
    }

    override suspend fun extract(source: ProviderSource): ExtractionResult = withContext(Dispatchers.IO) {
        try {
            Log.d("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Starting extraction for: ${source.url}")
            val bypassedUrl = bypassHrefli(source.url)
            if (!bypassedUrl.isNullOrBlank()) {
                Log.d("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Successfully bypassed shortener: $bypassedUrl")
                val host = when {
                    bypassedUrl.contains("driveleech") -> HostType.DRIVELEECH
                    bypassedUrl.contains("driveseed") || bypassedUrl.contains("video-seed") || bypassedUrl.contains("video-leech") || bypassedUrl.contains("video-plex") -> HostType.DRIVESEED
                    else -> HostType.THENAUKRIADDA
                }
                if (host != HostType.THENAUKRIADDA) {
                    return@withContext next(source.copy(url = bypassedUrl, hostType = host))
                } else {
                    if (bypassedUrl.endsWith(".mp4") || bypassedUrl.endsWith(".mkv") || bypassedUrl.contains("m3u8")) {
                        return@withContext result(createStream(source, bypassedUrl))
                    }
                }
            } else {
                Log.w("STREAM_PIPELINE", "[TheNaukriAddaExtractor] bypassHrefli returned null for: ${source.url}")
            }
        } catch (e: Exception) {
            Log.e("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Exception caught", e)
        }
        Log.d("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Returning emptyResult")
        emptyResult()
    }

    private suspend fun bypassHrefli(url: String): String? {
        val host = getBaseUrl(url)
        val jar = LinkedHashMap<String, String>()
        var currentUrl = url

        Log.d("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Fetching initial URL: $currentUrl")
        val initialRes = fetchManual(jar, currentUrl, isPost = false)
        var doc = initialRes.document
        var res = initialRes

        // Look specifically for landing page form containing inputs starting with _lp or _wp
        var form = doc.select("form").firstOrNull { f ->
            f.select("input").any {
                val name = it.attr("name")
                name.startsWith("_lp") || name.startsWith("_wp")
            }
        }

        var count = 0
        var lastFormData = emptyMap<String, String>()

        while (form != null && count < 5) {
            val action = form.attr("action").ifBlank { currentUrl }
            val formUrl = fixUrl(action, host)
            val formData = form.select("input").associate { it.attr("name") to it.attr("value") }
            if (formData.isEmpty()) break

            lastFormData = formData
            count++
            Log.d("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Hop $count: Submitting form to: $formUrl with ${formData.size} fields")
            val postRes = fetchManual(jar, formUrl, isPost = true, postData = formData, referer = currentUrl)
            currentUrl = formUrl
            res = postRes
            doc = postRes.document

            form = doc.select("form").firstOrNull { f ->
                f.select("input").any {
                    val name = it.attr("name")
                    name.startsWith("_lp") || name.startsWith("_wp")
                }
            }
        }

        val docText = res.body

        // 1. Check for lp_go token
        val token = Regex("""lp_go=(lp-[a-zA-Z0-9]+)""").find(docText)?.groupValues?.getOrNull(1)
        if (token != null) {
            Log.d("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Found lp_go token: $token")
            val cookieRegex = Regex("""sc\(\s*["']""" + Regex.escape(token) + """["']\s*,\s*["']([^"']+)["']""")
            val cookieVal = cookieRegex.find(docText)?.groupValues?.getOrNull(1)
            if (cookieVal != null) {
                val cleanVal = cookieVal.replace("""\/""", "/").replace(" ", "+").trim()
                jar[token] = cleanVal
                val targetUrl = "$host/?lp_go=$token"
                Log.d("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Requesting targetUrl with cookie: $targetUrl")
                val targetRes = fetchManual(jar, targetUrl, isPost = false, referer = currentUrl)
                val targetBody = targetRes.body

                val destination = Regex("""replace\(\s*["']([^"']+)["']\s*\)""").find(targetBody)?.groupValues?.getOrNull(1)
                    ?: Regex("""replace\(\s*["']([^"']+)["']""").find(targetBody)?.groupValues?.getOrNull(1)
                    ?: targetRes.document.selectFirst("meta[http-equiv=refresh]")?.attr("content")?.substringAfter("url=")
                    ?: Regex("""window\.location(?:\.href)?\s*=\s*["']([^"']+)["']""").find(targetBody)?.groupValues?.getOrNull(1)
                    ?: Regex("""location\.href\s*=\s*["']([^"']+)["']""").find(targetBody)?.groupValues?.getOrNull(1)
                    ?: targetRes.headers["Location"]
                    ?: targetRes.headers["location"]

                if (!destination.isNullOrBlank()) {
                    val cleaned = destination.replace("""\/""", "/").trim()
                    if (cleaned != "/404") {
                        val base = targetRes.url.ifBlank { targetUrl }
                        val resolved = fixUrl(cleaned, getBaseUrl(base))
                        Log.d("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Resolved destination from targetBody: $resolved")
                        return resolved
                    }
                }

                if (targetRes.url.isNotBlank() && targetRes.url != targetUrl && !targetRes.url.contains("lp_go=")) {
                    Log.d("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Resolved targetRes.url: ${targetRes.url}")
                    return targetRes.url
                }
            }
        }

        // 2. Check for script containing ?go=
        val scriptElement = doc.selectFirst("script:containsData(?go=)")
        val skToken = scriptElement?.data()?.substringAfter("?go=", "")?.substringBefore("\"", "")?.substringBefore("'", "")?.takeIf { it.isNotBlank() }
        if (skToken != null) {
            Log.d("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Found skToken: $skToken")
            val wpHttp = lastFormData["_wp_http2"] ?: ""
            if (wpHttp.isNotBlank()) {
                jar[skToken] = wpHttp
            }
            val driveUrl = "$host/?go=$skToken"
            val driveRes = fetchManual(jar, driveUrl, isPost = false, referer = currentUrl)
            val path = driveRes.document.selectFirst("meta[http-equiv=refresh]")?.attr("content")?.substringAfter("url=") ?: driveRes.url
            if (path.isNotBlank()) {
                val getRes = CineThetaHttpClient.get(
                    url = path,
                    headers = mapOf("User-Agent" to userAgent)
                )
                if (getRes is NetworkResult.Success) {
                    val body = getRes.data.bodyAsString()
                    val finalPath = body.substringAfter("replace(\"", "").substringBefore("\")", "").replace("""\/""", "/")
                    if (finalPath != "/404" && finalPath.isNotBlank()) {
                        val resolved = fixUrl(finalPath, getBaseUrl(path))
                        Log.d("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Resolved destination from ?go=: $resolved")
                        return resolved
                    }
                }
            }
        }

        // 3. Check for direct replace or meta refresh in docText
        val directDest = Regex("""replace\(\s*["']([^"']+)["']""").find(docText)?.groupValues?.getOrNull(1)
            ?: doc.selectFirst("meta[http-equiv=refresh]")?.attr("content")?.substringAfter("url=")
        if (!directDest.isNullOrBlank()) {
            val cleaned = directDest.replace("""\/""", "/").trim()
            if (cleaned != "/404" && !cleaned.contains("{") && !cleaned.contains("}") && (cleaned.startsWith("http") || cleaned.startsWith("/"))) {
                val resolved = fixUrl(cleaned, host)
                Log.d("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Resolved direct destination: $resolved")
                return resolved
            }
        }

        // 4. Fallback search for Driveleech or Driveseed links in page
        for (link in doc.select("a[href]")) {
            val href = link.attr("href")
            if (href.contains("driveleech") || href.contains("driveseed")) {
                Log.d("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Found fallback href in document: $href")
                return href
            }
        }

        return null
    }

    private fun fetchManual(
        jar: MutableMap<String, String>,
        fetchUrl: String,
        isPost: Boolean = false,
        postData: Map<String, String>? = null,
        referer: String? = null
    ): ManualResponse {
        var nextUrl = fetchUrl
        var usePost = isPost
        var lastRes: ManualResponse? = null
        var i = 0

        val noRedirectClient = CineThetaHttpClient.okHttpClient.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()

        while (i < 10) {
            val hdrs = mutableMapOf("User-Agent" to userAgent)
            if (referer != null) hdrs["Referer"] = referer
            val ck = jar.entries.joinToString("; ") { "${it.key}=${it.value}" }
            if (ck.isNotEmpty()) hdrs["Cookie"] = ck

            val reqBuilder = Request.Builder().url(nextUrl)
            hdrs.forEach { (k, v) -> reqBuilder.header(k, v) }

            if (usePost && postData != null) {
                val formBody = FormBody.Builder()
                postData.forEach { (k, v) -> formBody.add(k, v) }
                reqBuilder.post(formBody.build())
            } else {
                reqBuilder.get()
            }

            val resp = noRedirectClient.newCall(reqBuilder.build()).execute()
            val code = resp.code
            val headers = resp.headers
            val body = resp.body?.string() ?: ""
            val effectiveUrl = resp.request.url.toString()

            absorbHeaders(jar, headers)
            lastRes = ManualResponse(code, effectiveUrl, headers, body)

            if (code in 300..399) {
                val location = headers["Location"] ?: headers["location"] ?: break
                nextUrl = fixUrl(location, getBaseUrl(nextUrl))
                usePost = false
                i++
                continue
            } else {
                break
            }
        }
        return lastRes ?: ManualResponse(500, fetchUrl, Headers.headersOf(), "")
    }

    private fun absorbHeaders(jar: MutableMap<String, String>, headers: Headers) {
        for (setCookie in headers.values("Set-Cookie")) {
            val str = setCookie.split(";").firstOrNull()?.trim() ?: continue
            val eqIdx = str.indexOf('=')
            if (eqIdx > 0) {
                val k = str.substring(0, eqIdx).trim()
                val v = str.substring(eqIdx + 1).trim()
                if (!v.equals("deleted", ignoreCase = true) && v.isNotEmpty()) {
                    jar[k] = v
                } else {
                    jar.remove(k)
                }
            }
        }
    }

    private fun fixUrl(url: String, domain: String): String {
        if (url.startsWith("http")) return url
        if (url.isEmpty()) return ""
        if (url.startsWith("//")) return "https:$url"
        if (url.startsWith("/")) return "$domain$url"
        return "$domain/$url"
    }

    private fun getBaseUrl(url: String): String {
        return try {
            val uri = URI(url)
            "${uri.scheme}://${uri.host}"
        } catch (_: Exception) {
            url
        }
    }
}
