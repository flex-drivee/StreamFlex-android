package com.streamflex.providers.netmirror

import com.streamflex.core.cache.CacheManager
import com.streamflex.core.logger.Logger
import com.streamflex.domain.models.MediaType
import com.streamflex.domain.models.ProviderResult
import com.streamflex.domain.models.SearchResult
import com.streamflex.domain.provider.Provider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class NetMirrorProvider(
    private val cacheManager: CacheManager = CacheManager()
) : Provider {

    override val id = "netmirror"
    override val name = "NetMirror (All OTTs)"

    override val supportedMedia = setOf(MediaType.MOVIE, MediaType.TV)
    
    override val baseUrl: String = NetMirrorConfig.DEFAULT_DOMAIN

    private val searchImpl = NetMirrorSearch()
    private val detailsImpl = NetMirrorDetails()

    companion object {
        private const val TAG = "NetMirrorProvider"
        val OTT_LIST = listOf(
            NetMirrorConfig.OTT_NETFLIX,
            NetMirrorConfig.OTT_PRIME,
            NetMirrorConfig.OTT_HOTSTAR,
            NetMirrorConfig.OTT_DISNEY
        )
    }

    override suspend fun search(query: String): List<SearchResult> = coroutineScope {
        val deferredResults = OTT_LIST.map { ott ->
            async {
                runCatching {
                    searchImpl.search(
                        query = query, 
                        baseUrl = baseUrl, 
                        ott = ott, 
                        providerId = id, 
                        providerName = "NetMirror (${ott.uppercase()})"
                    )
                }.onFailure {
                    Logger.e("[$id] Search failed for '$query' on OTT '$ott': ${it.message}", TAG)
                }.getOrDefault(emptyList())
            }
        }
        
        // Wait for all searches to finish and flatten the results
        deferredResults.awaitAll().flatten()
    }

    override suspend fun load(searchResult: SearchResult): ProviderResult? {
        return runCatching {
            // NetMirrorSearch constructs detail URLs as: netmirror://<ott>/<id>
            // We need to extract the OTT value to fetch the details correctly.
            val ott = searchResult.url.substringAfter("netmirror://").substringBefore("/")
            
            // Fallback if parsing fails for some reason
            val resolvedOtt = if (ott.isNotBlank() && ott != searchResult.url) ott else NetMirrorConfig.OTT_NETFLIX

            detailsImpl.load(
                result = searchResult,
                baseUrl = baseUrl,
                ott = resolvedOtt,
                providerId = id,
                providerName = searchResult.providerName
            )
        }.onFailure {
            Logger.e("[$id] Load failed for '${searchResult.title}': ${it.message}", TAG)
        }.getOrNull()
    }
}
