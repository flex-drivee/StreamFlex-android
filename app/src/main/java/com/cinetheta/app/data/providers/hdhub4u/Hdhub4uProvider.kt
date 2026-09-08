package com.cinetheta.app.data.providers.hdhub4u
import com.cinetheta.app.data.extractors.Hdhub4uExtractor
import com.cinetheta.app.domain.models.VideoStream
import com.cinetheta.app.domain.models.SearchResult


class Hdhub4uProvider {

    private val parser = Hdhub4uParser()
    private val extractor = Hdhub4uExtractor()

    suspend fun search(query: String): List<SearchResult> {
        return parser.search(query)
    }

    suspend fun load(detailUrl: String): List<VideoStream> {
        val links = extractor.extract(detailUrl)

        return links.map {
            VideoStream(
                url = it,
                quality = "Auto",

            )
        }
    }
}
