package com.streamflex.providers.netmirror


import com.streamflex.domain.models.*

class NetMirrorDetails {

    suspend fun load(
        result: SearchResult,
        baseUrl: String,
        ott: String,
        providerId: String,
        providerName: String
    ): ProviderResult? {
        
        // URL format from Search: netmirror://{ott}/{id}
        val id = result.url.substringAfterLast("/")
        
        // Since /mobile/post.php is blocked by Cloudflare and desktop post.php returns Invalid User,
        // we bypass the network request entirely. We construct the ProviderSource directly.
        // For TV shows, we cannot currently fetch the full episode list without a Cloudflare bypass,
        // so this will only correctly resolve Streams for Movies for now.
        val sources = mutableListOf<ProviderSource>()
        sources += createPlayerSource(id, ott, baseUrl, providerName, result.title)
        
        return ProviderResult(
            id = id,
            providerId = providerId,
            title = result.title,
            detailUrl = result.url,
            mediaType = result.mediaType,
            sources = sources,
            seasons = emptyList(),
            year = result.year,
            poster = result.poster,
            overview = null,
            success = true
        )
    }



    private fun createPlayerSource(
        id: String,
        ott: String,
        baseUrl: String,
        providerName: String,
        title: String
    ): ProviderSource {
        // Create a custom URI scheme so our Extractor can intercept it
        val encodedTitle = android.net.Uri.encode(title)
        val playerUri = "netmirror://player?id=$id&ott=$ott&base=${baseUrl}&title=$encodedTitle"
        
        return ProviderSource(
            provider = providerName,
            host = "NetMirror API",
            hostType = HostType.NETMIRROR,
            url = playerUri,
            quality = Quality.UNKNOWN
        )
    }
}
