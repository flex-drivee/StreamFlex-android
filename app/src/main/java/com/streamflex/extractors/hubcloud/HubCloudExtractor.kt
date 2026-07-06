package com.streamflex.extractors.hubcloud

import com.streamflex.core.parser.HtmlParser
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.StreamLink
import com.streamflex.extractors.common.BaseExtractor
import com.streamflex.extractors.shared.ExtractorHelper

/**
 * Extractor for HubCloud.
 *
 * Responsible only for resolving HubCloud pages into playable streams.
 */
class HubCloudExtractor(

    private val helper: ExtractorHelper = ExtractorHelper()

) : BaseExtractor() {

    override val hostType = HostType.HUBCLOUD

    override suspend fun extract(
        source: ProviderSource
    ): List<StreamLink> {

        if (!supports(source)) {
            return emptyList()
        }

        val html = helper.getHtml(
            source.url,
            source.headers
        )

        if (html.isBlank()) {
            return emptyList()
        }

        val document = HtmlParser.parse(
            html,
            source.url
        )

        return parseHubCloud(
            source,
            document
        )
    }

    /**
     * Parse HubCloud page.
     */
    private suspend fun parseHubCloud(
        source: ProviderSource,
        document: org.jsoup.nodes.Document
    ): List<StreamLink> {

        val streams = mutableListOf<StreamLink>()

        // Part 2

        return streams
    }

}