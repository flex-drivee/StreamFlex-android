package com.streamflex.domain.provider

import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.SearchResult
import com.streamflex.domain.models.MediaType

/**
 * Base contract for every streaming provider.
 *
 * Examples:
 * - HDHub4u
 * - MovieBox
 * - OTTMirror
 * - NetMirror
 */
interface Provider {

    /**
     * Provider display name.
     */
    val name: String

    /**
     * Stable provider identifier.
     */
    val id: String

    /**
     * Main website.
     */
    val baseUrl: String

    /**
     * Whether this provider is enabled.
     */
    val enabled: Boolean
        get() = true

    /**
     * Supported content.
     */
    val supportedMedia: Set<MediaType>

    /**
     * Search content.
     */
    suspend fun search(
        query: String
    ): List<SearchResult>

    /**
     * Load a selected item.
     *
     * Returns provider sources,
     * NOT final streams.
     */
    suspend fun load(
        searchResult: SearchResult
    ): ProviderResult?
}