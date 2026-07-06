package com.streamflex.extractors.shared

import com.streamflex.core.network.HttpClient
import com.streamflex.core.network.NetworkResponse
import com.streamflex.core.network.NetworkResult
import com.streamflex.core.network.RequestBuilder
import com.streamflex.core.parser.HtmlParser
import com.streamflex.core.parser.JsonParser
import org.jsoup.nodes.Document
import org.json.JSONArray
import org.json.JSONObject

/**
 * Shared helper used by all extractors.
 *
 * Handles networking and parsing so extractors only contain
 * host-specific logic.
 */
object ExtractorHelper {

    /**
     * Execute GET request.
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
     * Download raw text.
     */
    fun getText(
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
     * Download HTML.
     */
    fun getHtml(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): String {

        return getText(url, headers)
    }

    /**
     * Download and parse HTML.
     */
    fun fetchDocument(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): Document {

        return HtmlParser.parse(
            getHtml(url, headers),
            url
        )
    }

    /**
     * Parse HTML.
     */
    fun parseHtml(
        html: String,
        baseUrl: String = ""
    ): Document {

        return if (baseUrl.isBlank()) {

            HtmlParser.parse(html)

        } else {

            HtmlParser.parse(
                html,
                baseUrl
            )
        }
    }

    /**
     * Parse JSON object.
     */
    fun parseJsonObject(
        text: String
    ): JSONObject? {

        return JsonParser.parseObject(text)
    }

    /**
     * Returns true if request succeeded.
     */
    fun exists(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): Boolean {

        return get(url, headers) is NetworkResult.Success
    }

    /**
     * Parse JSON array.
     */
    fun parseJsonArray(
        text: String
    ): JSONArray {

        return JsonParser.parseArray(text)
    }

}