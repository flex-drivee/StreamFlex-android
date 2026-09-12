package com.cinetheta.domain.models

data class HomePageList(
    val title: String,
    val items: List<SearchResult>
)

data class HomePageResponse(
    val sections: List<HomePageList>
)
