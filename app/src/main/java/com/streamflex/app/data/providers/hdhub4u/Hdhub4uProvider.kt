package com.streamflex.app.data.providers.hdhub4u
import com.streamflex.app.data.extractors.Hdhub4uExtractor
import com.streamflex.app.domain.models.VideoStream
import com.streamflex.app.domain.models.SearchResult


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
