package com.streamflex.core.network

import okhttp3.Headers

class RequestBuilder {

    private var url: String = ""
    private var method: String = "GET"
    private var headers = Headers.Builder()
    private var body: ByteArray? = null
    private var followRedirects: Boolean = true
    private var useCookies: Boolean = true
    private var timeout: Long = 30_000L

    fun url(url: String) = apply {
        this.url = url
    }

    fun method(method: String) = apply {
        this.method = method.uppercase()
    }

    fun get() = apply {
        method = "GET"
    }

    fun post(body: ByteArray) = apply {
        method = "POST"
        this.body = body
    }

    fun put(body: ByteArray) = apply {
        method = "PUT"
        this.body = body
    }

    fun delete() = apply {
        method = "DELETE"
    }

    fun header(name: String, value: String) = apply {
        headers[name] = value
    }

    fun headers(map: Map<String, String>) = apply {
        map.forEach { (key, value) ->
            headers[key] = value
        }
    }

    fun body(body: ByteArray) = apply {
        this.body = body
    }

    fun followRedirects(enabled: Boolean) = apply {
        this.followRedirects = enabled
    }

    fun useCookies(enabled: Boolean) = apply {
        this.useCookies = enabled
    }

    fun timeout(timeout: Long) = apply {
        this.timeout = timeout
    }

    fun build(): NetworkRequest {
        require(url.isNotBlank()) {
            "Request URL cannot be empty."
        }

        return NetworkRequest(
            url = url,
            method = method,
            headers = headers.build(),
            body = body,
            followRedirects = followRedirects,
            useCookies = useCookies,
            timeout = timeout
        )
    }
}