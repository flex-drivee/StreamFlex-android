package com.cinetheta.app.data.extractors

import com.cinetheta.app.domain.models.StreamOption

interface Extractor {
    suspend fun extract(url: String): List<StreamOption>
}
