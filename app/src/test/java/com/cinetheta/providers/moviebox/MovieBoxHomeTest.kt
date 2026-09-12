package com.cinetheta.providers.moviebox

import com.cinetheta.core.network.HttpClient
import com.cinetheta.core.network.NetworkResult
import com.cinetheta.core.network.RequestBuilder
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.json.JSONObject

class MovieBoxHomeTest {
    @Test
    fun testHomeEndpoints() = runBlocking {
        val baseUrl = "https://api6.aoneroom.com"
        val url = "$baseUrl/wefeed-mobile-bff/tab/ranking-list?tabId=0&categoryType=4516404531735022304&page=1&perPage=10"
        val headers = MovieBoxCrypto.getHeaders(method = "GET", url = url)
        val request = RequestBuilder().url(url).get().headers(headers).build()
        when (val response = HttpClient.execute(request)) {
            is NetworkResult.Success -> {
                val jsonStr = response.data.bodyAsString()
                println(jsonStr.take(1500))
            }
            else -> println("FAILED")
        }
    }
}
