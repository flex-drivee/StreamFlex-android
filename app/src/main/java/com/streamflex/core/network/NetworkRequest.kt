package com.streamflex.core.network

/**
 * Internal request model used throughout StreamFlex.
 * Providers and extractors should use this instead of OkHttp directly.
 */
data class NetworkRequest(

    val url: String,

    val method: HttpMethod = HttpMethod.GET,

    val headers: Map<String, String> = emptyMap(),

    val cookies: Map<String, String> = emptyMap(),

    val referer: String? = null,

    val origin: String? = null,

    val body: ByteArray? = null,


    val timeout: Long = 30000,

    val followRedirects: Boolean = true,

    val useCookies: Boolean = true
)

enum class HttpMethod {
    GET,
    POST,
    PUT,
    PATCH,
    DELETE,
    HEAD,
    OPTIONS
}