package com.streamflex.app.data.network

object ApiRoutes {

    private const val BASE = "https://v3-cinemeta.strem.io/meta"

    fun movieMeta(id: String): String = "$BASE/movie/${id}.json"
    fun seriesMeta(id: String): String = "$BASE/series/${id}.json"
    fun seasons(id: String): String = "$BASE/series/${id}/seasons.json"
    fun episodes(id: String, season: Int): String = "$BASE/series/${id}/season/$season/episodes.json"
}
