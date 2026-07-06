package com.streamflex.extractors.shared

import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResponse
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.HtmlParser
import com.streamflex.core.parser.JsonParser

/**
 * Shared helper used by all extractors.
 *
 * Contains reusable networking and parsing operations.
 * Host-specific logic should remain inside individual extractors.
 */
object ExtractorHelper {

    /**
     * Perform a GET request.
     */
    fun get(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): NetworkResult<NetworkResponse> {

        val builder = RequestBuilder()
            .url(url)

        headers.forEach { (key, value) ->
            builder.header(key, value)
        }

        return HttpClient.execute(builder.build())
    }

    /**
     * Download HTML.
     */
    fun getHtml(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): String {

        return when (val response = get(url, headers)) {

            is NetworkResult.Success ->
                response.data.bodyAsString()

            else ->
                ""
        }
    }

    /**
     * Parse HTML.
     */
    fun parseHtml(html: String) =
        HtmlParser.parse(html)

    /**
     * Parse JSON object.
     */
    fun parseJson(text: String) =
        JsonParser.parseObject(text)
}