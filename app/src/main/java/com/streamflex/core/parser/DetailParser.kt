package com.streamflex.core.parser

import com.streamflex.domain.models.ProviderResult

/**
 * DetailParser
 *
 * Standardized contract for parsing a content detail page (movie or show)
 * from a [TransportResult] into a canonical [ProviderResult].
 *
 * Keeps HTML/JSON extraction logic isolated from provider network fetching.
 */
interface DetailParser {

    /**
     * Parse a transport result into a [ProviderResult].
     *
     * @param raw The raw transport result (e.g. [TransportResult.HtmlResponse]).
     * @param detailUrl The original URL of the detail page being parsed.
     * @return Parsed [ProviderResult] containing metadata and initial sources or seasons.
     */
    fun parse(raw: TransportResult, detailUrl: String = ""): ProviderResult

    /**
     * Convenience overload accepting any object (e.g. raw String or JSONObject).
     */
    fun parseRaw(raw: Any, detailUrl: String = ""): ProviderResult {
        val transport = when (raw) {
            is TransportResult -> raw
            is String -> TransportResult.HtmlResponse(raw, detailUrl)
            is org.json.JSONObject -> TransportResult.JsonResponse(raw, detailUrl)
            else -> throw IllegalArgumentException("Unsupported transport type: ${raw.javaClass.simpleName}")
        }
        return parse(transport, detailUrl)
    }
}
