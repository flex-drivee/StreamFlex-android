package com.cinetheta.providers.netmirror

import com.cinetheta.core.cache.CacheManager
import com.cinetheta.core.logger.Logger
import com.cinetheta.core.network.DomainResolver
import com.cinetheta.core.network.DomainResult
import com.cinetheta.domain.models.MediaType
import com.cinetheta.domain.models.ProviderResult
import com.cinetheta.domain.models.SearchResult
import com.cinetheta.domain.provider.Provider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class NetMirrorProvider(
    private val cacheManager: CacheManager = CacheManager(),
    private val resolver: DomainResolver = DomainResolver(cacheManager)
) : Provider {

    override val id = "netmirror"
    override val name = "All OTTs"

    override val supportedMedia = setOf(MediaType.MOVIE, MediaType.TV)

    @Volatile
    private var resolvedDomain: String? = null

    override val baseUrl: String
        get() = resolvedDomain ?: NetMirrorConfig.DEFAULT_DOMAIN

    private val searchImpl = NetMirrorSearch()
    private val detailsImpl = NetMirrorDetails()

    companion object {
        private const val TAG = "NetMirrorProvider"
        const val MANIFEST_PATH = "providers/netmirror.json"
        val OTT_LIST = listOf(
            NetMirrorConfig.OTT_NETFLIX,
            NetMirrorConfig.OTT_PRIME,
            NetMirrorConfig.OTT_HOTSTAR,
            NetMirrorConfig.OTT_DISNEY
        )
    }

    suspend fun ensureDomain(): String {
        resolvedDomain?.let { return it }

        val result = resolver.resolve(
            providerId = id,
            hardcoded = NetMirrorConfig.DEFAULT_DOMAIN,
            manifestPath = MANIFEST_PATH
        )

        val domain = when (result) {
            is DomainResult.Resolved -> {
                Logger.i("[$id] Domain resolved (${result.step}): ${result.domain}", TAG)
                result.domain
            }
            is DomainResult.Mirror -> {
                Logger.i("[$id] Using mirror [${result.mirrorIndex}]: ${result.domain}", TAG)
                result.domain
            }
            is DomainResult.Hardcoded -> {
                Logger.w("[$id] Using hardcoded fallback: ${result.domain}", TAG)
                result.domain
            }
            is DomainResult.Offline -> {
                Logger.e("[$id] Provider OFFLINE — tried: ${result.tried}", TAG)
                NetMirrorConfig.DEFAULT_DOMAIN
            }
        }

        resolvedDomain = domain
        return domain
    }

    fun resetDomain() {
        resolvedDomain = null
        resolver.invalidate(id)
    }

    override suspend fun search(query: String): List<SearchResult> = coroutineScope {
        ensureDomain()
        val currentBaseUrl = baseUrl
        val deferredResults = OTT_LIST.map { ott ->
            async {
                runCatching {
                    val displayName = when (ott.lowercase()) {
                        NetMirrorConfig.OTT_NETFLIX -> "Netflix"
                        NetMirrorConfig.OTT_PRIME -> "Amazon Prime"
                        NetMirrorConfig.OTT_HOTSTAR -> "Hotstar"
                        NetMirrorConfig.OTT_DISNEY -> "Disney+"
                        else -> ott.uppercase()
                    }
                    searchImpl.search(
                        query = query, 
                        baseUrl = currentBaseUrl, 
                        ott = ott, 
                        providerId = id, 
                        providerName = displayName
                    )
                }.onFailure {
                    Logger.e("[$id] Search failed for '$query' on OTT '$ott': ${it.message}", TAG)
                }.getOrDefault(emptyList())
            }
        }
        
        // Wait for all searches to finish and flatten the results
        val allResults = deferredResults.awaitAll().flatten()
        
        // Clean query to remove things like " Season 1" which CineTheta appends
        val isSeasonQuery = query.contains(Regex("(?i)\\s*Season\\s*\\d+"))
        val cleanQuery = query.replace(Regex("(?i)\\s*Season\\s*\\d+"), "").trim()

        // Sort results to prioritize exact matches
        allResults.sortedByDescending { result ->
            var score = 0
            if (result.title.equals(cleanQuery, ignoreCase = true)) score += 30
            else if (result.title.equals(query, ignoreCase = true)) score += 30
            else if (result.title.contains(cleanQuery, ignoreCase = true)) score += 10
            
            // If CineTheta automatically appended " Season X", prioritize TV Shows!
            if (isSeasonQuery && result.mediaType == MediaType.TV) {
                score += 50
            }
            // In general, if there is a tie between a movie and a show with the exact same name, 
            // give the series a slight edge since TV shows are searched more often.
            if (result.mediaType == MediaType.TV) {
                score += 1
            }
            score
        }
    }

    override suspend fun load(searchResult: SearchResult): ProviderResult? {
        ensureDomain()
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
            resetDomain()
        }.getOrNull()
    }
}
