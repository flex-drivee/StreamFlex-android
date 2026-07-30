package com.streamflex.engine.stream

import com.streamflex.core.utils.StreamLogger
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.StreamLink
import com.streamflex.engine.resolver.ResolverEngine

/**
 * Collects playable streams from ProviderSources.
 *
 * Delegates to [ResolverEngine] (Phase 1.5) to execute the complete
 * 12-stage resolution chain (Direct Fast-Path, Redirects, Iframes,
 * Extractor Dispatch, and Header Injection).
 */
object StreamCollector {

    /**
     * Resolve a single ProviderSource.
     */
    suspend fun collect(
        source: ProviderSource
    ): List<StreamLink> {

        StreamLogger.debug(
            "StreamCollector",
            "Resolving source: ${source.provider} | ${source.hostType}"
        )

        val streams = ResolverEngine.resolve(source)

        StreamLogger.debug(
            "StreamCollector",
            "ResolverEngine returned ${streams.size} stream(s)"
        )

        return streams
    }

    /**
     * Resolve multiple ProviderSources concurrently.
     */
    suspend fun collect(
        sources: List<ProviderSource>
    ): List<StreamLink> {

        StreamLogger.info(
            "StreamCollector",
            "Resolving ${sources.size} provider source(s) via ResolverEngine"
        )

        val streams = ResolverEngine.resolveAll(sources)

        StreamLogger.info(
            "StreamCollector",
            "Collected ${streams.size} total stream(s) across all sources"
        )

        return streams
    }
}