package com.streamflex.core.network.detector

import com.streamflex.core.network.NetworkResponse

object RedirectDetector {

    /**
     * Returns true if response is a redirect.
     */
    fun isRedirect(response: NetworkResponse): Boolean {
        return response.code in 300..399
    }

    /**
     * Returns redirect destination if available.
     */
    fun getRedirectUrl(response: NetworkResponse): String? {
        return response.headers["Location"]
            ?: response.headers["location"]
    }

    /**
     * Checks whether URL belongs to a known redirect service.
     */
    fun isRedirectHost(url: String): Boolean {
        val host = url.lowercase()

        return host.contains("hubcloud")
                || host.contains("hubdrive")
                || host.contains("hblinks")
                || host.contains("hubcdn")
                || host.contains("linkbox")
                || host.contains("new4u")
                || host.contains("filecrypt")
                || host.contains("short")
                || host.contains("bit.ly")
                || host.contains("ouo.io")
                || host.contains("adf.ly")
    }

    /**
     * Determines whether the URL should be resolved further.
     */
    fun needsResolution(url: String): Boolean {
        return isRedirectHost(url)
    }
}