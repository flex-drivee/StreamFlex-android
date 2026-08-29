package com.streamflex.providers.netmirror

import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.network.NetworkResult
import com.streamflex.extractors.netmirror.NetMirrorBypassManager
import kotlinx.coroutines.runBlocking
import org.junit.Test

class NetMirrorDetailsTest {
    @Test
    fun testReacherIds() = runBlocking {
        val base = "https://net52.cc"
        val bypassToken = NetMirrorBypassManager.getToken(base)
        
        val ids = listOf(
            "0RTZ57DQ6PBHH29UN5JS7U7CW4",
            "0QSK7JQ4I3WDX3YZX1CBKEB7YK",
            "0P52WN3GC5OHP25WVULFKF2OUD"
        )
        
        for (id in ids) {
            val unixTs = System.currentTimeMillis() / 1000L
            val cookieStr = "t_hash_t=$bypassToken; ott=pv; hd=on"
            val postUrl = "$base/mobile/pv/post.php?id=$id&t=$unixTs"
            
            val response = HttpClient.execute(
                RequestBuilder()
                    .url(postUrl)
                    .header("User-Agent", NetMirrorBypassManager.NATIVE_UA)
                    .header("X-Requested-With", "app.netmirror.netmirrornew")
                    .header("Cookie", cookieStr)
                    .header("Referer", "$base/mobile/home?app=1")
                    .build()
            )
            
            if (response is NetworkResult.Success) {
                val json = response.data.bodyAsString()
                println("ID: $id -> ${json.take(200)}")
            }
            kotlinx.coroutines.delay(1000)
        }
    }
}
