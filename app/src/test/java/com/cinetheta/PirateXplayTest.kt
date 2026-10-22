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
        
        assertTrue("Extractor should find inner sources", extractionResult.sources.isNotEmpty() || extractionResult.streams.isNotEmpty())

        val streams = com.cinetheta.extractors.ExtractorManager.extract(source)
        println("Playable Streams: ${streams.size}")
        for (st in streams) {
            println(" -> Found Playable Stream: ${st.name} | ${st.url}")
        }
        assertTrue("ExtractorManager should produce playable stream(s)", streams.isNotEmpty())
    }

    @Test
    fun testJujutsuKaisenNoRecommendations() = runBlocking {
        println("Starting Jujutsu Kaisen Multi-Season & Recommendation Filtering Test...")
        val details = PirateXplayDetails()
        val dummyResult = SearchResult(
            id = "jujutsu-kaisen-season-3-95479",
            title = "Jujutsu Kaisen",
            url = "https://piratexplay.cc/series/jujutsu-kaisen-season-3-95479",
            mediaType = MediaType.TV,
            providerId = "piratexplay",
            providerName = "PirateXplay"
        )
        val result = details.load(dummyResult, "https://piratexplay.cc")
        assertNotNull("Result should not be null", result)
        println("Total seasons loaded: ${result?.seasons?.size}")
        assertTrue("Should have at least 3 seasons", (result?.seasons?.size ?: 0) >= 3)

        val season1 = result?.seasons?.firstOrNull { it.number == 1 }
        assertNotNull("Season 1 should exist", season1)
        println("Season 1 episode count: ${season1?.episodes?.size}")
        assertTrue("Season 1 should have real episodes (e.g. 24 eps)", (season1?.episodes?.size ?: 0) >= 20)

        // Verify none of the episodes have recommendation titles
        for (season in result?.seasons.orEmpty()) {
            for (ep in season.episodes) {
                org.junit.Assert.assertFalse("Should not contain Doraemon: ${ep.title}", ep.title.contains("Doraemon", ignoreCase = true))
                org.junit.Assert.assertFalse("Should not contain Tokyo Ghoul: ${ep.title}", ep.title.contains("Tokyo Ghoul", ignoreCase = true))
                org.junit.Assert.assertFalse("Should not contain Apothecary: ${ep.title}", ep.title.contains("Apothecary", ignoreCase = true))
                assertTrue("Episode source must contain /episode/: ${ep.sources.firstOrNull()?.url}", ep.sources.firstOrNull()?.url?.contains("/episode/") == true)
            }
        }
        println("All episodes verified successfully! No recommendation videos found.")
    }
}
