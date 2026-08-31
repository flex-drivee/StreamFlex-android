package com.streamflex

import android.net.Uri
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UriTest {
    @Test
    fun testUri() {
        val uri = Uri.parse("https:///files/80057281/720p.m3u8")
        println("HOST IS: " + uri.host)
        assert(true)
    }
}
