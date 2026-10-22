package com.cinetheta.providers.animedekho

import com.cinetheta.core.logger.Logger
import com.cinetheta.core.network.HttpClient
import com.cinetheta.core.network.NetworkResult
import com.cinetheta.core.network.RequestBuilder
import com.cinetheta.core.parser.HtmlParser
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.MediaType
import com.cinetheta.domain.models.ProviderEpisode
import com.cinetheta.domain.models.ProviderResult
import com.cinetheta.domain.models.ProviderSeason
import com.cinetheta.domain.models.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.nodes.Document

class AnimeDekhoDetails {

    companion object {
        private const val TAG = "AnimeDekhoDetails"
    }

    /**
     * Load full details and seasons/episodes for a search result on animedekho.tv.
     */
    suspend fun load(
        result  : SearchResult,
        baseUrl : String
    ): ProviderResult? = withContext(Dispatchers.IO) {
        var safeBaseUrl = baseUrl
        if (safeBaseUrl.contains("hindisubanime", ignoreCase = true) || safeBaseUrl.contains("animedekho.app", ignoreCase = true)) {
            safeBaseUrl = AnimeDekhoConfig.DEFAULT_DOMAIN
        }
        val safeResultUrl = if (result.url.contains("hindisubanime", ignoreCase = true) || result.url.contains("animedekho.app", ignoreCase = true)) {
            result.url.replace("https://hindisubanime.co", AnimeDekhoConfig.DEFAULT_DOMAIN)
                      .replace("http://hindisubanime.co", AnimeDekhoConfig.DEFAULT_DOMAIN)
                      .replace("https://animedekho.app", AnimeDekhoConfig.DEFAULT_DOMAIN)
                      .replace("http://animedekho.app", AnimeDekhoConfig.DEFAULT_DOMAIN)
        } else {
            result.url
        }

        // Fetch detail page
        val detailHtml = fetchHtml(safeResultUrl, safeBaseUrl) ?: return@withContext null
        val detailDoc  = HtmlParser.parse(detailHtml)

        // 1. Extract postId
        val postId = extractPostId(detailDoc, detailHtml, result.copy(url = safeResultUrl))

        // 2. Check if TV or Movie
        val isTV = result.mediaType == MediaType.TV ||
            safeResultUrl.contains("series", ignoreCase = true) ||
            detailHtml.contains("dgt-season-tabs") ||
            detailHtml.contains("id=\"episodes\"") ||
            detailDoc.select("a[href*=/epi/]").isNotEmpty()

        if (isTV) {
            val seasons = loadSeasons(postId, baseUrl, detailDoc, detailHtml, result)
            if (seasons.isNotEmpty()) {
                return@withContext ProviderResult(
                    id         = result.id,
                    providerId = AnimeDekhoConfig.PROVIDER_ID,
                    title      = result.title,
                    detailUrl  = result.url,
                    mediaType  = MediaType.TV,
                    poster     = result.poster,
                    seasons    = seasons
                )
            }
        }

        // --- Movie: defer source extraction to AnimeDekhoExtractor ---
        val source = AnimeDekhoMapper.toProviderSource(
            iframeUrl = result.url,
            hostType  = HostType.ANIMEDEKHO,
            referer   = baseUrl,
            metadata  = mapOf(
                "postId" to postId,
                "episodeId" to "0",
                "isMovie" to "true",
                "baseUrl" to baseUrl
            )
        )

        ProviderResult(
            id         = result.id,
            providerId = AnimeDekhoConfig.PROVIDER_ID,
            title      = result.title,
            detailUrl  = result.url,
            mediaType  = MediaType.MOVIE,
            poster     = result.poster,
            sources    = listOf(source)
        )
    }

    private suspend fun loadSeasons(
        postId: String,
        baseUrl: String,
        doc: Document,
        html: String,
        result: SearchResult
    ): List<ProviderSeason> = coroutineScope {
        val seasonNumbers = mutableListOf<Int>()

        // 1. Try extracting seasons list from data-tabs in #dgt-season-tabs
        val tabsMatch = Regex("""data-tabs=["']([^"']+)["']""").find(html)
        if (tabsMatch != null) {
            val rawJson = tabsMatch.groupValues[1]
                .replace("&quot;", "\"")
                .replace("&#038;", "&")
            try {
                val jsonArr = JSONArray(rawJson)
                for (i in 0 until jsonArr.length()) {
                    val sObj = jsonArr.optJSONObject(i) ?: continue
                    val sNum = sObj.optInt("season", 0)
                    if (sNum > 0) seasonNumbers.add(sNum)
                }
            } catch (e: Exception) {
                Logger.w("Failed to parse data-tabs json: ${e.message}", TAG)
            }
        }

        // If tabs didn't give numbers but we have postId, probe season 1..10
        if (seasonNumbers.isEmpty() && postId.isNotBlank()) {
            seasonNumbers.add(1)
        }

        // 2. Fetch episodes for each season via REST API
        if (postId.isNotBlank() && seasonNumbers.isNotEmpty()) {
            val seasons = mutableListOf<ProviderSeason>()
            for (seasonNum in seasonNumbers) {
                val epUrl = "$baseUrl/wp-json/dgt/v1/episodes?series=$postId&season=$seasonNum"
                val jsonStr = fetchRestJson(epUrl, baseUrl)
                if (jsonStr.isNullOrBlank()) continue

                try {
                    val obj = JSONObject(jsonStr)
                    val epArr = obj.optJSONArray("episodes") ?: continue
                    if (epArr.length() == 0) continue

                    val episodeList = mutableListOf<ProviderEpisode>()
                    for (k in 0 until epArr.length()) {
                        val epObj = epArr.optJSONObject(k) ?: continue
                        val epId = epObj.optInt("id", 0)
                        val epNum = epObj.optInt("episode", k + 1)
                        val epName = epObj.optString("name").takeIf { it.isNotBlank() } ?: "Episode $epNum"
                        val epStill = epObj.optString("still").takeIf { it.isNotBlank() }
                        val epPageUrl = epObj.optString("url").takeIf { it.isNotBlank() } ?: result.url

                        val source = AnimeDekhoMapper.toProviderSource(
                            iframeUrl = epPageUrl,
                            hostType  = HostType.ANIMEDEKHO,
                            referer   = baseUrl,
                            metadata  = mapOf(
                                "postId" to postId,
                                "episodeId" to epId.toString(),
                                "isMovie" to "false",
                                "baseUrl" to baseUrl
                            )
                        )

                        episodeList.add(
                            ProviderEpisode(
                                number    = epNum,
                                title     = epName,
                                thumbnail = epStill,
                                sources   = listOf(source)
                            )
                        )
                    }

                    if (episodeList.isNotEmpty()) {
                        seasons.add(
                            ProviderSeason(
                                number   = seasonNum,
                                title    = "Season $seasonNum",
                                episodes = episodeList
                            )
                        )
                    }
                } catch (e: Exception) {
                    Logger.w("Failed parsing episodes json for season $seasonNum: ${e.message}", TAG)
                }
            }

            if (seasons.isNotEmpty()) return@coroutineScope seasons
        }

        // 3. Fallback: Parse /epi/ links from page HTML
        val episodeLinks = doc.select("a[href]")
            .map { it.attr("abs:href").ifBlank { it.attr("href") } }
            .filter { "/epi/" in it }
            .distinct()

        if (episodeLinks.isNotEmpty()) {
            val seasonMap = mutableMapOf<Int, MutableList<Pair<Int, String>>>()
            val seasonEpRegex = Regex("""(\d+)x(\d+)""")

            for (link in episodeLinks) {
                val fullLink = if (link.startsWith("http")) link else "$baseUrl/${link.trimStart('/')}"
                val slug = fullLink.trimEnd('/').substringAfterLast("/")
                val match = seasonEpRegex.find(slug)
                val season = match?.groupValues?.get(1)?.toIntOrNull() ?: 1
                val ep     = match?.groupValues?.get(2)?.toIntOrNull() ?: 1
                seasonMap.getOrPut(season) { mutableListOf() }.add(ep to fullLink)
            }

            return@coroutineScope seasonMap.entries.sortedBy { it.key }.map { (seasonNum, eps) ->
                val sortedEps = eps.sortedBy { it.first }
                val episodes  = sortedEps.map { (epNum, epUrl) ->
                    val source = AnimeDekhoMapper.toProviderSource(
                        iframeUrl = epUrl,
                        hostType  = HostType.ANIMEDEKHO,
                        referer   = baseUrl,
                        metadata  = mapOf(
                            "postId" to postId,
                            "episodeId" to "0",
                            "isMovie" to "false",
                            "baseUrl" to baseUrl
                        )
                    )
                    ProviderEpisode(
                        number  = epNum,
                        title   = "Episode $epNum",
                        sources = listOf(source)
                    )
                }
                ProviderSeason(
                    number   = seasonNum,
                    title    = "Season $seasonNum",
                    episodes = episodes
                )
            }
        }

        emptyList()
    }

    private fun extractPostId(
        doc: Document,
        html: String,
        result: SearchResult
    ): String {
        if (result.id.isNotBlank() && result.id.all(Char::isDigit)) {
            return result.id
        }

        // Check shortlink ?p=123
        val shortlinkMatch = Regex("""[?&]p=(\d+)""").find(html)
        if (shortlinkMatch != null) return shortlinkMatch.groupValues[1]

        // Check body postid-123
        val bodyClasses = doc.body()?.classNames() ?: emptySet()
        val postidClass = bodyClasses.firstOrNull { it.startsWith("postid-") && it.removePrefix("postid-").all(Char::isDigit) }
        if (postidClass != null) return postidClass.removePrefix("postid-")

        val termClass = bodyClasses.firstOrNull { it.startsWith("term-") && it.removePrefix("term-").all(Char::isDigit) }
        if (termClass != null) return termClass.removePrefix("term-")

        // Check comment_post_ID
        val commentMatch = Regex("""comment_post_ID['"]\s+value=['"](\d+)['"]""").find(html)
        if (commentMatch != null) return commentMatch.groupValues[1]

        // Check json {"id":825,"type":
        val jsonMatch = Regex("""\{&quot;id&quot;:(\d+),""").find(html)
            ?: Regex("""\{"id":(\d+),""").find(html)
        if (jsonMatch != null) return jsonMatch.groupValues[1]

        return ""
    }

    private suspend fun fetchRestJson(url: String, baseUrl: String): String? {
        val req = RequestBuilder()
            .url(url)
            .header("Referer", "$baseUrl/")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
            .timeout(10_000L)
            .build()
        return when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> res.data.body?.toString(Charsets.UTF_8)
            else -> null
        }
    }

    private suspend fun fetchHtml(url: String, referer: String): String? {
        val req = RequestBuilder()
            .url(url)
            .header("Referer", "$referer/")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
            .timeout(10_000L)
            .build()
        return when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> res.data.body?.toString(Charsets.UTF_8)
            else -> null
        }
    }
}
