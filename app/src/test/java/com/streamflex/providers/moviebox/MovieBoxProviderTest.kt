package com.streamflex.providers.moviebox

import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.RequestBuilder
import com.streamflex.domain.models.SearchResult
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.HostType
import org.json.JSONObject

class MovieBoxProviderTest {
    @Test
    fun testMultiAudio() = runBlocking {
        // We'll manually emulate the play-info API call to see its response
        // Spider-Man: Brand New Day or any movie ID.
        // First search to get ID
        val search = MovieBoxSearch()
        val results = search.search("spider man", "https://api3.aoneroom.com")
        if (results.isEmpty()) {
            println("No results found.")
            return@runBlocking
        }
        val first = results.first()
        println("Found: ${first.title} (ID: ${first.id})")

        // Now call play-info
        val playUrl = "https://api3.aoneroom.com/wefeed-mobile-bff/subject-api/play-info?subjectId=${first.id}"
        val headers = MovieBoxCrypto.getHeaders(method = "GET", url = playUrl)
        
        val req = RequestBuilder()
            .url(playUrl)
            .get()
            .headers(headers)
            .build()
            
        val resp = HttpClient.execute(req)
        if (resp is com.streamflex.core.network.NetworkResult.Success) {
            val json = resp.data.bodyAsString()
            println("PLAY INFO JSON:")
            println(JSONObject(json).toString(2))
        } else {
            println("Failed to fetch play-info")
        }
    }
}
