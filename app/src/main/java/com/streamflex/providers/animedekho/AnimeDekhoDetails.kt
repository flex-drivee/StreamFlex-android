package com.streamflex.providers.animedekho

import com.streamflex.core.logger.Logger
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.HtmlParser
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.ProviderEpisode
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.ProviderSeason
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class AnimeDekhoDetails {

    companion object {
        private const val TAG = "AnimeDekhoDetails"
    }

    /**
     * Load full details and sources for a search result.
     *
     * Flow:
     * 1. Fetch the series/movie page → collect episode links (TV) or treat page as movie.
     * 2. For each episode page, extract the body `term-(\d+)` ID.
     * 3. Probe /?trdekho=1..MAX_SERVERS&trid={termId}&trtype={1|2} on the MIRROR domain
     *    (hindisubanime.co — no Cloudflare on the trdekho endpoint).
     * 4. Each probe returns an HTML page with an <iframe src="...">; collect that src.
     * 5. Map iframe src domain → HostType and wrap into ProviderSource objects.
     */
    suspend fun load(
        result  : SearchResult,
        baseUrl : String
    ): ProviderResult? = withContext(Dispatchers.IO) {

        val isTV = result.mediaType == MediaType.TV

        // Fetch detail page
        val detailHtml = fetchHtml(result.url, baseUrl) ?: return@withContext null
        val detailDoc  = HtmlParser.parse(detailHtml)

        if (isTV) {
            // --- TV: collect /epi/ links from the detail page ---
            val episodeLinks = detailDoc
                .select("a[href]")
                .map { it.attr("abs:href") }
                .filter { "/epi/" in it }
                .distinct()

            if (episodeLinks.isEmpty()) {
                Logger.w("[$TAG] No episode links found for '${result.title}'")
                return@withContext null
            }

            // Group episodes by season (format: slug-{season}x{episode})
            val seasonMap = mutableMapOf<Int, MutableList<Pair<Int, String>>>()
            val seasonEpRegex = Regex("""(\d+)x(\d+)""")

            for (link in episodeLinks) {
                val match = seasonEpRegex.find(link.substringAfterLast("/"))
                val season = match?.groupValues?.get(1)?.toIntOrNull() ?: 1
                val ep     = match?.groupValues?.get(2)?.toIntOrNull() ?: 1
                seasonMap.getOrPut(season) { mutableListOf() }.add(ep to link)
            }

            val seasons = seasonMap.entries.sortedBy { it.key }.map { (seasonNum, eps) ->
                val sortedEps = eps.sortedBy { it.first }
                val episodes  = sortedEps.map { (epNum, epUrl) ->
                    // Defer source extraction to AnimeDekhoExtractor
                    val source = AnimeDekhoMapper.toProviderSource(
                        iframeUrl = epUrl,
                        hostType  = HostType.ANIMEDEKHO,
                        referer   = baseUrl,
                        metadata  = mapOf("isMovie" to "false", "baseUrl" to baseUrl)
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

            ProviderResult(
                id         = result.id,
                providerId = AnimeDekhoConfig.PROVIDER_ID,
                title      = result.title,
                detailUrl  = result.url,
                mediaType  = MediaType.TV,
                poster     = result.poster,
                seasons    = seasons
            )

        } else {
            // --- Movie: defer source extraction to AnimeDekhoExtractor ---
            val source = AnimeDekhoMapper.toProviderSource(
                iframeUrl = result.url,
                hostType  = HostType.ANIMEDEKHO,
                referer   = baseUrl,
                metadata  = mapOf("isMovie" to "true", "baseUrl" to baseUrl)
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
    }

    private suspend fun fetchHtml(url: String, referer: String): String? {
        val req = RequestBuilder()
            .url(url)
            .header("Referer", referer)
            .build()
        return when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> res.data.body?.toString(Charsets.UTF_8)
            else -> null
        }
    }
}
