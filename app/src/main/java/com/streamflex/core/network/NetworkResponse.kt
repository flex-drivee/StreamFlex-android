package com.streamflex.core.network

/**
 * Standard network response used throughout StreamFlex.
 * All OkHttp responses should be converted into this model.
 */
data class NetworkResponse(

    /** Final URL after redirects */
    val url: String,

    /** HTTP status code (200, 404, etc.) */
    val code: Int,

    /** True when status code is 2xx */
    val isSuccessful: Boolean,

    /** Response body as String */
    val body: String,

    /** Raw response bytes (videos, images, etc.) */
    val bytes: ByteArray? = null,

    /** Response headers */
    val headers: Map<String, String> = emptyMap(),

    /** Cookies received from server */
    val cookies: Map<String, String> = emptyMap(),

    /** Content-Type header */
    val contentType: String? = null,

    /** Mime type (video/mp4, text/html, etc.) */
    val mimeType: String? = null,

    /** Response size in bytes */
    val contentLength: Long = -1L,

    /** Response time in milliseconds */
    val responseTime: Long = 0L
)