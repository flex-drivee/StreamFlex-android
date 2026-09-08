package com.cinetheta.extractors

import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.domain.models.StreamLink
import com.cinetheta.extractors.common.BaseExtractor
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import com.cinetheta.extractors.googlevideo.GoogleVideoExtractor
import com.cinetheta.extractors.hblinks.HBLinksExtractor
import com.cinetheta.extractors.hubcdn.HubCDNExtractor
import com.cinetheta.extractors.hubcloud.HubCloudExtractor
import com.cinetheta.extractors.hubdrive.HubDriveExtractor
import com.cinetheta.extractors.redirect.RedirectExtractor
import com.cinetheta.extractors.dood.DoodExtractor
import com.cinetheta.extractors.pixeldrain.PixelDrainExtractor
import com.cinetheta.extractors.netmirror.NetMirrorExtractor
import com.cinetheta.extractors.moviebox.MovieBoxExtractor
import com.cinetheta.core.utils.StreamLogger
import java.util.ArrayDeque

/**
 * Central extraction engine.
 *
 * Providers return ProviderSources.
 * Extractors resolve those ProviderSources into:
 *
 * - StreamLinks
 * - More ProviderSources
 *
 * The manager continues processing until no more
 * ProviderSources remain.
 */
object ExtractorManager {

    /**
     * Prevent infinite redirect loops.
     */
    private const val MAX_SOURCES = 30

    /**
     * Registered extractors.
     */
    private val extractors: List<BaseExtractor> = listOf(

        HubCloudExtractor(),

        HubDriveExtractor(),

        HubCDNExtractor(),

        HBLinksExtractor(),

        RedirectExtractor(),

        GoogleVideoExtractor(),

        // Phase 3 — HDHub4U complete extractors
        DoodExtractor(),

        PixelDrainExtractor(),

        com.cinetheta.extractors.hdstream4u.HdStream4uExtractor(),
        com.cinetheta.extractors.hdstream4u.HubStreamExtractor(),

        // Phase 3 — NetMirror complete extractors
        NetMirrorExtractor(),
        
        MovieBoxExtractor(),
        
        // Phase 3+ providers
        com.cinetheta.extractors.streamtape.StreamTapeExtractor(),
        com.cinetheta.extractors.mixdrop.MixDropExtractor(),
        
        // AnimeDekho extractors
        com.cinetheta.extractors.animedekho.AnimeDekhoExtractor(),
        com.cinetheta.extractors.toonstream.ToonStreamExtractor(),
        com.cinetheta.extractors.abyss.AbyssPlayerExtractor(),
        com.cinetheta.extractors.turbovid.TurboVidExtractor(),
        com.cinetheta.extractors.vidmoly.VidmolyExtractor(),
        com.cinetheta.extractors.streamruby.StreamRubyExtractor(),
        com.cinetheta.extractors.gdmirrorbot.GDMirrorBotExtractor(),
        com.cinetheta.extractors.cloudy.CloudyExtractor(),
        com.cinetheta.extractors.streamup.StreamUpExtractor(),
        com.cinetheta.extractors.xerver.XerverExtractor()
    )

    /**
     * Fast lookup by HostType.
     */
    private val extractorMap =
        extractors.associateBy { it.hostType }

    /**
     * Resolve a ProviderSource into playable streams.
     */
    suspend fun extract(
        source: ProviderSource,
        onStreamFound: suspend (StreamLink) -> Unit = {}
    ): List<StreamLink> {

        StreamLogger.info(
            "ExtractorManager",
            "Starting extraction pipeline"
        )

        val queued = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
        val visited = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
        val streams = java.util.concurrent.CopyOnWriteArrayList<StreamLink>()
        val emittedUrls = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

        var currentBatch = listOf(source)
        queued.add(source.url)

        while (currentBatch.isNotEmpty() && visited.size < MAX_SOURCES) {
            kotlinx.coroutines.yield()
            
            val nextBatch = java.util.concurrent.ConcurrentLinkedQueue<ProviderSource>()

            kotlinx.coroutines.coroutineScope {
                val jobs = currentBatch.map { current ->
                    async(kotlinx.coroutines.Dispatchers.IO) {
                        StreamLogger.debug(
                            "ExtractorManager",
                            "Processing ${current.hostType} -> ${current.url}"
                        )

                        if (!visited.add(current.url)) {
                            StreamLogger.debug("ExtractorManager", "Already visited. Skipping.")
                            return@async
                        }

                        if (com.cinetheta.core.network.detector.HostDetector.isDirect(current.hostType)) {
                            StreamLogger.debug("ExtractorManager", "Direct stream queued: ${current.url}")
                            val stream = com.cinetheta.domain.models.StreamLink(
                                name = "${current.provider} \u2022 Direct",
                                url = current.url,
                                quality = current.quality,
                                host = current.hostType,
                                headers = current.headers,
                                cookies = current.cookies,
                                referer = current.referer
                            )
                            if (emittedUrls.add(stream.url)) {
                                onStreamFound(stream)
                            }
                            streams.add(stream)
                            return@async
                        }

                        val extractor = extractorMap[current.hostType]
                        if (extractor == null) {
                            StreamLogger.warn("ExtractorManager", "No extractor registered for ${current.hostType}")
                            return@async
                        }

                        StreamLogger.debug("ExtractorManager", "Using ${extractor.javaClass.simpleName}")

                        try {
                            val result = extractor.extract(current)

                            StreamLogger.debug(
                                "ExtractorManager",
                                "Streams: ${result.streams.size}, Next Sources: ${result.sources.size}"
                            )

                            result.streams.forEach { stream ->
                                if (emittedUrls.add(stream.url)) {
                                    onStreamFound(stream)
                                }
                            }
                            streams.addAll(result.streams)

                            result.sources.filter {
                                it.url.isNotBlank() && it.hostType != HostType.UNKNOWN
                            }.forEach {
                                nextBatch.add(it)
                            }

                        } catch (e: Exception) {
                            StreamLogger.error(
                                "ExtractorManager",
                                "Extractor ${extractor.javaClass.simpleName} failed",
                                e
                            )
                        }
                    }
                }
                jobs.awaitAll()
            }

            currentBatch = nextBatch.filter { it.url !in visited && queued.add(it.url) }.take(MAX_SOURCES - visited.size)
        }

        StreamLogger.info(
            "ExtractorManager",
            "Extraction finished. Total streams: ${streams.size}"
        )

        return streams.distinctBy { it.url }
    }
    /**
     * Returns true if an extractor exists.
     */
    fun supports(
        source: ProviderSource
    ): Boolean {

        return extractorMap.containsKey(
            source.hostType
        )
    }

    /**
     * Returns extractor for debugging/testing.
     */
    fun findExtractor(
        source: ProviderSource
    ): BaseExtractor? {

        return extractorMap[
            source.hostType
        ]
    }
}