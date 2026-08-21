package com.streamflex.providers.moviebox

import kotlinx.coroutines.runBlocking
import org.junit.Test
import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.JsonParser

class MovieBoxAudioTest {

    @Test
    fun testAudioStreams() = runBlocking {
        // Fetch token first
        val url = "https://api6.aoneroom.com/wefeed-mobile-bff/tab/ranking-list?tabId=0&categoryType=4516404531735022304&page=1&perPage=1"
        var headers = MovieBoxCrypto.getHeaders(method = "GET", url = url)
        var req = RequestBuilder().url(url).get().headers(headers).build()
        var resp = HttpClient.execute(req)
        println("Token Fetch GET response: $resp")
        if (resp is com.streamflex.core.network.NetworkResult.Success) {
            val hdrs = resp.data.headers
            println("Token Fetch Headers: $hdrs")
            resp.data.header("x-user")?.let { xu ->
                val root = JsonParser.parse(xu)
                if (root != null) {
                    val token = JsonParser.string(root, "token")
                    if (!token.isNullOrEmpty()) {
                        MovieBoxCrypto.xUserToken = token
                        println("Got Token manually: $token")
                    }
                }
            }
        }

        // Let's search for "spider-man"
        val search = MovieBoxSearch()
        val results = search.search("spider-man", "https://api6.aoneroom.com")
        println("Raw Search JSON: ${results}")
        if (results.isEmpty()) {
            println("No results found.")
            return@runBlocking
        }
        val first = results.first()
        println("Found: ${first.title} (ID: ${first.id})")

        if (resp is com.streamflex.core.network.NetworkResult.Success) {
            resp.data.header("x-user")?.let { xu ->
                val root = JsonParser.parse(xu)
                if (root != null) {
                    val token = JsonParser.string(root, "token")
                    if (!token.isNullOrEmpty()) {
                        MovieBoxCrypto.xUserToken = token
                        println("Got Token: $token")
                    }
                }
            }
        }

        // Now call play-info with se=1&ep=1
        val playUrl = "https://api6.aoneroom.com/wefeed-mobile-bff/subject-api/play-info?subjectId=${first.id}&se=1&ep=1"
        headers = MovieBoxCrypto.getHeaders(method = "GET", url = playUrl)
        
        req = RequestBuilder()
            .url(playUrl)
            .get()
            .headers(headers)
            .build()
            
        resp = HttpClient.execute(req)
        if (resp is com.streamflex.core.network.NetworkResult.Success) {
            val json = resp.data.bodyAsString()
            println("PLAY INFO JSON:")
            val root = JsonParser.parse(json)
            if (root != null) {
                val data = JsonParser.objectOf(root, "data")
                if (data != null) {
                    val streams = JsonParser.array(data, "streams")
                    println("Total Streams Found: ${streams.size}")
                    for (i in 0 until streams.size) {
                        val stream = streams.get(i)
                        val res = JsonParser.string(stream, "resolutions")
                        val lang = JsonParser.string(stream, "language")
                        println("Stream $i: Lang=$lang, Res=$res")
                    }
                } else {
                    println(json)
                }
            }
        } else {
            println("Failed to fetch play-info: $resp")
        }
    }
}
