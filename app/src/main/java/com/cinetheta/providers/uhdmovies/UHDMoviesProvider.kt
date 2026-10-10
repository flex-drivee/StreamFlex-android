package com.cinetheta.providers.uhdmovies

import com.cinetheta.domain.models.*
import com.cinetheta.domain.provider.Provider
import org.jsoup.Jsoup
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UHDMoviesProvider : Provider {
    override val name: String = "UHDMovies"
    override val id: String = "uhdmovies"
    override val baseUrl: String = "https://uhdmovies.my"
    override val supportedMedia: Set<MediaType> = setOf(MediaType.MOVIE, MediaType.TV)
    
    private val client = OkHttpClient.Builder().build()
    
    override suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val searchUrl = "$baseUrl/?s=${query.replace(" ", "+")}"
        
        try {
            val request = Request.Builder().url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()
                
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            
            val doc = Jsoup.parse(response.body?.string() ?: "")
            val results = mutableListOf<SearchResult>()
            
            val elements = doc.select("article.gridlove-post, article.latestPost, div.post")
            for (element in elements) {
                val a = element.selectFirst("a") ?: continue
                val title = element.selectFirst(".entry-title, .title, h2, .sanket")?.text() ?: a.attr("title")
                val url = a.attr("href")
                var img = element.selectFirst("img")?.attr("src") ?: ""
                
                if (img.isEmpty() || img.contains("data:image")) {
                    img = element.selectFirst("img")?.attr("data-src") ?: img
                }
                
                if (title.isNotEmpty() && url.isNotEmpty()) {
                    results.add(
                        SearchResult(
                            id = url,
                            url = url,
                            providerId = id,
                            providerName = name,
                            title = title,
                            poster = img,
                            mediaType = if (title.contains("Season", true) || title.contains("Episode", true)) MediaType.TV else MediaType.MOVIE
                        )
                    )
                }
            }
            results
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun load(searchResult: SearchResult): ProviderResult? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(searchResult.url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()
                
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext ProviderResult(
                    id = searchResult.id, providerId = id, title = searchResult.title,
                    detailUrl = searchResult.url, mediaType = searchResult.mediaType,
                    success = false, error = "HTTP ${response.code}"
                )
            }
            
            val doc = Jsoup.parse(response.body?.string() ?: "")
            
            // Look for Google Drive download buttons that wrap the shortener links
            val links = doc.select("a.maxbutton-download-g-drive, a[href*=thenaukriadda], a[href*=driveleech], a[href*=driveseed]")
            
            val sources = links.mapIndexed { index, el ->
                val href = el.attr("href")
                val isShortener = href.contains("thenaukriadda")
                val isDriveleech = href.contains("driveleech")
                val isDriveseed = href.contains("driveseed")
                
                val hostType = when {
                    isShortener -> HostType.THENAUKRIADDA
                    isDriveleech -> HostType.DRIVELEECH
                    isDriveseed -> HostType.DRIVESEED
                    else -> HostType.THENAUKRIADDA // default fallback
                }
                
                // Get the parent text to determine quality
                val qualityText = el.parent()?.parent()?.previousElementSibling()?.text() ?: "Link ${index + 1}"
                
                ProviderSource(
                    provider = name,
                    host = qualityText.take(50),
                    hostType = hostType, 
                    url = href
                )
            }
            
            ProviderResult(
                id = searchResult.id,
                providerId = id,
                title = searchResult.title,
                detailUrl = searchResult.url,
                mediaType = searchResult.mediaType,
                sources = sources,
                poster = searchResult.poster,
                success = true,
                overview = doc.selectFirst("div.entry-content p")?.text()
            )
        } catch (e: Exception) {
            ProviderResult(
                id = searchResult.id, providerId = id, title = searchResult.title,
                detailUrl = searchResult.url, mediaType = searchResult.mediaType,
                success = false, error = e.message
            )
        }
    }
}
