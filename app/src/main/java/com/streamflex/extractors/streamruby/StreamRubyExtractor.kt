package com.streamflex.extractors.streamruby

import com.streamflex.core.logger.Logger
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.StreamLink
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.extractors.shared.JsUnpacker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StreamRubyExtractor : BaseExtractor() {
    override val hostType = HostType.STREAMRUBY

    override suspend fun extract(source: ProviderSource): ExtractionResult = withContext(Dispatchers.IO) {
        val url = source.url
        val fileCode = url.substringAfterLast("/")
        
        if (fileCode.isEmpty()) {
            Logger.w("[StreamRuby] Invalid URL: $url")
            return@withContext emptyResult()
        }

        val dlUrl = "https://rubystm.com/dl"
        val payload = "op=embed&file_code=$fileCode&auto=1&referer="
        
        val request = RequestBuilder()
            .url(dlUrl)
            .post(payload.toByteArray())
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Referer", url)
            .build()
            
        when (val response = HttpClient.execute(request)) {
            is NetworkResult.Success -> {
                val html = response.data.bodyAsString()
                
                // Find the eval block first to prevent regex catastrophic backtracking/overflow
                val evalRegex = Regex("""eval\(function.*?\.split\('\|'\).*?\)\)""", RegexOption.DOT_MATCHES_ALL)
                val evalBlock = evalRegex.find(html)?.value ?: html
                
                if (evalBlock.isNotBlank()) {
                    val unpacked = JsUnpacker.unpack(evalBlock)
                    if (unpacked != null) {
                        // Extract file:"..." or file:'...'
                        val fileRegex = Regex("""file\s*:\s*["']([^"']+)["']""")
                        val fileMatch = fileRegex.find(unpacked)
                        
                        if (fileMatch != null) {
                            val streamUrl = fileMatch.groupValues[1]
                            val stream = StreamLink(
                                name = "StreamRuby",
                                url = streamUrl,
                                host = HostType.DIRECT,
                                adaptive = streamUrl.contains(".m3u8"),
                                headers = mapOf("Referer" to "https://rubystm.com/"),
                                referer = "https://rubystm.com/"
                            )
                            return@withContext ExtractionResult(listOf(stream))
                        } else {
                            Logger.w("[StreamRuby] Could not find 'file:' inside unpacked JS.")
                        }
                    } else {
                        Logger.w("[StreamRuby] Failed to unpack JS.")
                    }
                } else {
                    Logger.w("[StreamRuby] Could not find eval(function...) in HTML.")
                }
                
                emptyResult()
            }
            else -> {
                Logger.e("[StreamRuby] Request failed or timed out.")
                emptyResult()
            }
        }
    }
}
