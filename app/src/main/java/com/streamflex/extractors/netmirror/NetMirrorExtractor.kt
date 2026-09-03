package com.streamflex.extractors.netmirror

import android.net.Uri
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.JsonParser
import com.streamflex.core.utils.StreamLogger
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.Quality
import com.streamflex.domain.models.StreamLink
import com.streamflex.domain.models.Subtitle
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.providers.netmirror.NetMirrorConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * NetMirrorExtractor — v2 (Silent Mobile API)
 *
 * Replaces the old WebViewResolver-based approach with a fully automated,
 * background HTTP bypass (NetMirrorBypassManager) and uses the native
 * /mobile/ REST API endpoints instead of the Cloudflare-protected web endpoints.
 *
 * Flow:
 *   1. Parse source URI  ->  extract id, ott, baseUrl
 *   2. Call NetMirrorBypassManager.getToken() to acquire t_hash_t silently
 *   3. GET /mobile/playlist.php?id=<id>&t=<title>&tm=<unix_ts>
 *   4. Parse JSON PlayList  ->  build StreamLink list (m3u8 + subtitles)
 */
class NetMirrorExtractor : BaseExtractor() {

    override val hostType = HostType.NETMIRROR

    private companion object {
        const val TAG = "NetMirrorExtractor"
    }

    override suspend fun extract(source: ProviderSource): ExtractionResult {
        // source.url format: netmirror://player?id=$id&ott=$ott&base=$baseUrl&title=$title
        val uri     = Uri.parse(source.url)
        val id      = uri.getQueryParameter("id")    ?: return emptyResult()
        val ott     = uri.getQueryParameter("ott")   ?: return emptyResult()
        val baseUrl = uri.getQueryParameter("base")?.trimEnd('/') ?: NetMirrorConfig.DEFAULT_DOMAIN
        val title   = uri.getQueryParameter("title") ?: ""

        return withContext(Dispatchers.IO) {

            // ── Step 1: Acquire t_hash_t silently (no WebView) ───────────────
            val tHashT = NetMirrorBypassManager.getToken(baseUrl)
            if (tHashT.isNullOrBlank()) {
                StreamLogger.error(TAG, "Could not acquire t_hash_t for $baseUrl")
                return@withContext emptyResult()
            }

            // ── Step 2: Build authenticated cookie string ────────────────────
            val cookieStr = "t_hash_t=$tHashT; ott=$ott; hd=on"
            val referer   = "$baseUrl/mobile/home?app=1"
            val unixTs    = System.currentTimeMillis() / 1000L

            // ── Step 3: Fetch /mobile/playlist.php ──────────────────────────
            val playlistPath = when (ott) {
                NetMirrorConfig.OTT_PRIME   -> "/mobile/pv/playlist.php"
                NetMirrorConfig.OTT_HOTSTAR, NetMirrorConfig.OTT_DISNEY -> "/mobile/hs/playlist.php"
                else                        -> "/mobile/playlist.php"
            }
            val playlistUrl = "$baseUrl$playlistPath?id=$id&t=${Uri.encode(title)}&tm=$unixTs"
            StreamLogger.debug(TAG, "GET $playlistUrl")

            val request = RequestBuilder()
                .url(playlistUrl)
                .header("User-Agent", NetMirrorBypassManager.NATIVE_UA)
                .header("X-Requested-With", "app.netmirror.netmirrornew")
                .header("Cookie", cookieStr)
                .header("Referer", referer)
                .header("Accept", "*/*")
                .header("Accept-Language", "en-IN,en-US;q=0.9,en;q=0.8")
                .header("Connection", "keep-alive")
                .build()

            val response = HttpClient.execute(request)
            if (response !is NetworkResult.Success) {
                StreamLogger.error(TAG, "playlist.php request failed: $response")
                // Token may be stale — clear cache; next request will re-bypass
                NetMirrorBypassManager.refreshToken(baseUrl)
                return@withContext emptyResult()
            }

            val json = response.data.bodyAsString()
            StreamLogger.debug(TAG, "playlist.php response: $json")

            // ── Step 4: Parse PlayList JSON ──────────────────────────────────
            parsePlaylist(json, source, tHashT, referer)
        }
    }

    /**
     * Parses the /mobile/playlist.php JSON response into a list of StreamLinks.
     *
     * Expected structure (array of PlayList items):
     * [
     *   {
     *     "sources": [ { "file": "...m3u8?q=1080p", "label": "1080p" }, ... ],
     *     "tracks":  [ { "kind": "captions", "file": "...srt", "label": "English" }, ... ]
     *   }
     * ]
     */
    private suspend fun parsePlaylist(
        json: String,
        source: ProviderSource,
        tHashT: String,
        referer: String
    ): ExtractionResult {
        return try {
            // Use Gson-based parsing (JsonElement) for consistent API usage
            val root = JsonParser.parse(json)?.asJsonArray?.toList()
                ?: return emptyResult()

            val subtitles = mutableListOf<Subtitle>()
            val streams   = mutableListOf<StreamLink>()

            // First pass: collect subtitle tracks from all playlist items
            for (playlistItem in root) {
                val tracks = JsonParser.array(playlistItem, "tracks")
                for (track in tracks) {
                    val kind  = JsonParser.string(track, "kind")  ?: continue
                    if (kind != "captions") continue
                    val file  = JsonParser.string(track, "file")  ?: continue
                    val label = JsonParser.string(track, "label") ?: "Unknown"
                    val fileClean = file.replace("\\", "")
                    val url = if (fileClean.startsWith("//")) {
                        "https:$fileClean"
                    } else if (fileClean.startsWith("/")) {
                        val base = referer.substringBefore("/mobile")
                        "$base$fileClean"
                    } else if (fileClean.startsWith("http")) {
                        fileClean
                    } else {
                        val base = referer.substringBefore("/mobile")
                        "$base/$fileClean"
                    }
                    subtitles += Subtitle(language = label, url = url, label = label)
                }
            }

            // Second pass: build StreamLinks with subtitle list attached
            for (playlistItem in root) {
                val sources = JsonParser.array(playlistItem, "sources")
                for (src in sources) {
                    val fileRaw = JsonParser.string(src, "file")  ?: continue
                    val label   = JsonParser.string(src, "label") ?: "Auto"

                    // file can be a relative path like "/mobile/hls/...", ensure absolute URL
                    val file = if (fileRaw.startsWith("/")) {
                        val base = referer.substringBefore("/mobile")
                        "$base$fileRaw"
                    } else if (fileRaw.startsWith("http")) {
                        fileRaw
                    } else {
                        val base = referer.substringBefore("/mobile")
                        "$base/$fileRaw"
                    }

                    // Quality label comes from "label" field (e.g. "Full HD", "720p")
                    val quality = Quality.fromLabel(label)
                    
                    val ott = android.net.Uri.parse(source.url).getQueryParameter("ott") ?: "pv"
                    val finalUrl = file

                    streams += StreamLink(
                        name        = "${source.provider} - $label",
                        url         = finalUrl,
                        host        = HostType.M3U8,
                        contentType = com.streamflex.core.network.detector.ContentType.M3U8,
                        headers     = mapOf(
                            "Cookie"     to "hd=on; t_hash_t=$tHashT; ott=$ott",
                            "Referer"    to referer,
                            "User-Agent" to NetMirrorBypassManager.NATIVE_UA
                        ),
                        quality     = quality,
                        subtitles   = subtitles
                    )
                }
            }

            if (streams.isEmpty()) {
                StreamLogger.error(TAG, "No streams found in playlist response")
                return emptyResult()
            }

            com.streamflex.core.utils.StreamLogger.error("SUBTITLE_DEBUG", "NetMirrorExtractor Extracted ${streams.size} streams, ${subtitles.size} subtitles")
            StreamLogger.debug(TAG, "Extracted ${streams.size} streams, ${subtitles.size} subtitles")
            result(streams)
        } catch (e: Exception) {
            StreamLogger.error(TAG, "Playlist parse error: ${e.message}")
            emptyResult()
        }
    }
}
