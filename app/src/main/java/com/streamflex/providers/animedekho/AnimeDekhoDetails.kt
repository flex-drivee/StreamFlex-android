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
                    // Fetch each episode's sources
                    val sources = loadEpisodeSources(epUrl, baseUrl, isMovie = false)
                    ProviderEpisode(
                        number  = epNum,
                        title   = "Episode $epNum",
                        sources = sources
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
            // --- Movie: find term ID directly from the detail page ---
            val sources = loadEpisodeSources(result.url, baseUrl, isMovie = true)

            ProviderResult(
                id         = result.id,
                providerId = AnimeDekhoConfig.PROVIDER_ID,
                title      = result.title,
                detailUrl  = result.url,
                mediaType  = MediaType.MOVIE,
                poster     = result.poster,
                sources    = sources
            )
        }
    }

    /**
     * Probes all trdekho server slots for a single episode/movie page and
     * returns a list of ProviderSources.
     */
    private suspend fun loadEpisodeSources(
        pageUrl  : String,
        referer  : String,
        isMovie  : Boolean
    ): List<ProviderSource> = coroutineScope {

        // Fetch the episode/movie page to extract the term-ID from <body class>
        val html = fetchHtml(pageUrl, referer) ?: return@coroutineScope emptyList()
        val doc  = HtmlParser.parse(html)

        val bodyClasses = doc.body()?.classNames() ?: emptySet()
        val termId = bodyClasses
            .firstOrNull { it.startsWith("term-") && it.removePrefix("term-").all(Char::isDigit) }
            ?.removePrefix("term-")
            ?: run {
                Logger.w("[$TAG] term ID not found in body class for $pageUrl")
                return@coroutineScope emptyList()
            }

        val trtype = if (isMovie) 1 else 2

        // Probe all server slots concurrently
        val jobs = (1..AnimeDekhoConfig.MAX_SERVERS).map { serverIdx ->
            async(Dispatchers.IO) {
                fetchIframeSource(termId, serverIdx, trtype)
            }
        }

        jobs.awaitAll()
            .filterNotNull()
            .map { (iframeSrc, serverIdx) ->
                val hostType = resolveHostType(iframeSrc)
                Logger.d("[$TAG] Server $serverIdx → $hostType : $iframeSrc")
                AnimeDekhoMapper.toProviderSource(
                    iframeUrl = iframeSrc,
                    hostType  = hostType,
                    referer   = referer,
                    metadata  = mapOf("server" to serverIdx.toString())
                )
            }
    }

    /**
     * Calls /?trdekho={idx}&trid={termId}&trtype={type} on the mirror domain,
     * parses the iframe src and returns it paired with the server index.
     */
    private suspend fun fetchIframeSource(
        termId    : String,
        serverIdx : Int,
        trtype    : Int
    ): Pair<String, Int>? {
        val url = "${AnimeDekhoConfig.MIRROR_DOMAIN}/?trdekho=$serverIdx&trid=$termId&trtype=$trtype"
        val req = RequestBuilder()
            .url(url)
            .header("Referer", AnimeDekhoConfig.MIRROR_DOMAIN)
            .build()

        return when (val res = HttpClient.execute(req)) {
            is NetworkResult.Success -> {
                val html = res.data.body?.toString(Charsets.UTF_8) ?: return null
                val doc  = HtmlParser.parse(html)
                val src  = doc.selectFirst("iframe[src]")?.attr("src")
                    ?.takeIf { it.isNotBlank() } ?: return null
                src to serverIdx
            }
            else -> null
        }
    }

    /** Map iframe src domain to a StreamFlex HostType. */
    private fun resolveHostType(url: String): HostType {
        val lower = url.lowercase()
        return when {
            "abyssplayer.com" in lower || "playhydrax.com" in lower -> HostType.ABYSS
            "vidmoly" in lower                                       -> HostType.VIDMOLY
            "rubystm.com" in lower || "streamruby" in lower         -> HostType.STREAMRUBY
            "gdmirrorbot" in lower                                   -> HostType.GDMIRRORBOT
            "cloudy.upns" in lower                                   -> HostType.CLOUDY
            "turbovidhls" in lower || "emturbovid" in lower         -> HostType.TURBOVID
            "strmup.to" in lower                                     -> HostType.STREAMUP
            "xerver.xyz" in lower                                    -> HostType.REDIRECT
            "vidcloud.upns" in lower                                 -> HostType.CLOUDY
            lower.endsWith(".mp4") || lower.endsWith(".mkv")        -> HostType.DIRECT
            ".m3u8" in lower                                         -> HostType.M3U8
            else                                                     -> HostType.UNKNOWN
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
