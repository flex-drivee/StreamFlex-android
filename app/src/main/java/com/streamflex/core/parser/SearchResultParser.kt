package com.streamflex.core.parser

import com.streamflex.domain.models.SearchResult

/**
 * SearchResultParser
 *
 * Standardized contract for parsing search results from a [TransportResult]
 * (HTML page, JSON API, or Typesense search index) into canonical [SearchResult] list.
 *
 * Every provider search implementation must use a class implementing this interface,
 * keeping network transport separate from HTML/JSON parsing logic.
 */
interface SearchResultParser {

    /**
     * Parse a transport result into a list of [SearchResult].
     *
     * @param raw The raw transport result ([TransportResult.HtmlResponse], [TransportResult.TypesenseResponse], etc.)
     * @return List of parsed search results, or empty list if none found.
     */
    fun parse(raw: TransportResult): List<SearchResult>

    /**
     * Convenience overload accepting any object (e.g. raw String or JSONObject).
     */
    fun parseRaw(raw: Any): List<SearchResult> {
        val transport = when (raw) {
            is TransportResult -> raw
            is String -> TransportResult.HtmlResponse(raw)
            is org.json.JSONObject -> TransportResult.JsonResponse(raw)
            else -> return emptyList()
        }
        return parse(transport)
    }
}
