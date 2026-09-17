package com.cinetheta.extractors.blakite

import com.cinetheta.core.constants.Constants
import com.cinetheta.core.logger.Logger
import com.cinetheta.core.network.HttpClient
import com.cinetheta.core.network.NetworkResult
import com.cinetheta.core.network.RequestBuilder
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
 * Blakiteapi Extractor (blakiteapi.xyz).
 *
 * Flow from CloudStream reference:
 * 1. Parse tmdbId and id from URL:
 *    tmdbId = url.substringAfter("embed/").substringBefore("/")
 *    id = url.substringAfterLast("/")
 * 2. GET ${mainUrl}/api/get.php?id=${id}&tmdbId=${tmdbId}
 * 3. Parse JSON: { "success": true, "data": { "dataId": "...", "format": "m3u8" } }
 * 4. Stream URL: ${mainUrl}/stream/${dataId}.${format}
 */
class BlakiteExtractor : BaseExtractor() {

    override val hostType = HostType.BLAKITE

    companion object {
        private const val TAG = "BlakiteExtractor"
    }

    override suspend fun extract(source: ProviderSource): ExtractionResult = withContext(Dispatchers.IO) {
        val url = source.url.trim()
        val baseUrl = try {
            val uri = URI(url)
            "${uri.scheme}://${uri.host}"
        } catch (_: Exception) {
            "https://blakiteapi.xyz"
        }

        val tmdbId = if (url.contains("embed/")) {
            url.substringAfter("embed/").substringBefore("/")
        } else ""

        val id = url.substringAfterLast("/").trim()
        val parts = url.split("/").filter { it.isNotBlank() }

        val apiUrl = if (parts.size > 5 && id.isNotBlank()) {
            "$baseUrl/api/get.php?id=$id&tmdbId=$tmdbId"
        } else {
            "$baseUrl/api/get.php?tmdbId=$tmdbId"
        }

        Logger.d("[$TAG] Requesting API: $apiUrl")

        val req = RequestBuilder()
            .url(apiUrl)
            .header("Referer", url)
            .header("User-Agent", Constants.DEFAULT_USER_AGENT)
            .build()

        when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> {
                val body = res.data.bodyAsString()
                try {
                    val root = JSONObject(body)
                    val success = root.optBoolean("success", false)
                    if (success) {
                        val data = root.optJSONObject("data")
                        val dataId = data?.optString("dataId").orEmpty()
                        val format = data?.optString("format", "m3u8")?.lowercase()?.ifBlank { "m3u8" } ?: "m3u8"

                        if (dataId.isNotBlank()) {
                            val streamUrl = "$baseUrl/stream/$dataId.$format"
                            Logger.i("[$TAG] Extracted stream: $streamUrl")
                            val stream = StreamLink(
                                name = "Blakite",
                                url = streamUrl,
                                quality = source.quality,
                                host = HostType.DIRECT,
                                referer = url
                            )
                            return@withContext result(listOf(stream))
                        }
                    }
                } catch (e: Exception) {
                    Logger.e("[$TAG] Failed to parse JSON: ${e.message}")
                }
            }
            else -> {
                Logger.w("[$TAG] Request failed for $apiUrl")
            }
        }

        emptyResult()
    }
}
