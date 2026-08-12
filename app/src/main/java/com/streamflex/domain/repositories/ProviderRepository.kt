package com.streamflex.domain.repositories

import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.SearchResult
import com.streamflex.domain.provider.Provider

/**
 * Central repository for every provider.
 *
 * Providers are responsible for:
 * - search
 * - loading pages
 * - producing ProviderSources
 *
 * This repository never performs extraction.
 */
class ProviderRepository(

    private val providers: List<Provider>

) {

    /**
     * Currently selected provider ID.
     * If set, only this provider will be searched.
     */
    var selectedProviderId: String? = null

    /**
     * Search all enabled providers.
     */
    suspend fun search(
        query: String
    ): List<SearchResult> {

        val targetProviders = if (selectedProviderId != null) {
            providers.filter { it.enabled && it.id == selectedProviderId }
        } else {
            providers.filter { it.enabled }
        }

        return targetProviders

            .flatMap { provider ->

                runCatching {

                    provider.search(query)

                }.getOrDefault(emptyList())

            }
    }

    /**
     * Load a selected search result.
     */
    suspend fun load(

        providerId: String,

        item: SearchResult

    ): ProviderResult? {

        val provider = providers.firstOrNull {

            it.id == providerId

        } ?: return null

        return runCatching {

            provider.load(item)

        }.getOrNull()
    }

    /**
     * Returns every enabled provider.
     */
    fun enabledProviders(): List<Provider> {

        return providers.filter {

            it.enabled

        }
    }

    /**
     * Returns a provider by id.
     */
    fun provider(

        id: String

    ): Provider? {

        return providers.firstOrNull {

            it.id == id

        }
    }
}