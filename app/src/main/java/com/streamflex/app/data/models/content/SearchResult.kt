package com.streamflex.app.data.models.content

sealed class SearchResult {
    data class MovieResult(val movie: Movie) : SearchResult()
    data class ShowResult(val show: Show) : SearchResult()
}
