package com.streamflex.core.network

data class NetworkResponse(

    val url: String,

    val code: Int,

    val message: String,

    val isSuccessful: Boolean,

    val body: ByteArray?,

    val headers: Map<String, List<String>>,

    val cookies: Map<String, String> = emptyMap(),

    val contentType: String? = null,

    val mimeType: String? = null,

    val contentLength: Long = -1,

    val responseTime: Long = 0
) {

    fun bodyAsString(): String {
        return body?.toString(Charsets.UTF_8).orEmpty()
    }

    fun isHtml(): Boolean {
        return contentType?.contains("text/html", true) == true
    }

    fun isJson(): Boolean {
        return contentType?.contains("application/json", true) == true
    }

    fun isVideo(): Boolean {
        return contentType?.startsWith("video/", true) == true
    }

    fun isM3U8(): Boolean {
        return contentType?.contains("mpegurl", true) == true
    }
}