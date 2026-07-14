package com.streamflex.extractors.hubcloud

import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.StreamLink
import com.streamflex.core.network.detector.HostDetector
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.extractors.shared.ExtractorUtils
import com.streamflex.extractors.shared.ExtractorHelper
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.core.utils.StreamLogger

/**
 * Extractor for HubCloud.
 *
 * Responsible only for resolving HubCloud pages into playable streams.
 */
class HubCloudExtractor
    : BaseExtractor() {

    override val hostType = HostType.HUBCLOUD

    override suspend fun extract(
        source: ProviderSource
    ): ExtractionResult {
        StreamLogger.info(
            "HubCloudExtractor",
            "Extracting HubCloud page"
        )

        StreamLogger.debug(
            "HubCloudExtractor",
            "URL: ${source.url}"
        )

        if (!supports(source)) {

            StreamLogger.warn(
                "HubCloudExtractor",
                "Unsupported source: ${source.hostType}"
            )

            return emptyResult()
        }

        val document = ExtractorHelper.fetchDocument(

            source.url,
            source.headers
        )
        StreamLogger.debug(
            "HubCloudExtractor",
            "Document downloaded"
        )
        return result(

            parseHubCloud(
                source,
                document
            )


        )
    }

    /**
     * Parse HubCloud page.
     */
    private suspend fun parseHubCloud(
        source: ProviderSource,
        document: org.jsoup.nodes.Document
    ): List<StreamLink> {

        val candidates = linkedSetOf<String>()
        StreamLogger.debug(
            "HubCloudExtractor",
            "Scanning page for candidate URLs..."
        )

        // ----------------------------------------------------
        // 1. Video tags
        // ----------------------------------------------------

        document.select("video source[src]")
            .map { it.absUrl("src") }
            .filter { it.isNotBlank() }
            .forEach(candidates::add)

        // ----------------------------------------------------
        // 2. Download buttons
        // ----------------------------------------------------

        document.select("a[href]")
            .map { it.absUrl("href") }
            .filter { it.isNotBlank() }
            .forEach(candidates::add)

        // ----------------------------------------------------
        // 3. Iframes
        // ----------------------------------------------------

        document.select("iframe[src]")
            .map { it.absUrl("src") }
            .filter { it.isNotBlank() }
            .forEach(candidates::add)

        // ----------------------------------------------------
        // 4. JavaScript
        // ----------------------------------------------------

        document.select("script")
            .forEach {

                val html = it.data()

                ExtractorUtils
                    .allMatches(
                        """https?:\/\/[^\s"'<>\\]+""",
                        html
                    )
                    .forEach(candidates::add)
            }

        if (candidates.isEmpty()) {

            StreamLogger.warn(
                "HubCloudExtractor",
                "No candidate URLs found."
            )

            return emptyList()
        }
        return buildCandidateStreams(
            source,
            candidates.toList()
        )
    }
    private fun buildCandidateStreams(
        source: ProviderSource,
        urls: List<String>
    ): List<StreamLink> {

        val streams = mutableListOf<StreamLink>()
        val pendingSources = mutableListOf<ProviderSource>()

        val sorted = urls
            .distinct()
            .sortedWith(
                compareBy<String> {

                    when {

                        it.contains(".m3u8", true) -> 0

                        it.endsWith(".mp4", true) -> 1

                        it.endsWith(".mkv", true) -> 2

                        it.contains("googlevideo", true) ||
                                it.contains("googleusercontent", true) -> 3

                        else -> 100
                    }

                }
            )

        sorted.forEach { url ->

            val type = HostDetector.detect(url)

            when (type) {

                HostType.M3U8,
                HostType.DIRECT,
                HostType.GOOGLE_VIDEO -> {

                    streams += createStream(
                        source = source,
                        url = url
                    )
                }

                else -> {

                    pendingSources += buildProviderSource(
                        source,
                        url
                    )

                }
            }
        }

        return streams.distinctBy(StreamLink::url)
        // Remaining ProviderSources will be forwarded
// by ExtractorManager after recursive extraction
    }

    private fun buildProviderSource(
        source: ProviderSource,
        url: String
    ): ProviderSource {

        return source.copy(
            url = url,
            hostType = HostDetector.detect(url)
        )
    }

}