package com.streamflex.extractors.pixeldrain

import com.streamflex.core.utils.StreamLogger
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.extractors.shared.ExtractorHelper

/**
 * PixelDrain extractor.
 *
 * ─── How PixelDrain Works ────────────────────────────────────────────────────
 * PixelDrain uses a simple public REST API — no auth, no scraping.
 *
 *   Input:  https://pixeldrain.com/u/<file_id>     (share/embed page)
 *           https://pixeldrain.com/l/<file_id>     (list view)
 *
 *   API:    GET https://pixeldrain.com/api/file/<file_id>?download
 *           → Response is the raw file stream (direct video URL).
 *
 * ─── Architecture Role ──────────────────────────────────────────────────────
 * This extractor converts PixelDrain share links into direct download/stream
 * URLs using the public `/api/file/<id>?download` endpoint.
 *
 * ─── CloudStream Reference ──────────────────────────────────────────────────
 * Mirrors the PixelDrain extractor registered in StreamPlayPlugin:
 *   - Domain: pixeldrain.com
 *   - Method: /api/file/$id?download (direct API, no JS)
 *
 * The PixelDrain API always returns the file with `Content-Disposition: attachment`.
 * For video files this is a direct playable MP4/MKV URL.
 *
 * ─── ID Extraction Patterns ─────────────────────────────────────────────────
 * From URL:
 *   https://pixeldrain.com/u/AbCdEfGh              → id = AbCdEfGh
 *   https://pixeldrain.com/api/file/AbCdEfGh       → id = AbCdEfGh
 *   https://pixeldrain.com/l/AbCdEfGh              → list id = AbCdEfGh
 */
class PixelDrainExtractor : BaseExtractor() {

    override val hostType = HostType.PIXELDRAIN

    companion object {
        private const val TAG = "PixelDrainExtractor"
        private const val BASE_URL = "https://pixeldrain.com"

        /**
         * Regex to extract the file/list ID from any PixelDrain URL.
         *
         * Matches:
         *   /u/<id>         share page
         *   /l/<id>         list page
         *   /api/file/<id>  already an API URL
         *   /api/list/<id>  list API
         */
        private val ID_REGEX = Regex(
            """/(?:u|l|api/file|api/list)/([A-Za-z0-9_-]+)"""
        )
    }

    override suspend fun extract(source: ProviderSource): ExtractionResult {

        StreamLogger.info(TAG, "Extracting PixelDrain: ${source.url}")

        // ── Step 1: Extract the file ID from the URL ─────────────────────────
        val fileId = extractFileId(source.url) ?: run {
            StreamLogger.warn(TAG, "Cannot extract file ID from: ${source.url}")
            return emptyResult()
        }

        StreamLogger.debug(TAG, "PixelDrain file ID: $fileId")

        // ── Step 2: Build the direct download API URL ─────────────────────────
        // The /api/file/<id>?download endpoint streams the raw file directly.
        // No redirect, no token, no JS — pure REST.
        val streamUrl = "$BASE_URL/api/file/$fileId?download"

        // ── Step 3: Quick HEAD check — verify the file is accessible ──────────
        // PixelDrain returns 200 for valid files, 404 for missing, 403 for
        // files that have been removed due to abuse reports.
        val exists = ExtractorHelper.exists(
            url = streamUrl,
            headers = mapOf(
                "Referer" to "$BASE_URL/",
                "User-Agent" to PIXEL_UA
            )
        )

        if (!exists) {
            StreamLogger.warn(TAG, "PixelDrain file not accessible (404/403): $fileId")
            return emptyResult()
        }

        StreamLogger.info(TAG, "PixelDrain resolved: $streamUrl")

        return result(
            createStream(
                source = source,
                url = streamUrl
            )
        )
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Extracts the PixelDrain file/list ID from any valid PixelDrain URL.
     *
     * Returns null if the URL doesn't match any known pattern.
     */
    private fun extractFileId(url: String): String? {
        // Try regex first (handles /u/, /l/, /api/file/ patterns)
        val match = ID_REGEX.find(url)
        if (match != null) return match.groupValues[1]

        // Fallback: take the last path segment (handles bare IDs)
        val lastSegment = url.trimEnd('/').substringAfterLast('/')
        return if (lastSegment.isNotBlank() && lastSegment.length >= 8) lastSegment else null
    }
}

// ─── Package-level constant ───────────────────────────────────────────────────
private const val PIXEL_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
    "AppleWebKit/537.36 (KHTML, like Gecko) " +
    "Chrome/131.0.0.0 Safari/537.36"
