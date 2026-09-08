package com.cinetheta

import com.cinetheta.providers.toonstream.ToonStreamDetails
import com.cinetheta.domain.models.SearchResult
import com.cinetheta.domain.models.MediaType
import kotlinx.coroutines.runBlocking

object DetailsTest {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val result = SearchResult(
            id = "test",
            url = "https://toon-stream.site/series/hunter-x-hunter-hindi-dub",
            providerId = "toonstream",
            providerName = "ToonStream",
            title = "Hunter x Hunter",
            poster = "",
            mediaType = MediaType.TV
        )
        val details = ToonStreamDetails().load(result, "https://toon-stream.site")
        
        details?.seasons?.forEach { season ->
            println("Season \${season.number} eps: \${season.episodes.size}")
            if (season.episodes.isNotEmpty()) {
                println("  First EP: \${season.episodes.first().title} -> \${season.episodes.first().sources.first().url}")
            }
        }
    }
}
