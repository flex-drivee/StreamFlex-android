package com.streamflex.core.parser

import org.json.JSONObject

/**
 * TransportResult
 *
 * Sealed hierarchy representing the raw transport layer response received by a provider.
 * Providers never touch HTTP clients directly for scraping; instead, they receive a
 * [TransportResult] from the engine and delegate extraction to [SearchResultParser],
 * [DetailParser], or [SourceParser].
 */
sealed class TransportResult {

    /**
     * Standard HTML document string (for web scraping providers like HDHub4u detail pages).
     */
    data class HtmlResponse(val html: String, val url: String = "") : TransportResult()

    /**
     * Standard JSON Object response (for REST/JSON API providers).
     */
    data class JsonResponse(val json: JSONObject, val url: String = "") : TransportResult()

    /**
     * Typesense / Algolia / ElasticSearch hit array (for high-speed search endpoints).
     */
    data class TypesenseResponse(val hits: List<JSONObject>, val url: String = "") : TransportResult()

    /**
     * Raw text response (for playlist files, master.txt, or plain text responses).
     */
    data class TextResponse(val text: String, val url: String = "") : TransportResult()

    /**
     * Helper to extract string content regardless of transport type.
     */
    fun asString(): String {
        return when (this) {
            is HtmlResponse -> html
            is JsonResponse -> json.toString()
            is TypesenseResponse -> hits.toString()
            is TextResponse -> text
        }
    }
}
