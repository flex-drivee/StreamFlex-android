package com.streamflex.providers.moviebox

import org.junit.Test
import org.junit.Assert.*

class MovieBoxCryptoTest {
    @Test
    fun testSignature() {
        val url = "https://api3.aoneroom.com/wefeed-mobile-bff/tab/ranking-list?tabId=0&categoryType=4516404531735022304&page=1&perPage=1"
        val headers = MovieBoxCrypto.getHeaders("GET", url = url)
        println("Headers: $headers")
        assertNotNull(headers["X-Tr-Signature"])
    }
}
