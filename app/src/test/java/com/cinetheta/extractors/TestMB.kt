package com.cinetheta.extractors

import com.cinetheta.providers.moviebox.MovieBoxCrypto
import com.cinetheta.core.network.HttpClient
import com.cinetheta.core.network.RequestBuilder
import com.cinetheta.core.network.NetworkResult
import kotlinx.coroutines.runBlocking

object TestMB {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        MovieBoxCrypto.xUserToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1aWQiOjE3NTM5ODc4NDI2OTIyNDIyNzIsImV4cCI6MTc5NjQ4OTE5MywiaWF0IjoxNzg4NzEyODkzfQ.5tP4LMlcj-pAeLzw76DhqwtczGZF-FOJQUUr2Uv0VLY"
        val url = "https://api3.aoneroom.com/wefeed-mobile-bff/subject-api/get?subjectId=6633755939558542480"
        val headers = MovieBoxCrypto.getHeaders("GET", url, null)
        val request = RequestBuilder().url(url).get().headers(headers).build()
        val result = HttpClient.execute(request)
        if (result is NetworkResult.Success) {
            println(result.data.bodyAsString())
        } else {
            println("Failed: $result")
        }
    }
}
