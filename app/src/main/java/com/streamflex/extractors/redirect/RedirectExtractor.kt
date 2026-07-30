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

        // ---------- HDHub4u / WP Encoded Redirects ----------
        val html = document.html()
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
                lower == source.url.lowercase()
            ) {
                return@forEach
            }

            if (
                lower.contains("facebook") ||
                lower.contains("twitter") ||
                lower.contains("telegram") ||
                lower.contains("discord")
            ) {
                return@forEach
            }

            val type =
                if (
                    lower.contains("go.php") ||
                    lower.contains("download.php") ||
                    lower.contains("redirect.php") ||
                    lower.contains("gamerxyt")
                ) {
                    HostType.REDIRECT
                } else {
                    HostDetector.detect(url)
                }

            nextSources += source.copy(
                url = url,
                hostType = type
            )
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
    private fun decodeWpRedirect(combined: String): String? {
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
            val encodedUrl = com.streamflex.core.parser.JsonParser.string(root, "o")?.trim() ?: ""
            if (encodedUrl.isNotEmpty()) {
                String(decodeBase64Safe(encodedUrl), Charsets.UTF_8).trim()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}