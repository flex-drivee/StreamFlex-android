package com.streamflex.extractors

import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.StreamLink
import com.streamflex.extractors.common.BaseExtractor
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import com.streamflex.extractors.googlevideo.GoogleVideoExtractor
import com.streamflex.extractors.hblinks.HBLinksExtractor
import com.streamflex.extractors.hubcdn.HubCDNExtractor
import com.streamflex.extractors.hubcloud.HubCloudExtractor
import com.streamflex.extractors.hubdrive.HubDriveExtractor
import com.streamflex.extractors.redirect.RedirectExtractor
import com.streamflex.extractors.dood.DoodExtractor
import com.streamflex.extractors.pixeldrain.PixelDrainExtractor
import com.streamflex.extractors.netmirror.NetMirrorExtractor
import com.streamflex.extractors.moviebox.MovieBoxExtractor
import com.streamflex.core.utils.StreamLogger
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

        com.streamflex.extractors.hdstream4u.HdStream4uExtractor(),

        // Phase 3 — NetMirror complete extractors
        NetMirrorExtractor(),
        
        MovieBoxExtractor(),
        
        // Phase 3+ providers
        com.streamflex.extractors.streamtape.StreamTapeExtractor(),
        com.streamflex.extractors.mixdrop.MixDropExtractor(),
        
        // AnimeDekho extractors
        com.streamflex.extractors.animedekho.AnimeDekhoExtractor(),
        com.streamflex.extractors.abyss.AbyssPlayerExtractor(),
        com.streamflex.extractors.turbovid.TurboVidExtractor(),
        com.streamflex.extractors.vidmoly.VidmolyExtractor(),
        com.streamflex.extractors.streamruby.StreamRubyExtractor(),
        com.streamflex.extractors.gdmirrorbot.GDMirrorBotExtractor(),
        com.streamflex.extractors.cloudy.CloudyExtractor(),
        com.streamflex.extractors.streamup.StreamUpExtractor(),
        com.streamflex.extractors.xerver.XerverExtractor()
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

                        if (com.streamflex.core.network.detector.HostDetector.isDirect(current.hostType)) {
                            StreamLogger.debug("ExtractorManager", "Direct stream queued: ${current.url}")
                            val stream = com.streamflex.domain.models.StreamLink(
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