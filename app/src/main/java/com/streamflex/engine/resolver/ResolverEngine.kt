package com.streamflex.engine.resolver

import com.streamflex.core.constants.Constants
import com.streamflex.core.logger.Logger
import com.streamflex.core.network.detector.HostDetector
import com.streamflex.core.utils.StreamLogger
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.StreamLink
import com.streamflex.extractors.ExtractorManager
import com.streamflex.extractors.ExtractorRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap

/**
 * ResolverEngine
 *
 * Central orchestrator for the StreamFlex stream resolution pipeline (Phase 1.5).
 *
 * ─── The Resolution Chain ───────────────────────────────────────────────────
 * Instead of each provider implementing custom HTML scrapers or redirect logic,
 * providers simply return initial [ProviderSource] embed URLs.
 * [ResolverEngine] owns and executes the 12-stage resolution flow:
 *
 *   ProviderSource (Embed / Short-link / Iframe)
 *        ↓
 *   Stage 1: Direct Link Fast-Path (MP4, M3U8, Google Video)
 *        ↓
 *   Stage 2: Gateway & Redirect Resolver (gamerxyt, go.php, redirect.php)
 *        ↓
 *   Stage 3: Iframe & Host Resolution via [ExtractorRegistry]
 *        ↓
 *   Stage 4: Extractor Dispatch via [ExtractorManager]
 *        ↓
 *   Stage 5: Header Injection & Validation (Referer/Origin from registry.json)
 *        ↓
 *   Player-Ready [StreamLink] list for [StreamEngine]
 */
object ResolverEngine {

    private const val TAG = "ResolverEngine"

    /**
     * Resolves a list of [ProviderSource] objects into player-ready [StreamLink]s.
     * Uses coroutines to resolve independent sources concurrently.
     *
     * @param sources The list of initial embed sources discovered by a provider.
     * @return List of unique, header-validated [StreamLink]s ready for StreamEngine.
     */
    suspend fun resolveAll(sources: List<ProviderSource>): List<StreamLink> = coroutineScope {
        if (sources.isEmpty()) {
            StreamLogger.warn(TAG, "No provider sources to resolve")
            return@coroutineScope emptyList()
        }

        StreamLogger.info(TAG, "Starting resolver pipeline for ${sources.size} source(s)")

        // Track visited URLs across concurrent resolutions to avoid duplicate work
        val visitedUrls = ConcurrentHashMap.newKeySet<String>()

        val jobs = sources.map { source ->
            async {
                resolveInternal(source, depth = 0, visitedUrls = visitedUrls)
            }
        }

        val allStreams = jobs.awaitAll().flatten()

        // Remove duplicate stream URLs while retaining highest priority
        val distinctStreams = allStreams
            .distinctBy { it.url }
            .map { injectRegistryHeaders(it) }

        StreamLogger.info(
            TAG,
            "Resolver pipeline complete. Produced ${distinctStreams.size} playable stream(s)"
        )

        distinctStreams
    }

    /**
     * Resolves a single [ProviderSource] into playable [StreamLink]s.
     *
     * @param source The initial provider source.
     * @return List of extracted stream links.
     */
    suspend fun resolve(source: ProviderSource): List<StreamLink> {
        val visited = ConcurrentHashMap.newKeySet<String>()
        val streams = resolveInternal(source, depth = 0, visitedUrls = visited)
        return streams.distinctBy { it.url }.map { injectRegistryHeaders(it) }
    }

    /**
     * Internal recursive resolution loop with redirect hop guarding.
     */
    private suspend fun resolveInternal(
        source: ProviderSource,
        depth: Int,
        visitedUrls: MutableSet<String>
    ): List<StreamLink> {
        // 1. Redirect loop & depth protection
        if (depth >= Constants.MAX_REDIRECT_HOPS) {
            StreamLogger.warn(TAG, "Max redirect hops (${Constants.MAX_REDIRECT_HOPS}) exceeded for ${source.url}")
            return emptyList()
        }

        if (!visitedUrls.add(source.url)) {
            StreamLogger.debug(TAG, "Skipping already visited URL: ${source.url}")
            return emptyList()
        }

        StreamLogger.debug(
            TAG,
            "Hop $depth: Resolving [${source.hostType}] -> ${source.url}"
        )

        // 2. Fast-Path: Check if source is already a direct playable stream
        if (HostDetector.isDirect(source.hostType)) {
            StreamLogger.debug(TAG, "Direct stream detected: ${source.url}")
            val directLink = StreamLink(
                name = "${source.provider} • Direct",
                url = source.url,
                quality = source.quality,
                host = source.hostType,
                headers = source.headers,
                cookies = source.cookies,
                referer = source.referer
            )
            return listOf(directLink)
        }

        // 3. Extractor Dispatch via ExtractorManager & ExtractorRegistry
        return try {
            val extractedStreams = ExtractorManager.extract(source)
            if (extractedStreams.isNotEmpty()) {
                StreamLogger.debug(
                    TAG,
                    "Extracted ${extractedStreams.size} stream(s) from ${source.hostType}"
                )
            } else {
                StreamLogger.warn(TAG, "No streams extracted for ${source.url}")
            }
            extractedStreams
        } catch (e: Exception) {
            Logger.e("Error resolving source ${source.url}: ${e.message}", e, TAG)
            emptyList()
        }
    }

    /**
     * Stage 11: Header Injection & Validation.
     *
     * Ensures that any required Referer or Origin headers defined in [ExtractorRegistry]
     * for this host type are present on the stream link so the CDN player does not 403.
     */
    private fun injectRegistryHeaders(stream: StreamLink): StreamLink {
        val requiredHeaders = ExtractorRegistry.getHeaders(stream.host.name)
        if (requiredHeaders.isEmpty() && !ExtractorRegistry.requiresReferer(stream.host.name)) {
            return stream
        }

        val mergedHeaders = stream.headers.toMutableMap()
        requiredHeaders.forEach { (key, valStr) ->
            if (!mergedHeaders.containsKey(key)) {
                mergedHeaders[key] = valStr
            }
        }

        // Ensure Referer is set if required by registry
        val referer = stream.referer ?: requiredHeaders["Referer"]

        return stream.copy(
            headers = mergedHeaders,
            referer = referer
        )
    }
}
