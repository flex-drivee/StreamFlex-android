package com.cinetheta.providers.toonstream

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
import kotlinx.coroutines.withContext
import org.jsoup.nodes.Document
import kotlinx.coroutines.delay

class ToonStreamDetails {

    companion object {
        private const val TAG = "ToonStreamDetails"
    }

    suspend fun load(
        result  : SearchResult,
        baseUrl : String
    ): ProviderResult? = withContext(Dispatchers.IO) {

        val actualBaseUrl = try {
            val uri = java.net.URI(result.url)
            "${uri.scheme}://${uri.host}"
        } catch (_: Exception) {
            baseUrl
        }

        val detailHtml = fetchHtml(result.url, actualBaseUrl) ?: return@withContext null
        val detailDoc  = HtmlParser.parse(detailHtml, actualBaseUrl)

        // Strategy 1 (CloudStream Reference):
        // ToonStream uses "div.season-swiper-wrapper a[data-url]" for season tabs.
        // Each tab has data-url attribute pointing to the HTML page containing that season's episodes.
        val seasonSwiperLinks = detailDoc.select("div.season-swiper-wrapper a[data-url], .choose-season a[data-url], div.aa-drp.choose-season a[data-url]")
        if (seasonSwiperLinks.isNotEmpty()) {
            val seasonMap = mutableMapOf<Int, MutableList<ProviderEpisode>>()

            for ((sIndex, seasonLink) in seasonSwiperLinks.withIndex()) {
                val dataUrl = seasonLink.attr("data-url").trim()
                if (dataUrl.isBlank()) continue
                val seasonUrl = fixUrl(dataUrl, actualBaseUrl)

                if (sIndex > 0) delay(100)

                val seasonHtml = fetchHtml(seasonUrl, result.url) ?: continue
                val seasonDoc = HtmlParser.parse(seasonHtml, actualBaseUrl)

                val episodeArticles = seasonDoc.select("article.episodes")
                for ((epIndex, article) in episodeArticles.withIndex()) {
                    val anchor = article.selectFirst("a.lnk-blk, article > a[href], a[href]") ?: continue
                    val rawHref = anchor.attr("href").trim()
                    if (rawHref.isBlank() || rawHref == "#") continue
                    val epUrl = fixUrl(rawHref, actualBaseUrl)

                    val epTitle = article.selectFirst("h5.entry-title1, .entry-title, h5, h2")
                        ?.text()
                        ?.replace("Watch Online", "", ignoreCase = true)
                        ?.trim()
                        .orEmpty()

                    val numEpiText = article.selectFirst("span.num-epi")?.text().orEmpty()
                    val match = Regex("""(\d+)x(\d+)""").find(numEpiText)
                    val seasonNum = match?.groupValues?.get(1)?.toIntOrNull() ?: (sIndex + 1)
                    val episodeNum = match?.groupValues?.get(2)?.toIntOrNull()
                        ?: Regex("""(?i)(?:ep|episode)\s*(\d+)""").find(epTitle)?.groupValues?.get(1)?.toIntOrNull()
                        ?: (epIndex + 1)

                    val finalTitle = epTitle.ifBlank { "Episode $episodeNum" }

                    val source = ToonStreamMapper.toProviderSource(
                        iframeUrl = epUrl,
                        hostType  = HostType.TOONSTREAM,
                        referer   = actualBaseUrl,
                        metadata  = mapOf("isMovie" to "false", "baseUrl" to actualBaseUrl)
                    )

                    val episode = ProviderEpisode(
                        number  = episodeNum,
                        title   = finalTitle,
                        sources = listOf(source)
                    )

                    val list = seasonMap.getOrPut(seasonNum) { mutableListOf() }
                    if (list.none { it.number == episodeNum }) {
                        list.add(episode)
                    }
                }
            }

            if (seasonMap.isNotEmpty()) {
                val allSeasons = seasonMap.entries
                    .sortedBy { it.key }
                    .map { (sNum, eps) ->
                        ProviderSeason(
                            number   = sNum,
                            title    = "Season $sNum",
                            episodes = eps.sortedBy { it.number }
                        )
                    }

                Logger.d("[$TAG] Successfully loaded ${allSeasons.size} season(s) from season-swiper links", TAG)
                return@withContext ProviderResult(
                    id         = result.id,
                    providerId = ToonStreamConfig.PROVIDER_ID,
                    title      = result.title,
                    detailUrl  = result.url,
                    mediaType  = MediaType.TV,
                    poster     = result.poster,
                    seasons    = allSeasons
                )
            }
        }

        // Strategy 2: Check if current page already has article.episodes directly
        val directArticles = detailDoc.select("article.episodes")
        if (directArticles.isNotEmpty()) {
            val episodes = directArticles.mapIndexedNotNull { epIndex, article ->
                val anchor = article.selectFirst("a.lnk-blk, article > a[href], a[href]") ?: return@mapIndexedNotNull null
                val rawHref = anchor.attr("href").trim()
                if (rawHref.isBlank() || rawHref == "#") return@mapIndexedNotNull null
                val epUrl = fixUrl(rawHref, actualBaseUrl)

                val epTitle = article.selectFirst("h5.entry-title1, .entry-title, h5, h2")
                    ?.text()
                    ?.replace("Watch Online", "", ignoreCase = true)
                    ?.trim()
                    .orEmpty()

                val numEpiText = article.selectFirst("span.num-epi")?.text().orEmpty()
                val match = Regex("""(\d+)x(\d+)""").find(numEpiText)
                val episodeNum = match?.groupValues?.get(2)?.toIntOrNull()
                    ?: Regex("""(?i)(?:ep|episode)\s*(\d+)""").find(epTitle)?.groupValues?.get(1)?.toIntOrNull()
                    ?: (epIndex + 1)

                val finalTitle = epTitle.ifBlank { "Episode $episodeNum" }
                val source = ToonStreamMapper.toProviderSource(
                    iframeUrl = epUrl,
                    hostType  = HostType.TOONSTREAM,
                    referer   = actualBaseUrl,
                    metadata  = mapOf("isMovie" to "false", "baseUrl" to actualBaseUrl)
                )

                ProviderEpisode(
                    number  = episodeNum,
                    title   = finalTitle,
                    sources = listOf(source)
                )
            }

            if (episodes.isNotEmpty()) {
                return@withContext ProviderResult(
                    id         = result.id,
                    providerId = ToonStreamConfig.PROVIDER_ID,
                    title      = result.title,
                    detailUrl  = result.url,
                    mediaType  = MediaType.TV,
                    poster     = result.poster,
                    seasons    = listOf(
                        ProviderSeason(
                            number   = 1,
                            title    = "Season 1",
                            episodes = episodes.sortedBy { it.number }
                        )
                    )
                )
            }
        }

        // Strategy 3: DooPlay AJAX fallback (div.aa-drp.choose-season > ul > li > a with data-post and data-season)
        val seasonButtons = detailDoc.select("div.aa-drp.choose-season > ul > li > a, .choose-season a, .season-list a")
        if (seasonButtons.isNotEmpty()) {
            val allSeasons = mutableListOf<ProviderSeason>()

            for ((sIndex, seasonButton) in seasonButtons.withIndex()) {
                val postId = seasonButton.attr("data-post").trim()
                val seasonId = seasonButton.attr("data-season").trim()
                val seasonName = seasonButton.text().ifBlank { "Season $seasonId" }
                val seasonNumber = Regex("""\d+""").find(seasonName)?.value?.toIntOrNull()
                    ?: seasonId.toIntOrNull()
                    ?: (sIndex + 1)

                if (sIndex > 0) delay(150)

                val ajaxUrl = "$actualBaseUrl/wp-admin/admin-ajax.php"
                val body = "action=action_select_season&season=$seasonId&post=$postId"
                val ajaxReq = RequestBuilder()
                    .url(ajaxUrl)
                    .post(body.toByteArray(Charsets.UTF_8))
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .header("Referer", result.url)
                    .build()

                val ajaxHtml = when (val res = HttpClient.execute(ajaxReq)) {
                    is NetworkResult.Success -> res.data.bodyAsString()
                    else -> null
                }

                val episodes = if (!ajaxHtml.isNullOrBlank()) {
                    val ajaxDoc = HtmlParser.parse(ajaxHtml, actualBaseUrl)
                    val episodeElements = ajaxDoc.select("article, li")

                    episodeElements.mapIndexedNotNull { epIndex, epEl ->
                        val anchor = epEl.selectFirst("article > a[href], a[href]") ?: return@mapIndexedNotNull null
                        val rawHref = anchor.attr("href").trim()
                        if (rawHref.isBlank() || rawHref == "#") return@mapIndexedNotNull null

                        val epUrl = fixUrl(rawHref, actualBaseUrl)
                        val epTitle = epEl.selectFirst("article > header.entry-header > h2, header.entry-header > h2, h2, .entry-title")
                            ?.text()
                            ?.replace("Watch Online", "", ignoreCase = true)
                            ?.trim()
                            .orEmpty()
                            .ifBlank { "Episode ${epIndex + 1}" }

                        val epNum = Regex("""(?i)(?:ep|episode)\s*(\d+)""").find(epTitle)?.groupValues?.get(1)?.toIntOrNull()
                            ?: Regex("""\d+""").find(epTitle)?.value?.toIntOrNull()
                            ?: (epIndex + 1)

                        val source = ToonStreamMapper.toProviderSource(
                            iframeUrl = epUrl,
                            hostType  = HostType.TOONSTREAM,
                            referer   = actualBaseUrl,
                            metadata  = mapOf("isMovie" to "false", "baseUrl" to actualBaseUrl)
                        )
                        ProviderEpisode(
                            number  = epNum,
                            title   = epTitle,
                            sources = listOf(source)
                        )
                    }
                } else emptyList()

                if (episodes.isNotEmpty()) {
                    allSeasons.add(
                        ProviderSeason(
                            number   = seasonNumber,
                            title    = seasonName,
                            episodes = episodes
                        )
                    )
                }
            }

            if (allSeasons.isNotEmpty()) {
                return@withContext ProviderResult(
                    id         = result.id,
                    providerId = ToonStreamConfig.PROVIDER_ID,
                    title      = result.title,
                    detailUrl  = result.url,
                    mediaType  = MediaType.TV,
                    poster     = result.poster,
                    seasons    = allSeasons
                )
            }
        }

        // Strategy 4: Fallback direct episode links in list (.episodios a, #seasons li a)
        val directEpisodeLinks = detailDoc.select("ul.episodios li a[href], .episodios a[href], #seasons li a[href], .les-content a[href]")
            .map { it.attr("abs:href").ifBlank { it.attr("href") } }
            .filter { it.isNotBlank() && ("episode" in it.lowercase() || "/ep-" in it.lowercase()) }
            .distinct()

        if (directEpisodeLinks.isNotEmpty()) {
            val episodes = directEpisodeLinks.mapIndexed { index, epUrl ->
                val fixedUrl = fixUrl(epUrl, actualBaseUrl)
                val epNum = Regex("""(?i)(?:ep|episode)-?(\d+)""").find(fixedUrl)?.groupValues?.get(1)?.toIntOrNull() ?: (index + 1)
                val source = ToonStreamMapper.toProviderSource(
                    iframeUrl = fixedUrl,
                    hostType  = HostType.TOONSTREAM,
                    referer   = actualBaseUrl,
                    metadata  = mapOf("isMovie" to "false", "baseUrl" to actualBaseUrl)
                )
                ProviderEpisode(
                    number  = epNum,
                    title   = "Episode $epNum",
                    sources = listOf(source)
                )
            }

            return@withContext ProviderResult(
                id         = result.id,
                providerId = ToonStreamConfig.PROVIDER_ID,
                title      = result.title,
                detailUrl  = result.url,
                mediaType  = MediaType.TV,
                poster     = result.poster,
                seasons    = listOf(
                    ProviderSeason(
                        number   = 1,
                        title    = "Season 1",
                        episodes = episodes
                    )
                )
            )
        }

        // Single video / Movie fallback
        val source = ToonStreamMapper.toProviderSource(
            iframeUrl = result.url,
            hostType  = HostType.TOONSTREAM,
            referer   = actualBaseUrl,
            metadata  = mapOf("isMovie" to "true", "baseUrl" to actualBaseUrl)
        )

        ProviderResult(
            id         = result.id,
            providerId = ToonStreamConfig.PROVIDER_ID,
            title      = result.title,
            detailUrl  = result.url,
            mediaType  = MediaType.MOVIE,
            poster     = result.poster,
            sources    = listOf(source)
        )
    }

    private suspend fun fetchHtml(url: String, referer: String): String? {
        val req = RequestBuilder()
            .url(url)
            .header("Referer", referer)
            .build()
        return when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> res.data.bodyAsString()
            else -> null
        }
    }

    private fun fixUrl(url: String, base: String): String {
        return when {
            url.isBlank() -> ""
            url.startsWith("//") -> "https:$url"
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.startsWith("/") -> {
                try {
                    val uri = java.net.URI(base)
                    "${uri.scheme}://${uri.host}$url"
                } catch (_: Exception) {
                    "$base$url"
                }
            }
            else -> {
                try {
                    val uri = java.net.URI(base)
                    "${uri.scheme}://${uri.host}/$url"
                } catch (_: Exception) {
                    "$base/$url"
                }
            }
        }
    }
}
