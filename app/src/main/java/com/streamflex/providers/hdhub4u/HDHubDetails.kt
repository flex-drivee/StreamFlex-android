package com.streamflex.providers.hdhub4u

import com.streamflex.core.network.detector.HostDetector
import com.streamflex.core.network.detector.QualityDetector
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.SearchResult
import com.streamflex.core.parser.HtmlParser
import com.streamflex.core.utils.StreamLogger

/**
 * Loads a movie/episode detail page and extracts provider sources.
 *
 * This class DOES NOT resolve playable streams.
 * It only discovers host pages (HubCloud, HubDrive, etc.).
 */
class HDHubDetails {

    companion object {
        private const val PROVIDER_ID = "hdhub4u"
    }



    /**
     * Load a movie detail page.
     */
    suspend fun load(
        result: SearchResult
    ): ProviderResult {

        val pageUrl = normalizeUrl(result.url)
        StreamLogger.info(
            "HDHubDetails",
            "Loading detail page"
        )

        StreamLogger.debug(
            "HDHubDetails",
            "Title: ${result.title}"
        )

        StreamLogger.debug(
            "HDHubDetails",
            "MediaType: ${result.mediaType}"
        )

        StreamLogger.debug(
            "HDHubDetails",
            "URL: $pageUrl"
        )


        val request = RequestBuilder()
            .url(pageUrl)
            .header("Referer", HDHubConfig.DEFAULT_DOMAIN)
            .header("Cookie", HDHubConfig.COOKIE)
            .build()

        return when (val response = HttpClient.execute(request)) {

            is NetworkResult.Success -> {

                val html = response.data.bodyAsString()
                StreamLogger.debug(
                    "HDHubDetails",
                    "Downloaded HTML (${html.length} chars)"
                )

                if (result.mediaType == MediaType.MOVIE) {

                    val sources = parseSources(html)
                    StreamLogger.info(
                        "HDHubDetails",
                        "Found ${sources.size} provider source(s)"
                    )

                    sources.forEachIndexed { index, source ->

                        StreamLogger.debug(
                            "HDHubDetails",
                            "Source ${index + 1}: ${source.hostType} | ${source.url}"
                        )
                    }

                    HDHubMapper.toProviderResult(
                        providerId = PROVIDER_ID,
                        title = result.title,
                        detailUrl = pageUrl,
                        sources = sources,
                        mediaType = result.mediaType,
                        year = result.year,
                        poster = result.poster
                    )

                } else {
                    StreamLogger.warn(
                        "HDHubDetails",
                        "TV detail page detected - season parser not implemented yet"
                    )

                    // TV support will be implemented later.

                    HDHubMapper.toProviderResult(
                        providerId = PROVIDER_ID,
                        title = result.title,
                        detailUrl = pageUrl,
                        seasons = emptyList(),
                        mediaType = result.mediaType,
                        year = result.year,
                        poster = result.poster
                    )
                }
            }

            else -> {
                StreamLogger.error(
                    "HDHubDetails",
                    "Failed to load detail page: $pageUrl (${response})"
                )
                HDHubMapper.toProviderResult(
                    providerId = PROVIDER_ID,
                    title = result.title,
                    detailUrl = pageUrl,
                    sources = emptyList(),
                    mediaType = result.mediaType,
                    year = result.year,
                    poster = result.poster,
                    success = false,
                    error = "Failed to load detail page."
                )
            }
        }
    }

    /**
     * Implement in Part 2.
     */
    private fun parseSources(
        html: String
    ): List<ProviderSource> {

        val document = HtmlParser.parse(html)

        val sources = mutableListOf<ProviderSource>()
        StreamLogger.debug(
            "HDHubDetails",
            "Scanning HTML for provider links"
        )

        val elements = HtmlParser.select(
            document,
            """
        h3 a,
        h4 a,
        .page-body a,
        .entry-content a,
        article a
        """.trimIndent()
        )

        val visited = mutableSetOf<String>()

        for (element in elements) {

            var url = HtmlParser.absUrl(
                element,
                "href"
            )

            if (url.isBlank()) {
                url = HtmlParser.attr(
                    element,
                    "href"
                )
            }

            if (url.isBlank())
                continue

            if (!visited.add(url))
                continue

            if (shouldSkip(url))
                continue

            //----------------------------------------------------
            // HDHub redirect page
            //----------------------------------------------------

            if (url.contains("?id=")) {

                url = decodeRedirect(url)

                if (url.isBlank())
                    continue
            }

            //----------------------------------------------------
            // Detect host
            //----------------------------------------------------

            val hostType = HostDetector.detect(url)
            StreamLogger.debug(
                "HDHubDetails",
                "Detected host: $hostType"
            )

            if (hostType == com.streamflex.domain.models.HostType.UNKNOWN)

                continue

            StreamLogger.debug(
                "HDHubDetails",
                "Detected host: $hostType"
            )
            //----------------------------------------------------
            // Detect quality
            //----------------------------------------------------

            val quality = QualityDetector.detect(

                text = buildString {

                    append(
                        HtmlParser.text(element)
                    )
                    append(" ")
                    append(url)

                }
            )

            //----------------------------------------------------
            // Create ProviderSource
            //----------------------------------------------------

            sources += HDHubMapper.toProviderSource(

                provider = PROVIDER_ID,

                host = hostType.name,

                hostType = hostType,

                url = url,

                quality = quality,

                referer = HDHubConfig.DEFAULT_DOMAIN,

                cookies = mapOf(
                    "xla" to "s4t"
                )
            )
        }

        val finalSources =
            sources.distinctBy { it.url }

        StreamLogger.info(
            "HDHubDetails",
            "Returning ${finalSources.size} unique provider source(s)"
        )

        return finalSources
    }

    /**
     * Implement in Part 4.
     */
    private fun normalizeUrl(
        url: String
    ): String {

        val domain = getActiveDomain()

        return url.replace(
            Regex("https?://[^/]+"),
            domain
        )
    }

    @Volatile
    private var cachedDomain: String? = null

    private fun getActiveDomain(): String {

        cachedDomain?.let {
            return it
        }

        val request = RequestBuilder()
            .url(HDHubConfig.DOMAIN_CONFIG_URL)
            .build()

        val domain = when (val response = HttpClient.execute(request)) {

            is NetworkResult.Success -> {

                val json = response.data.bodyAsString()

                val obj = com.streamflex.core.parser.JsonParser
                    .parseObject(json)

                obj?.optString(
                    "HDHUB4u",
                    HDHubConfig.DEFAULT_DOMAIN
                ) ?: HDHubConfig.DEFAULT_DOMAIN
            }

            else -> HDHubConfig.DEFAULT_DOMAIN
        }

        cachedDomain = domain

        return domain
    }
    private fun shouldSkip(
        url: String
    ): Boolean {

        val lower = url.lowercase()

        return listOf(

            "facebook",
            "twitter",
            "telegram",
            "discord",
            "imdb",
            "/tag/",
            "/category/",
            "how-to-download"

        ).any {
            lower.contains(it)
        }
    }

    private fun decodeRedirect(
        url: String
    ): String {

        return runCatching {

            val request = RequestBuilder()
                .url(url)
                .header("Referer", HDHubConfig.DEFAULT_DOMAIN)
                .header("Cookie", HDHubConfig.COOKIE)
                .build()

            when (val response = HttpClient.execute(request)) {

                is NetworkResult.Success -> {

                    val html = response.data.bodyAsString()
                    StreamLogger.debug(
                        "HDHubDetails",
                        "Downloaded HTML (${html.length} chars)"
                    )

                    val regex =
                        """s\('o','([A-Za-z0-9+/=]+)'|ck\('_wp_http_\d+','([^']+)'"""
                            .toRegex()

                    val encoded = buildString {

                        regex.findAll(html).forEach {

                            val value =
                                it.groups[1]?.value
                                    ?: it.groups[2]?.value

                            if (!value.isNullOrBlank()) {
                                append(value)
                            }
                        }
                    }

                    if (encoded.isBlank()) {
                        return url
                    }

                    val step1 = String(
                        android.util.Base64.decode(
                            encoded,
                            android.util.Base64.DEFAULT
                        )
                    )

                    val step2 = String(
                        android.util.Base64.decode(
                            step1,
                            android.util.Base64.DEFAULT
                        )
                    )

                    val step3 = pen(step2)

                    val decoded = String(
                        android.util.Base64.decode(
                            step3,
                            android.util.Base64.DEFAULT
                        )
                    )

                    val json =
                        com.streamflex.core.parser.JsonParser
                            .parseObject(decoded)

                    val encodedUrl =
                        json?.optString("o")
                            ?.takeIf { it.isNotBlank() }
                            ?.let {

                                String(
                                    android.util.Base64.decode(
                                        it,
                                        android.util.Base64.DEFAULT
                                    )
                                )
                            }

                    encodedUrl ?: url
                }

                else -> url
            }

        }.getOrDefault(url)
    }
    private fun pen(
        value: String
    ): String {

        return value.map {

            when (it) {

                in 'A'..'Z' ->
                    ((it - 'A' + 13) % 26 + 'A'.code).toChar()

                in 'a'..'z' ->
                    ((it - 'a' + 13) % 26 + 'a'.code).toChar()

                else ->
                    it
            }

        }.joinToString("")
    }
}