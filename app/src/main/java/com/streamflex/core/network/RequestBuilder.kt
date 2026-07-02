package com.streamflex.core.network

/**
 * Fluent builder for creating NetworkRequest objects.
 * Keeps providers/extractors independent of OkHttp.
 */
class RequestBuilder {

    private var url: String = ""

    private var method: HttpMethod = HttpMethod.GET

    private val headers = mutableMapOf<String, String>()

    private val cookies = mutableMapOf<String, String>()

    private var referer: String? = null

    private var origin: String? = null

    private var body: ByteArray? = null

    private var followRedirects = true

    private var useCookies = true

    private var timeout: Long = 30_000L

    fun url(url: String) = apply {
        this.url = url
    }

    fun method(method: HttpMethod) = apply {
        this.method = method
    }

    fun get() = apply {
        method = HttpMethod.GET
    }

    fun post(body: ByteArray? = null) = apply {
        method = HttpMethod.POST
        this.body = body
    }

    fun put(body: ByteArray? = null) = apply {
        method = HttpMethod.PUT
        this.body = body
    }

    fun patch(body: ByteArray? = null) = apply {
        method = HttpMethod.PATCH
        this.body = body
    }

    fun delete() = apply {
        method = HttpMethod.DELETE
    }

    fun head() = apply {
        method = HttpMethod.HEAD
    }

    fun options() = apply {
        method = HttpMethod.OPTIONS
    }

    fun header(name: String, value: String) = apply {
        headers[name] = value
    }

    fun headers(values: Map<String, String>) = apply {
        headers.putAll(values)
    }

    fun cookie(name: String, value: String) = apply {
        cookies[name] = value
    }

    fun referer(value: String) = apply {
        referer = value
    }

    fun origin(value: String) = apply {
        origin = value
    }

    fun body(body: ByteArray?) = apply {
        this.body = body
    }

    fun followRedirects(enabled: Boolean) = apply {
        followRedirects = enabled
    }

    fun useCookies(enabled: Boolean) = apply {
        useCookies = enabled
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
            headers = headers.toMap(),
            cookies = cookies.toMap(),
            referer = referer,
            origin = origin,
            body = body,
            followRedirects = followRedirects,
            useCookies = useCookies,
            timeout = timeout
        )
    }
}