package com.streamflex.core.utils

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

object UrlUtils {

    /**
     * Returns true if the URL is valid.
     */
    fun isValid(url: String): Boolean {
        return runCatching {
            URI(url)
            true
        }.getOrElse { false }
    }

    /**
     * Returns the domain.
     *
     * https://hubcloud.lol/file/123
     * ->
     * hubcloud.lol
     */
    fun getHost(url: String): String? {
        return runCatching {
            URI(url).host
        }.getOrNull()
    }

    /**
     * Returns the scheme.
     *
     * https
     * http
     */
    fun getScheme(url: String): String? {
        return runCatching {
            URI(url).scheme
        }.getOrNull()
    }

    /**
     * Returns the path.
     */
    fun getPath(url: String): String? {
        return runCatching {
            URI(url).path
        }.getOrNull()
    }

    /**
     * Returns the file extension.
     */
    fun extension(url: String): String? {

        val path = getPath(url) ?: return null

        val index = path.lastIndexOf('.')

        if (index == -1) return null

        return path.substring(index + 1).lowercase()
    }

    /**
     * URL Encode.
     */
    fun encode(value: String): String {
        return URLEncoder.encode(value, Charsets.UTF_8.name())
    }

    /**
     * URL Decode.
     */
    fun decode(value: String): String {
        return URLDecoder.decode(value, Charsets.UTF_8.name())
    }

    /**
     * Removes trailing slash.
     */
    fun removeTrailingSlash(url: String): String {
        return url.removeSuffix("/")
    }

    /**
     * Join base URL with relative path.
     */
    fun join(base: String, path: String): String {

        if (path.startsWith("http")) {
            return path
        }

        return removeTrailingSlash(base) +
                "/" +
                path.removePrefix("/")
    }

    /**
     * Returns true for HTTP/HTTPS URLs.
     */
    fun isHttp(url: String): Boolean {

        return url.startsWith("http://") ||
                url.startsWith("https://")
    }
}