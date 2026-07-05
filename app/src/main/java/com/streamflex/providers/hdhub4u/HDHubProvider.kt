package com.streamflex.providers.hdhub4u

import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.SearchResult

/**
 * Main entry point for the HDHub4u provider.
 *
 * Responsibilities:
 * - Search movies/TV shows
 * - Load detail pages
 * - (Later) Resolve playable streams via ExtractorManager
 */
class HDHubProvider {

    private val search = HDHubSearch()

    private val details = HDHubDetails()

    /**
     * Search HDHub4u.
     */
    suspend fun search(
        query: String
    ): List<SearchResult> {

        return search.search(query)
    }

    /**
     * Load a movie or TV show.
     */
    suspend fun load(
        result: SearchResult
    ): ProviderResult {

        return details.load(result)
    }
}