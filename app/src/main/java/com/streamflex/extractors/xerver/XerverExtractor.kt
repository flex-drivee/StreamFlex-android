package com.streamflex.extractors.xerver

import com.streamflex.core.logger.Logger
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.StreamLink
import com.streamflex.extractors.common.BaseExtractor
import org.json.JSONObject

class XerverExtractor : BaseExtractor() {
    override val hostType = HostType.XERVER

    override suspend fun extract(source: ProviderSource): ExtractionResult {
        // The URL is like: https://mirror.xerver.xyz/get/play.php?url=...
        // We need to append &fetch=1
        val fetchUrl = if (source.url.contains("?")) {
            "${source.url}&fetch=1"
        } else {
            "${source.url}?fetch=1"
        }

        val req = RequestBuilder()
            .url(fetchUrl)
            .header("Referer", source.url)
            .header("Accept", "application/json")
            .build()

        return when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> {
                val jsonStr = res.data.body?.toString(Charsets.UTF_8) ?: return emptyResult()
                val streams = mutableListOf<StreamLink>()
                
                try {
                    val root = JSONObject(jsonStr)
                    if (root.has("results")) {
                        val resultsObj = root.getJSONObject("results")
                        
                        // Parse direct_mgt, instant_dl, cloud_r2, etc.
                        val keys = resultsObj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val item = resultsObj.optJSONObject(key)
                            if (item != null && item.has("url")) {
                                val url = item.getString("url")
                                val label = item.optString("label", "Xerver $key")
                                
                                if (url.startsWith("http")) {
                                    val stream = createStream(source, url)
                                    streams.add(stream.copy(name = label))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Logger.e("[Xerver] Failed to parse JSON: ${e.message}")
                }

                if (streams.isNotEmpty()) {
                    Logger.d("[Xerver] Found ${streams.size} streams")
                    result(streams)
                } else {
                    Logger.w("[Xerver] No streams found in JSON")
                    emptyResult()
                }
            }
            else -> emptyResult()
        }
    }
}
