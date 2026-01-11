package com.streamflex.app.core

import com.streamflex.app.data.models.Media
import com.streamflex.app.data.models.MediaDetails
import com.streamflex.app.data.models.StreamLink

abstract class BaseProvider {
    abstract val name: String
    abstract val mainUrl: String

    abstract suspend fun search(query: String): List<Media>
    abstract suspend fun loadHome(): List<Media>
    abstract suspend fun loadDetails(url: String): MediaDetails
    abstract suspend fun loadLinks(episodeUrl: String): List<StreamLink>
}