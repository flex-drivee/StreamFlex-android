package com.streamflex.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.streamflex.extractors.moviebox.MovieBoxExtractor
import com.streamflex.providers.moviebox.MovieBoxDetails
import com.streamflex.providers.moviebox.MovieBoxSearch
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.HostType
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import android.util.Log

@RunWith(AndroidJUnit4::class)
class MovieBoxTest {

    @Test
    fun testMovieBoxSpiderMan() = runBlocking {
        Log.d("MovieBoxTest", "Starting Search...")
        val searcher = MovieBoxSearch()
        val results = searcher.search("Spider-Man No Way Home", "https://mbpapi.aoneroom.com")
        
        Log.d("MovieBoxTest", "Found ${results.size} results.")
        val first = results.firstOrNull()
        if (first == null) {
            Log.d("MovieBoxTest", "No results found.")
            return@runBlocking
        }
        
        Log.d("MovieBoxTest", "Fetching details for: ${first.title} (ID: ${first.id})")
        val details = MovieBoxDetails()
        val providerResult = details.load(first, "https://mbpapi.aoneroom.com")
        
        if (providerResult == null) {
            Log.d("MovieBoxTest", "No details found.")
            return@runBlocking
        }
        
        Log.d("MovieBoxTest", "Extracting streams for play info...")
        val extractor = MovieBoxExtractor()
        
        // Movie has only 1 provider source
        val source = providerResult.sources.firstOrNull()
        if (source == null) {
            Log.d("MovieBoxTest", "No provider sources found.")
            return@runBlocking
        }
        
        val streams = extractor.extract(source)
        Log.d("MovieBoxTest", "Extracted ${streams.streams.size} streams.")
        streams.streams.forEach { stream ->
            Log.d("MovieBoxTest", "Stream: ${stream.name}")
        }
    }
}
