package com.cinetheta.providers.piratexplay

import com.cinetheta.core.logger.Logger
import com.cinetheta.core.network.HttpClient
import com.cinetheta.core.network.NetworkResult
import com.cinetheta.core.network.RequestBuilder
import com.cinetheta.core.parser.HtmlParser
import com.cinetheta.domain.models.MediaType
import com.cinetheta.domain.models.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PirateXplaySearch {

    companion object {
        private const val TAG = "PirateXplaySearch"
    }

    suspend fun search(
        query   : String,
        baseUrl : String
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        val searchUrl = "$baseUrl/?s=$query"
        val request = RequestBuilder()
            .url(searchUrl)
            .header("Referer", "$baseUrl/")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        val response = HttpClient.execute(request)
        if (response is NetworkResult.Success) {
            val htmlStr = response.data.body?.toString(Charsets.UTF_8) ?: ""
            val document = HtmlParser.parse(htmlStr)
            val results = mutableListOf<SearchResult>()

            val elements = document.select("article.post")
            for (element in elements) {
                val titleElem = element.selectFirst("h2.entry-title")
                val linkElem = element.selectFirst("a.lnk-blk")
                val imgElem = element.selectFirst("img")

                if (titleElem != null && linkElem != null) {
                    val title = titleElem.text()
                    var url = linkElem.attr("href")
                    if (!url.startsWith("http")) {
                        if (url.startsWith("/")) {
                            url = "$baseUrl$url"
                        } else {
                            url = "$baseUrl/$url"
                        }
                    }

                    var posterUrl = imgElem?.attr("src") ?: ""
                    if (posterUrl.isEmpty()) {
                        posterUrl = imgElem?.attr("data-src") ?: ""
                    }

                    val isSeries = element.hasClass("series")
                    val mediaType = if (isSeries) MediaType.TV else MediaType.MOVIE

                    results.add(
                        SearchResult(
                            id = url.substringAfterLast("/").trimEnd('/'),
                            title = title,
                            url = url,
                            poster = posterUrl,
                            mediaType = mediaType,
                            providerId = PirateXplayConfig.PROVIDER_ID,
                            providerName = PirateXplayConfig.PROVIDER_NAME
                        )
                    )
                }
            }
            return@withContext results
        } else if (response is NetworkResult.Error) {
            Logger.e("Search failed with code: ${response.code}", TAG)
            throw Exception("HTTP ${response.code}")
        }
        emptyList()
    }
}
