package com.cinetheta.app.core

import com.cinetheta.app.domain.models.Movie
import com.cinetheta.app.domain.models.SearchResult
import com.cinetheta.app.domain.models.Show
import com.cinetheta.app.domain.models.VideoStream

abstract class BaseProvider {
    abstract val name: String
    abstract val mainUrl: String

    // Returns a list of search results (Matches your 'search' in Hdhub4uProvider)
    abstract suspend fun search(query: String): List<SearchResult>

    // Returns details for a Movie
    abstract suspend fun loadMovie(url: String): Movie

    // Returns details for a Show (seasons/episodes)
    abstract suspend fun loadShow(url: String): Show

    // Extract playable links (Matches 'extractStreams' in Hdhub4uProvider)
    abstract suspend fun extractStreams(url: String): List<VideoStream>
}