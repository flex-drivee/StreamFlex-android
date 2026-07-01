package com.streamflex.core.network

import okhttp3.JavaNetCookieJar
import java.net.CookieManager
import java.net.CookiePolicy

object Cookies {

    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    val cookieJar = JavaNetCookieJar(cookieManager)

    fun clear() {
        cookieManager.cookieStore.removeAll()
    }
    
}