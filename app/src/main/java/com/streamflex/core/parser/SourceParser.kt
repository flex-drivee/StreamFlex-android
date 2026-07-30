package com.streamflex.core.parser

import com.streamflex.domain.models.ProviderSource

/**
 * SourceParser
 *
 * Standardized contract for parsing embedded sources / player links from a
 * detail or streaming page [TransportResult] into [ProviderSource] list.
 *
 * Providers ONLY discover embed URLs (e.g. FileMoon iframe, HubCloud download link).
 * They do NOT unwrap redirects or run extractors — the [ResolverEngine] owns that pipeline.
 */
interface SourceParser {

    /**
     * Parse a transport result into a list of [ProviderSource].
     *
     * @param raw The raw transport result (e.g. [TransportResult.HtmlResponse]).
     * @param sourceUrl The original URL from which sources are being parsed.
     * @return List of discovered provider sources.
     */
    fun parse(raw: TransportResult, sourceUrl: String = ""): List<ProviderSource>

    /**
     * Convenience overload accepting any object (e.g. raw String or JSONObject).
     */
    fun parseRaw(raw: Any, sourceUrl: String = ""): List<ProviderSource> {
        val transport = when (raw) {
            is TransportResult -> raw
            is String -> TransportResult.HtmlResponse(raw, sourceUrl)
            is org.json.JSONObject -> TransportResult.JsonResponse(raw, sourceUrl)
            else -> return emptyList()
        }
        return parse(transport, sourceUrl)
    }
}
