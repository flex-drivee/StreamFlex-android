package com.streamflex.providers.hdhub4u

import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.SearchResult
import com.streamflex.domain.provider.Provider

/**
 * HDHub4u provider implementation.
 *
 * Responsibilities:
 * - Search movies and TV shows.
 * - Load provider content.
 *
 * This class intentionally contains no scraping logic.
 *
 * Search parsing is handled by:
 *      HDHubSearch
 *
 * Detail parsing is handled by:
 *      HDHubDetails
 */
class HDHubProvider : Provider {

    override val id = "hdhub4u"

    override val name = "HDHub4u"

    override val baseUrl =
        "https://new2.hdhub4u.cl/?utm=mn1"

    override val supportedMedia = setOf(
        MediaType.MOVIE,
        MediaType.TV
    )

    private val search = HDHubSearch()

    private val details = HDHubDetails()

    /**
     * Search HDHub4u.
     */
    override suspend fun search(
        query: String
    ): List<SearchResult> {

        return search.search(query)
    }

    /**
     * Load provider data.
     *
     * Movies:
     *      ProviderResult.sources
     *
     * TV Shows:
     *      ProviderResult.seasons
     */
    override suspend fun load(
        searchResult: SearchResult
    ): ProviderResult? {

        return runCatching {

            details.load(searchResult)

        }.onFailure {

            // Logger.e(...)
            // (We'll hook our Logger here later.)

        }.getOrNull()
    }
}