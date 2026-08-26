package com.streamflex.extractors.redirect

import com.streamflex.core.network.detector.HostDetector
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.extractors.shared.ExtractorHelper
import com.streamflex.extractors.shared.ExtractorUtils

/**
 * Generic redirect resolver.
 *
 * Handles pages such as:
 *
 * • gamerxyt
 * • go.php
 * • download.php
 * • redirect.php
 * • generic HTML gateways
 *
 * It never resolves streams itself.
 * It only discovers the next URLs and lets
 * ExtractorManager continue the chain.
 */
class RedirectExtractor : BaseExtractor() {

    override val hostType = HostType.REDIRECT

    override suspend fun extract(
        source: ProviderSource
    ): ExtractionResult {

        val document = ExtractorHelper.fetchDocument(
            source.url,
            source.headers
        )

        val candidates = linkedSetOf<String>()

        // ---------- Links ----------

        document.select(
            "a.btn, a[class*=btn], a[href]"
        ).forEach {

            val url = it.absUrl("href")

            if (url.isNotBlank()) {
                candidates += url
            }
        }

        // ---------- Video ----------

        document.select(
            "video source[src]"
        ).forEach {

            val url = it.absUrl("src")

            if (url.isNotBlank()) {
                candidates += url
            }
        }

        // ---------- iframe ----------

        document.select(
            "iframe[src]"
        ).forEach {

            val url = it.absUrl("src")

            if (url.isNotBlank()) {
                candidates += url
            }
        }

        // ---------- JavaScript ----------

        document.select("script")
            .forEach {

                ExtractorUtils
                    .allMatches(
                        """https?:\/\/[^\s"'<>\\]+""",
                        it.data()
                    )
                    .forEach(candidates::add)
            }

        val html = document.html()

        // ---------- JavaScript / Meta Refresh Redirects ----------
        val jsMatch = Regex("""window\.location\.href\s*=\s*['"]([^'"]+)['"]""").find(html)
            ?: Regex("""location(?:\.href)?\s*=\s*['"]([^'"]+)['"]""").find(html)
        jsMatch?.groups?.get(1)?.value?.let { jsUrl ->
            if (jsUrl.startsWith("http")) candidates += jsUrl
        }

        val metaMatch = Regex("""<meta[^>]*?url=['"]?([^'"\s>]+)""", RegexOption.IGNORE_CASE).find(html)
        metaMatch?.groups?.get(1)?.value?.let { metaUrl ->
            if (metaUrl.startsWith("http")) candidates += metaUrl
        }

        // ---------- HDHub4u / WP Encoded Redirects ----------
        val wpRegex = """s\('o','([A-Za-z0-9+/=]+)'|ck\('_wp_http_\d+','([^']+)'""".toRegex()
        val combinedEncoded = buildString {
            wpRegex.findAll(html).forEach { matchResult ->
                val extractedValue = matchResult.groups[1]?.value ?: matchResult.groups[2]?.value
                if (!extractedValue.isNullOrEmpty()) append(extractedValue)
            }
        }

        if (combinedEncoded.isNotEmpty()) {
            decodeWpRedirect(combinedEncoded)?.let { decodedUrl ->
                if (decodedUrl.isNotBlank() && decodedUrl.startsWith("http")) {
                    candidates += decodedUrl
                }
            }
        } else {
            // Fallback: search for single base64 tokens of length >= 50
            val allTokens = Regex("""[A-Za-z0-9+/=]{50,}""").findAll(html)
            for (tokenMatch in allTokens) {
                val decoded = decodeWpRedirect(tokenMatch.value)
                if (!decoded.isNullOrBlank() && decoded.startsWith("http")) {
                    candidates += decoded
                    break
                }
            }
        }

        // ---------- PHP Proxy Error Leaks ----------
        val phpErrorMatch = Regex("""\?url=(https?%3A%2F%2F[^"'\s<>)]+)""").find(html)
        if (phpErrorMatch != null) {
            val leakedUrl = java.net.URLDecoder.decode(phpErrorMatch.groups[1]!!.value, "UTF-8")
            candidates += leakedUrl
        }

        if (candidates.isEmpty()) {
            return ExtractionResult()
        }

        val nextSources = mutableListOf<ProviderSource>()

        candidates.forEach { url ->

            val lower = url.lowercase()

            // Skip garbage
            if (
                lower.startsWith("javascript:") ||
                lower == "#" ||
                lower == source.url.lowercase() ||
                lower.endsWith("/#main") ||
                lower.contains("/category/") ||
                lower.contains("/author/") ||
                lower.contains("/tag/") ||
                lower.contains("/page/") ||
                lower.contains("sample-page") ||
                lower.contains("/wp-") ||
                lower.contains("privacy-policy") ||
                lower.contains("terms")
            ) {
                return@forEach
            }

            // Skip bare domain homepages (e.g. https://gamerxyt.com/)
            val path = try { java.net.URL(url).path } catch (_: Exception) { "" }
            if (path.isEmpty() || path == "/" || path == "/#") {
                return@forEach
            }

            if (
                lower.contains("facebook") ||
                lower.contains("twitter") ||
                lower.contains("telegram") ||
                lower.contains("discord") ||
                lower.contains("instagram") ||
                lower.contains("whatsapp")
            ) {
                return@forEach
            }

            val type = when {
                lower.contains("hubcloud.php") || lower.contains("/drive/") || lower.contains("/file/") -> HostType.HUBCLOUD
                lower.contains("go.php") || lower.contains("download.php") || lower.contains("redirect.php") || lower.contains("?id=") -> HostType.REDIRECT
                lower.contains("gamerxyt.com") -> return@forEach // skip random blog posts on gamerxyt
                else -> HostDetector.detect(url)
            }

            if (type != HostType.UNKNOWN) {
                nextSources += source.copy(
                    url = url,
                    hostType = type
                )
            }
        }

        return ExtractionResult(
            sources = nextSources.distinctBy { it.url }
        )
    }

    /**
     * Helper to decode Base64 safely across Android devices and JVM unit tests.
     */
    private fun decodeBase64Safe(input: String): ByteArray {
        return try {
            val res = android.util.Base64.decode(input, android.util.Base64.DEFAULT)
            if (res != null && res.isNotEmpty()) res
            else java.util.Base64.getDecoder().decode(input)
        } catch (_: Exception) {
            java.util.Base64.getDecoder().decode(input)
        }
    }

    /**
     * Decodes WordPress / HDHub4u base64 + rot13 script redirects.
     * Inspired by CloudStream HDhub4u reference Utils.kt (getRedirectLinks).
     */
    fun decodeWpRedirect(combined: String): String? {
        return try {
            val b1 = decodeBase64Safe(combined)
            val b2 = decodeBase64Safe(String(b1))
            val rot13 = String(b2).map { ch ->
                when (ch) {
                    in 'A'..'Z' -> ((ch - 'A' + 13) % 26 + 'A'.code).toChar()
                    in 'a'..'z' -> ((ch - 'a' + 13) % 26 + 'a'.code).toChar()
                    else -> ch
                }
            }.joinToString("")
            val b3 = decodeBase64Safe(rot13)
            val jsonStr = String(b3, Charsets.UTF_8)
            val root = com.streamflex.core.parser.JsonParser.parse(jsonStr)

            // Method 1: "o" parameter directly holds the base64-encoded URL
            val encodedUrl = com.streamflex.core.parser.JsonParser.string(root, "o")?.trim() ?: ""
            if (encodedUrl.isNotEmpty()) {
                val direct = String(decodeBase64Safe(encodedUrl), Charsets.UTF_8).trim()
                if (direct.startsWith("http")) return direct
            }

            // Method 2: "data" + "blog_url" requires fetching ${blog_url}?re=${data}
            val data = com.streamflex.core.parser.JsonParser.string(root, "data")?.trim() ?: ""
            val blogUrl = com.streamflex.core.parser.JsonParser.string(root, "blog_url")?.trim() ?: ""
            if (data.isNotEmpty() && blogUrl.isNotEmpty()) {
                val decodedData = String(decodeBase64Safe(data), Charsets.UTF_8).trim()
                val req = com.streamflex.core.network.RequestBuilder()
                    .url("$blogUrl?re=$decodedData")
                    .build()
                return kotlinx.coroutines.runBlocking {
                    when (val resp = com.streamflex.core.network.HttpClient.execute(req)) {
                        is com.streamflex.core.network.NetworkResult.Success -> {
                            val body = resp.data.bodyAsString().trim()
                            if (body.startsWith("http")) {
                                body
                            } else {
                                Regex("""https?:\/\/[^\s"'<>\\]+""").find(body)?.value
                            }
                        }
                        else -> null
                    }
                }
            }

            null
        } catch (_: Exception) {
            null
        }
    }
}