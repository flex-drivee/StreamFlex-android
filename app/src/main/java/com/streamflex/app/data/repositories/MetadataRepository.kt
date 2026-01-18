package com.streamflex.app.data.repositories

import com.streamflex.app.data.models.*
import com.streamflex.app.data.network.ApiRoutes
import com.streamflex.app.data.network.HttpClient
import com.streamflex.app.utils.parsed

object MetadataRepository {

    fun getMovie(id: String): CinemetaResponse? {
        val json = HttpClient.get(ApiRoutes.movieMeta(id))
        return json.parsed()
    }

    fun getSeries(id: String): CinemetaResponse? {
        val json = HttpClient.get(ApiRoutes.seriesMeta(id))
        return json.parsed()
    }

    fun getSeasons(id: String): List<Meta>? {
        val json = HttpClient.get(ApiRoutes.seasons(id))
        return json.parsed()
    }

    fun getEpisodes(id: String, season: Int): List<MetaEpisode>? {
        val json = HttpClient.get(ApiRoutes.episodes(id, season))
        return json.parsed()
    }
}
