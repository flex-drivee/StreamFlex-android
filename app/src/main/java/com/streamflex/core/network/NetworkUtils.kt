package com.streamflex.core.network

import okhttp3.Headers
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object NetworkUtils {

    /**
     * Returns the base domain.
     * https://hubcloud.lol/file/123
     * -> https://hubcloud.lol
     */
    fun getBaseUrl(url: String): String {
        val parsed = URL(url)
        return "${parsed.protocol}://${parsed.host}"
    }

    /**
     * Returns the host only.
     * https://hubcloud.lol/file/123
     * -> hubcloud.lol
     */
    fun getHost(url: String): String {
        return URL(url).host
    }

    /**
     * Resolve relative URLs.
     */
    fun resolveUrl(base: String, path: String): String {
        return URL(URL(base), path).toString()
    }

    /**
     * Encode URL.
     */
    fun encode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
    }

    /**
     * Decode URL.
     */
    fun decode(value: String): String {
        return URLDecoder.decode(
            value,
            StandardCharsets.UTF_8.toString()
        )
    }

    /**
     * Returns true if URL is valid.
     */
    fun isValidUrl(url: String): Boolean {
        return try {
            URL(url)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Remove trailing slash.
     */
    fun normalizeUrl(url: String): String {
        return url.trimEnd('/')
    }

    /**
     * Returns file extension.
     */
    fun getExtension(url: String): String {
        return url.substringAfterLast('.', "")
            .substringBefore('?')
            .lowercase()
    }

    /**
     * Merge two header sets.
     */
    fun mergeHeaders(
        first: Headers,
        second: Headers
    ): Headers {

        val builder = Headers.Builder()

        first.forEach {
            builder.add(it.first, it.second)
        }

        second.forEach {
            builder.set(it.first, it.second)
        }

        return builder.build()
    }

    /**
     * Check if URL belongs to host.
     */
    fun isHost(url: String, host: String): Boolean {
        return getHost(url)
            .contains(host, ignoreCase = true)
    }

}