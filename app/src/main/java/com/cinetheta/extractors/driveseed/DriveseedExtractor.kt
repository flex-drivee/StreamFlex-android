package com.cinetheta.extractors.driveseed

import com.cinetheta.domain.models.ExtractionResult
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.extractors.common.BaseExtractor
import com.cinetheta.core.network.CineThetaHttpClient
import com.cinetheta.core.network.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class DriveseedExtractor : BaseExtractor() {
    override val hostType = HostType.DRIVESEED

    override suspend fun extract(source: ProviderSource): ExtractionResult = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("STREAM_PIPELINE", "[DriveseedExtractor] Starting extraction for: ${source.url}")
            val res = CineThetaHttpClient.get(
                url = source.url,
                headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            )
            
            if (res !is NetworkResult.Success) {
                android.util.Log.e("STREAM_PIPELINE", "[DriveseedExtractor] GET request failed: $res")
                return@withContext emptyResult()
            }
            
            val html = res.data.bodyAsString()
            android.util.Log.d("STREAM_PIPELINE", "[DriveseedExtractor] HTML length: ${html.length}")
            val doc = Jsoup.parse(html)
            
            val videoSrc = doc.selectFirst("video source")?.attr("src") 
                ?: doc.selectFirst("a[href*=download], a.btn")?.attr("href")
            
            if (!videoSrc.isNullOrEmpty()) {
                android.util.Log.d("STREAM_PIPELINE", "[DriveseedExtractor] Found direct video source: $videoSrc")
                return@withContext result(createStream(source, videoSrc))
            }
            
            // Check for API fetch logic based on Cloudstream reference
            val idMatch = Regex("""fetch\(['"]/download\?id=([a-zA-Z0-9/+]+)['"]""").find(html)
                ?: Regex("""fetch\(['"]/api/download\?id=([a-zA-Z0-9/+]+)['"]""").find(html)
            val tokenMatch = Regex("""formData\.append\(['"]token['"],\s*['"]([a-f0-9]+)['"]\)""").find(html)
                ?: Regex("""['"]x-token['"]\s*:\s*['"]([^'"]+)['"]""").find(html)
                
            if (idMatch != null && tokenMatch != null) {
                val id = idMatch.groupValues[1]
                val token = tokenMatch.groupValues[1]
                val fetchUrl = "https://${java.net.URI(source.url).host}/download?id=$id"
                android.util.Log.d("STREAM_PIPELINE", "[DriveseedExtractor] Found API tokens, fetching: $fetchUrl with token: $token")
                
                val apiRes = CineThetaHttpClient.postForm(
                    url = fetchUrl,
                    form = mapOf("token" to token),
                    headers = mapOf(
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                        "x-token" to token,
                        "Referer" to source.url
                    ),
                    referer = source.url
                )
                
                if (apiRes is NetworkResult.Success) {
                    val json = apiRes.data.bodyAsString()
                    android.util.Log.d("STREAM_PIPELINE", "[DriveseedExtractor] API response: $json")
                    try {
                        val videoUrl = org.json.JSONObject(json).optString("url")
                        if (videoUrl.isNotBlank()) {
                            return@withContext result(createStream(source, videoUrl))
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("STREAM_PIPELINE", "[DriveseedExtractor] JSON parsing error", e)
                    }
                }
            } else {
                android.util.Log.d("STREAM_PIPELINE", "[DriveseedExtractor] idMatch=${idMatch?.value}, tokenMatch=${tokenMatch?.value}")
            }
            
            // Check for meta refresh or replace logic
            val replaceMatch = Regex("""replace\(\s*['"]([^'"]+)['"]\s*\)""").find(html)
            if (replaceMatch != null) {
                val redirectUrl = replaceMatch.groupValues[1]
                android.util.Log.d("STREAM_PIPELINE", "[DriveseedExtractor] Found JS replace redirect: $redirectUrl")
                return@withContext next(source.copy(url = redirectUrl))
            }
            
            android.util.Log.d("STREAM_PIPELINE", "[DriveseedExtractor] Could not find direct video source.")
        } catch (e: Exception) {
            android.util.Log.e("STREAM_PIPELINE", "[DriveseedExtractor] Exception caught", e)
        }
        android.util.Log.d("STREAM_PIPELINE", "[DriveseedExtractor] Returning emptyResult")
        emptyResult()
    }
}
