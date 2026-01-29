package com.streamflex.app.data.providers.hdhub4u
import com.streamflex.app.domain.models.VideoStream
import com.streamflex.app.domain.models.SearchResult


class Hdhub4uProvider {

    /**
     * Search movies / shows on HDHub4u
     */
    fun search(query: String): List<SearchResult> {
        return Hdhub4uParser.search(query)
    }

    /**
     * Load detail page and return raw stream/download links
     */
    fun load(detailUrl: String): List<VideoStream> {
        return com.streamflex.app.data.extractors.Hdhub4uExtractor.extract(detailUrl)
    }


}
