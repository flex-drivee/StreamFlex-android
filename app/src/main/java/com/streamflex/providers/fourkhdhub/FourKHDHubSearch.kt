package com.streamflex.providers.fourkhdhub

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.HtmlParser
import com.streamflex.core.parser.SearchResultParser
import com.streamflex.core.parser.TransportResult
import com.streamflex.core.network.NetworkUtils
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.SearchResult

class FourKHDHubSearch : SearchResultParser {

    companion object {
        private const val PROVIDER = "4KHDHub"
    }

    suspend fun search(
        query: String,
        baseUrl: String = "https://4khdhub.one"
    ): List<SearchResult> {

        val request = RequestBuilder()
            .url("$baseUrl/?s=${NetworkUtils.encode(query)}")
            .header("Referer", baseUrl)
            .build()

        return withContext(Dispatchers.IO) {
            when (val response = HttpClient.execute(request)) {
                is NetworkResult.Success -> {
                    val htmlString = response.data.body
                        ?.toString(Charsets.UTF_8)
                        ?: return@withContext emptyList()

                    val rawTransport = TransportResult.TextResponse(
                        text = htmlString,
                        url = request.url
                    )

                    parse(rawTransport, baseUrl)
                }
                else -> emptyList()
            }
        }
    }

    override fun parse(raw: TransportResult): List<SearchResult> {
        return parse(raw, "https://4khdhub.one")
    }

    fun parse(raw: TransportResult, baseUrl: String): List<SearchResult> {
        val document = HtmlParser.parse(raw.asString())
        val results = mutableListOf<SearchResult>()

        val elements = document.select("a.movie-card")
        
        for (element in elements) {
            val href = element.attr("href")
            val detailUrl = if (href.startsWith("http")) href else baseUrl.trimEnd('/') + "/" + href.trimStart('/')
            
            val titleElement = element.selectFirst(".movie-card-title")
            val title = titleElement?.text() ?: continue
            
            val imgElement = element.selectFirst("img")
            val poster = imgElement?.attr("src")?.takeIf { it.isNotBlank() }

            val mediaType = if (detailUrl.contains("series", ignoreCase = true) || detailUrl.contains("tv", ignoreCase = true)) {
                MediaType.TV
            } else {
                MediaType.MOVIE
            }
            
            val yearText = element.selectFirst(".movie-card-meta")?.text()
            val yearMatch = yearText?.let { Regex("""\b(19|20)\d{2}\b""").find(it)?.value?.toIntOrNull() }

            results += FourKHDHubMapper.toSearchResult(
                title = title,
                detailUrl = detailUrl,
                poster = poster,
                year = yearMatch,
                mediaType = mediaType
            )
        }

        return results
    }
}
