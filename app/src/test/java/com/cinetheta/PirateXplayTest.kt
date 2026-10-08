package com.cinetheta

import com.cinetheta.core.network.detector.HostDetector
import com.cinetheta.domain.models.SearchResult
import com.cinetheta.domain.models.MediaType
import com.cinetheta.providers.piratexplay.PirateXplayDetails
import com.cinetheta.providers.piratexplay.PirateXplayExtractor
import com.cinetheta.providers.piratexplay.PirateXplaySearch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

class PirateXplayTest {

    @Test
    fun testSearchAndLoad() = runBlocking {
        println("Starting PirateXplay Test...")
        
        // 1. Test Search
        val search = PirateXplaySearch()
        val results = search.search("naruto", "https://piratexplay.cc")
        println("Search Results: ${results.size}")
        assertTrue("Search should return results", results.isNotEmpty())
        
        val tvShow = results.firstOrNull { it.mediaType == MediaType.TV }
        assertNotNull("Should find a TV show", tvShow)
        println("Found TV Show: ${tvShow?.title} | URL: ${tvShow?.url}")
        
        // 2. Test Details (Load)
        val details = PirateXplayDetails()
        val result = details.load(tvShow!!, "https://piratexplay.cc")
        assertNotNull("Details should not be null", result)
        println("Loaded Details: ${result?.title}")
        println("Seasons: ${result?.seasons?.size}")
        
        val firstSeason = result?.seasons?.firstOrNull()
        val firstEpisode = firstSeason?.episodes?.firstOrNull()
        assertNotNull("Should have at least one episode", firstEpisode)
        println("First Episode: ${firstEpisode?.title} | Num: ${firstEpisode?.number}")
        
        val source = firstEpisode?.sources?.firstOrNull()
        assertNotNull("Should have a provider source", source)
        println("Source URL: ${source?.url} | Host: ${source?.hostType}")
        
        // 3. Test Extractor
        val extractor = PirateXplayExtractor()
        val extractionResult = extractor.extract(source!!)
        
        println("Extraction Sources: ${extractionResult.sources.size}")
        for (s in extractionResult.sources) {
            println(" -> Found Extracted Source: ${s.host} | URL: ${s.url} | HostType: ${s.hostType}")
        }
        
        assertTrue("Extractor should find inner sources", extractionResult.sources.isNotEmpty())
    }
}
