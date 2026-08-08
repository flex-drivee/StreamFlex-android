package com.streamflex.extractors.dood

import com.streamflex.core.utils.StreamLogger
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.extractors.shared.ExtractorHelper
import com.streamflex.extractors.shared.ExtractorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * DoodStream extractor.
 *
 * ─── How Dood Works ─────────────────────────────────────────────────────────
 * DoodStream uses a two-step token-splice mechanism:
 *
 *   Step 1 — Fetch the embed page (e.g. https://dood.re/e/<id>)
 *             Extract the `/pass_md5/<hash>` path from the page JS.
 *
 *   Step 2 — GET `https://dood.re/pass_md5/<hash>`
 *             with `Referer: https://dood.re/e/<id>`
 *             Response body is a partial URL (e.g. `https://cdn.dood.re/.../?md5=...&expiry=...`)
 *
 *   Step 3 — Append a random 10-char alphanumeric token + current Unix timestamp:
 *             Final URL = <partial_url><random_token>?token=<md5_token>&expiry=<ts>
 *
 * ─── Architecture Role ──────────────────────────────────────────────────────
 * This extractor ONLY resolves Dood embed URLs into playable stream URLs.
 * No provider logic. No retry. No TMDB. Just Dood.
 *
 * ─── CloudStream Reference ──────────────────────────────────────────────────
 * Mirrors the DoodYtExtractor pattern from CloudStream:
 *   - Pass MD5 path extracted from `$.getScript('/pass_md5/...')`
 *   - Token spliced as: `<pass_md5_response><random_10_chars>?token=...&expiry=<ts>`
 *
 * Supported domains: dood.re, dood.la, dood.so, dood.to, dood.watch,
 *                    doods.pro, dood.yt, dood.pm, do0od.com, doodre.com
 */
class DoodExtractor : BaseExtractor() {

    override val hostType = HostType.DOOD

    companion object {
        private const val TAG = "DoodExtractor"

        /** Characters used to build the random splice token. */
        private const val TOKEN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        /** Length of the random token appended to the partial URL. */
        private const val TOKEN_LENGTH = 10

        /**
         * Regex to find the `/pass_md5/<hash>` path inside the Dood embed page JS.
         *
         * Matches patterns like:
         *   $.getScript('/pass_md5/abc123...')
         *   '/pass_md5/abc123...'
         *   "/pass_md5/abc123..."
         */
        private val PASS_MD5_REGEX = Regex("""/pass_md5/[a-zA-Z0-9/]+""")
    }

    override suspend fun extract(source: ProviderSource): ExtractionResult {

        StreamLogger.info(TAG, "Extracting Dood: ${source.url}")

        // Normalise embed URL (convert /d/ share links to /e/ embed links)
        val embedUrl = normalizeToEmbedUrl(source.url)
        val origin = extractOrigin(embedUrl) ?: run {
            StreamLogger.warn(TAG, "Cannot extract origin from: $embedUrl")
            return emptyResult()
        }

        // ── Step 1: Fetch the embed page ────────────────────────────────────
        val embedHtml = withContext(Dispatchers.IO) {
            ExtractorHelper.getText(
                url = embedUrl,
                headers = mapOf(
                    "Referer" to "$origin/",
                    "User-Agent" to USER_AGENT
                )
            )
        }

        if (embedHtml.isBlank()) {
            StreamLogger.warn(TAG, "Empty embed page for: $embedUrl")
            return emptyResult()
        }

        // ── Step 2: Extract /pass_md5/ path ─────────────────────────────────
        val passMd5Path = PASS_MD5_REGEX.find(embedHtml)?.value ?: run {
            StreamLogger.warn(TAG, "No /pass_md5/ path found in embed HTML")
            return emptyResult()
        }

        val passMd5Url = "$origin$passMd5Path"
        StreamLogger.debug(TAG, "pass_md5 URL: $passMd5Url")

        // ── Step 3: Fetch partial stream URL from /pass_md5/ ─────────────────
        val partialUrl = withContext(Dispatchers.IO) {
            ExtractorHelper.getText(
                url = passMd5Url,
                headers = mapOf(
                    "Referer" to "$embedUrl/",
                    "User-Agent" to USER_AGENT
                )
            )
        }.trim()

        if (partialUrl.isBlank() || !partialUrl.startsWith("http")) {
            StreamLogger.warn(TAG, "Invalid partial URL from pass_md5: '$partialUrl'")
            return emptyResult()
        }

        // ── Step 4: Splice token + timestamp to produce the final stream URL ──
        val randomToken = buildRandomToken()
        val expiry = System.currentTimeMillis()
        val finalUrl = "$partialUrl$randomToken?token=$randomToken&expiry=$expiry"

        StreamLogger.info(TAG, "Dood resolved stream: ${finalUrl.take(80)}...")

        return result(
            createStream(
                source = source,
                url = finalUrl
            )
        )
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Converts Dood share/download links to embed format.
     *
     * /d/<id> → /e/<id>
     * /f/<id> → /e/<id>
     */
    private fun normalizeToEmbedUrl(url: String): String {
        return url
            .replace(Regex("""/d/([a-zA-Z0-9]+)"""), "/e/$1")
            .replace(Regex("""/f/([a-zA-Z0-9]+)"""), "/e/$1")
    }

    /**
     * Returns `https://dood.re` from `https://dood.re/e/abc123`.
     */
    private fun extractOrigin(url: String): String? {
        return try {
            val uri = java.net.URI(url)
            "${uri.scheme}://${uri.host}"
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Builds a random [TOKEN_LENGTH]-character alphanumeric string.
     * Used as the splice token in Dood's final stream URL.
     */
    private fun buildRandomToken(): String {
        return (1..TOKEN_LENGTH)
            .map { TOKEN_CHARS.random() }
            .joinToString("")
    }
}

// ─── Package-level constant (shared across JVM + Android without android.* import) ───
private const val USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
    "AppleWebKit/537.36 (KHTML, like Gecko) " +
    "Chrome/131.0.0.0 Safari/537.36"
