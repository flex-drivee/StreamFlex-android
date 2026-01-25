package com.streamflex.app.data.extractors

import com.streamflex.app.domain.models.StreamOption

interface Extractor {
    suspend fun extract(url: String): List<StreamOption>
}
