package com.streamflex

import com.streamflex.domain.models.SearchResult
import com.streamflex.providers.toonstream.ToonStreamSearch
import com.streamflex.providers.toonstream.ToonStreamDetails
import com.streamflex.extractors.toonstream.ToonStreamExtractor
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.HostType
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ToonstreamTest {
    @Test
    fun testSearchAndLoad() = runBlocking {
        println("--- Starting Search ---")
        val search = ToonStreamSearch()
        val results = search.search("naruto")
        results.forEach { println("RESULT: ${it.title} | ${it.url}") }

        val firstMovie = results.firstOrNull { it.mediaType == com.streamflex.domain.models.MediaType.MOVIE }
        val firstTv = results.firstOrNull { it.mediaType == com.streamflex.domain.models.MediaType.TV }

        val details = ToonStreamDetails()
        
        if (firstMovie != null) {
            println("--- Loading Movie: ${firstMovie.title} ---")
            val res = details.load(firstMovie, "https://toon-stream.site")
            res?.sources?.forEach { println("SOURCE: ${it.iframeUrl} | ${it.hostType}") }
        }

        if (firstTv != null) {
            println("--- Loading TV: ${firstTv.title} ---")
            val res = details.load(firstTv, "https://toon-stream.site")
            val epSource = res?.seasons?.firstOrNull()?.episodes?.firstOrNull()?.sources?.firstOrNull()
            if (epSource != null) {
                println("EPISODE SOURCE: ${epSource.iframeUrl}")
                val extractor = ToonStreamExtractor()
                val extracted = extractor.extract(epSource)
                extracted.sources.forEach {
                    println("EXTRACTED SOURCE: ${it.iframeUrl} | ${it.hostType}")
                }
            }
        }
    }
}
