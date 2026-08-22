package com.streamflex.extractors.abyss

import com.streamflex.core.logger.Logger
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.Quality
import com.streamflex.domain.models.StreamLink
import com.streamflex.extractors.common.BaseExtractor
import org.json.JSONArray
import org.json.JSONObject

/**
 * AbyssPlayerExtractor
 *
 * Handles video extraction from:
 *   - https://abyssplayer.com/
 *   - https://playhydrax.com/
 *
 * ## Flow
 * 1. Fetch the player page (e.g. https://abyssplayer.com/6rjyLruUiY).
 * 2. Extract the base64-encoded encrypted `datas` JS variable from the inline script.
 * 3. POST the encrypted string to https://enc-dec.app/api/dec-abyss with body {"text":"<datas>"}.
 * 4. Parse the JSON response:
 *    {
 *      "status": 200,
 *      "result": {
 *        "sources": [
 *          { "url": "...", "type": "1080p", "codec": "h264", "size": 341060346, "status": true },
 *          ...
 *        ]
 *      }
 *    }
 * 5. Return one StreamLink per source, sorted best quality first (1080p → 720p → 360p).
 */
class AbyssPlayerExtractor : BaseExtractor() {

    override val hostType: HostType = HostType.ABYSS

    companion object {
        private const val TAG         = "AbyssPlayerExtractor"
        private const val DEC_API_URL = "https://enc-dec.app/api/dec-abyss"
        // Regex to find: datas="<base64>" or datas = '<base64>'
        private val DATAS_REGEX = Regex("""datas\s*=\s*["'`]([A-Za-z0-9+/=]+)["'`]""")
    }

    override suspend fun extract(source: ProviderSource): ExtractionResult {
        val playerUrl = source.url.trim()
        Logger.d("[$TAG] Extracting from $playerUrl")

        // Step 1: Fetch the player page
        val pageHtml = fetchPage(playerUrl) ?: run {
            Logger.w("[$TAG] Failed to fetch player page: $playerUrl")
            return emptyResult()
        }

        // Step 2: Extract the encrypted datas string
        val encryptedData = DATAS_REGEX.find(pageHtml)?.groupValues?.get(1) ?: run {
            Logger.w("[$TAG] Could not find 'datas' variable in player page: $playerUrl")
            return emptyResult()
        }

        Logger.d("[$TAG] Extracted datas (${encryptedData.length} chars), decrypting…")

        // Step 3: POST to decryption API
        val decryptedJson = decryptData(encryptedData) ?: run {
            Logger.w("[$TAG] Decryption API returned null for $playerUrl")
            return emptyResult()
        }

        // Step 4: Parse sources
        val streams = parseStreams(decryptedJson, source)
        Logger.i("[$TAG] Extracted ${streams.size} streams from $playerUrl")

        return result(streams)
    }

    /** Fetches the AbyssPlayer embed page HTML. */
    private suspend fun fetchPage(url: String): String? {
        val req = RequestBuilder()
            .url(url)
            .header("Referer", "https://animedekho.app/")
            .build()
        return when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> res.data.body?.toString(Charsets.UTF_8)
            else -> null
        }
    }

    /**
     * POSTs the encrypted text to the decryption API and returns the raw JSON string,
     * or null on failure.
     */
    private suspend fun decryptData(encryptedText: String): String? {
        val bodyBytes = """{"text":"$encryptedText"}""".toByteArray(Charsets.UTF_8)
        val request   = RequestBuilder()
            .url(DEC_API_URL)
            .post(bodyBytes)
            .header("Content-Type", "application/json")
            .build()

        return when (val res = HttpClient.execute(request)) {
            is NetworkResult.Success -> res.data.body?.toString(Charsets.UTF_8)
            else -> {
                Logger.w("[$TAG] Decryption API error: $res")
                null
            }
        }
    }

    /**
     * Parses the decryption API JSON response into a list of StreamLinks.
     *
     * Expected structure:
     * { "status": 200, "result": { "sources": [ { "url", "type", "codec", "size", "status" } ] } }
     */
    private fun parseStreams(json: String, source: ProviderSource): List<StreamLink> {
        return try {
            val root    = JSONObject(json)
            val status  = root.optInt("status", 0)
            if (status != 200) {
                Logger.w("[$TAG] Decryption API returned status $status")
                return emptyList()
            }

            val result  = root.optJSONObject("result") ?: return emptyList()
            val sources = result.optJSONArray("sources")   ?: return emptyList()

            val streams = mutableListOf<StreamLink>()

            for (i in 0 until sources.length()) {
                val entry   = sources.optJSONObject(i) ?: continue
                val active  = entry.optBoolean("status", true)
                if (!active) continue

                val url     = entry.optString("url").takeIf { it.isNotBlank() } ?: continue
                val typeStr = entry.optString("type", "")   // "1080p", "720p", "360p" …
                val codec   = entry.optString("codec", "")  // "h264", "av1", "hevc" …
                val size    = entry.optLong("size", -1L).takeIf { it > 0 }

                val quality  = parseQuality(typeStr)
                val codecMeta = mapCodecLabel(codec)

                // Build a provider source with the codec metadata so buildName() picks it up
                val sourceWithMeta = source.copy(
                    metadata = source.metadata + mapOf("codec" to codecMeta)
                )

                streams += createStream(
                    name     = "Abyss",
                    source   = sourceWithMeta.copy(hostType = HostType.DIRECT),
                    url      = url,
                    quality  = quality,
                    fileSize = size
                )
            }

            // Best quality first: 1080p → 720p → 480p → 360p; then prefer h264 over av1/hevc
            streams.sortedWith(
                compareByDescending<StreamLink> { it.quality.ordinal }
                    .thenBy { it.name.contains("AV1", ignoreCase = true) || it.name.contains("HEVC", ignoreCase = true) }
            )

        } catch (e: Exception) {
            Logger.e("[$TAG] JSON parse error: ${e.message}")
            emptyList()
        }
    }

    private fun parseQuality(typeStr: String): Quality = when {
        typeStr.contains("2160") || typeStr.contains("4k", ignoreCase = true) -> Quality.P2160
        typeStr.contains("1080")                                               -> Quality.P1080
        typeStr.contains("720")                                                -> Quality.P720
        typeStr.contains("480")                                                -> Quality.P480
        typeStr.contains("360")                                                -> Quality.P360
        else                                                                   -> Quality.UNKNOWN
    }

    private fun mapCodecLabel(codec: String): String = when (codec.lowercase()) {
        "h264", "avc"  -> "H.264"
        "h265", "hevc" -> "HEVC"
        "av1"          -> "AV1"
        else           -> codec.uppercase().ifBlank { "H.264" }
    }
}
