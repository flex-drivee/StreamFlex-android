package com.cinetheta.extractors.thenaukriadda

import com.cinetheta.domain.models.ExtractionResult
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.extractors.common.BaseExtractor
import com.cinetheta.core.network.CineThetaHttpClient
import com.cinetheta.core.network.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class TheNaukriAddaExtractor : BaseExtractor() {
    override val hostType = HostType.THENAUKRIADDA

    override suspend fun extract(source: ProviderSource): ExtractionResult = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Starting extraction for: ${source.url}")
            
            var currentUrl = source.url
            var res = CineThetaHttpClient.get(
                url = currentUrl,
                headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            )
            
            if (res !is NetworkResult.Success) {
                android.util.Log.e("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Initial GET request failed: $res")
                return@withContext emptyResult()
            }
            
            var html = res.data.bodyAsString()
            var doc = Jsoup.parse(html)
            
            var hopCount = 0
            val maxHops = 15
            
            while (hopCount < maxHops) {
                hopCount++
                
                // 1. Try to find form
                val form = doc.selectFirst("form")
                if (form != null) {
                    var formUrl = form.attr("action").takeIf { it.isNotBlank() } ?: currentUrl
                    // Force it to use currentUrl if it tries to post to root without the sid
                    if (formUrl == "https://en.thenaukriadda.in/") {
                        formUrl = currentUrl
                    }
                    
                    val formData = mutableMapOf<String, String>()
                    for (input in form.select("input")) {
                        val name = input.attr("name")
                        val value = input.attr("value")
                        if (name.isNotBlank()) {
                            formData[name] = value
                        }
                    }
                    
                    android.util.Log.d("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Hop $hopCount: Submitting form to: $formUrl")
                    
                    val postRes = CineThetaHttpClient.postForm(
                        url = formUrl,
                        form = formData,
                        headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"),
                        referer = currentUrl
                    )
                    
                    if (postRes !is NetworkResult.Success) {
                        android.util.Log.e("STREAM_PIPELINE", "[TheNaukriAddaExtractor] POST request failed: $postRes")
                        return@withContext emptyResult()
                    }
                    
                    currentUrl = postRes.data.url
                    html = postRes.data.bodyAsString()
                    doc = Jsoup.parse(html)
                    
                    val host = if (currentUrl.contains("driveleech")) HostType.DRIVELEECH 
                               else if (currentUrl.contains("driveseed")) HostType.DRIVESEED
                               else HostType.THENAUKRIADDA
                    
                    if (host != HostType.THENAUKRIADDA) {
                        android.util.Log.d("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Redirected to supported host via network: $host ($currentUrl)")
                        return@withContext next(source.copy(url = currentUrl, hostType = host))
                    }
                    
                    continue // Loop again to process the new HTML (might be another form, meta refresh, or video)
                }
                
                // 2. Check for meta refresh
                val metaRefresh = doc.selectFirst("meta[http-equiv=refresh]")?.attr("content")
                if (metaRefresh != null) {
                    val parts = metaRefresh.split("url=")
                    if (parts.size > 1) {
                        val redirectUrl = parts[1].trim()
                        val redirectHost = if (redirectUrl.contains("driveleech")) HostType.DRIVELEECH 
                                           else if (redirectUrl.contains("driveseed")) HostType.DRIVESEED
                                           else HostType.THENAUKRIADDA
                        
                        android.util.Log.d("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Hop $hopCount: Found meta refresh: $redirectUrl")
                        
                        if (redirectHost != HostType.THENAUKRIADDA) {
                            return@withContext next(source.copy(url = redirectUrl, hostType = redirectHost))
                        } else {
                            // Fetch the new url and loop
                            currentUrl = redirectUrl
                            val getRes = CineThetaHttpClient.get(
                                url = currentUrl,
                                headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                            )
                            if (getRes !is NetworkResult.Success) return@withContext emptyResult()
                            html = getRes.data.bodyAsString()
                            doc = Jsoup.parse(html)
                            continue
                        }
                    }
                }

                // 3. Check for JS redirect (window.location.replace)
                val jsRedirectMatcher = Regex("""window\.location\.replace\(['"]([^'"]+)['"]\)""").find(html)
                if (jsRedirectMatcher != null) {
                    val redirectUrl = jsRedirectMatcher.groupValues[1]
                    android.util.Log.d("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Hop $hopCount: Found JS redirect: $redirectUrl")
                    val redirectHost = if (redirectUrl.contains("driveleech")) HostType.DRIVELEECH 
                                       else if (redirectUrl.contains("driveseed")) HostType.DRIVESEED
                                       else HostType.THENAUKRIADDA
                    if (redirectHost != HostType.THENAUKRIADDA) {
                        return@withContext next(source.copy(url = redirectUrl, hostType = redirectHost))
                    } else {
                        currentUrl = redirectUrl
                        val getRes = CineThetaHttpClient.get(
                            url = currentUrl,
                            headers = mapOf("User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        )
                        if (getRes !is NetworkResult.Success) return@withContext emptyResult()
                        html = getRes.data.bodyAsString()
                        doc = Jsoup.parse(html)
                        continue
                    }
                }
                
                // 4. Try to find direct video source or download link
                val videoSrc = doc.selectFirst("video source")?.attr("src") 
                    ?: doc.selectFirst("a.btn-success, a.download-btn, a[href*=download]")?.attr("href")
                
                if (!videoSrc.isNullOrEmpty()) {
                    android.util.Log.d("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Found direct video source: $videoSrc")
                    return@withContext result(createStream(source, videoSrc))
                }
                
                // 5. Try any generic link in the page
                val genericLinks = doc.select("a[href]")
                for (link in genericLinks) {
                    val href = link.attr("href")
                    if (href.contains("driveleech") || href.contains("driveseed")) {
                        android.util.Log.d("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Found fallback href: $href")
                        val host = if (href.contains("driveleech")) HostType.DRIVELEECH else HostType.DRIVESEED
                        return@withContext next(source.copy(url = href, hostType = host))
                    }
                }
                
                // If nothing was found in this hop, break
                break
            }
            
        } catch (e: Exception) {
            android.util.Log.e("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Exception caught", e)
        }
        android.util.Log.d("STREAM_PIPELINE", "[TheNaukriAddaExtractor] Returning emptyResult")
        emptyResult()
    }
}
