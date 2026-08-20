package com.streamflex.extractors.cloudy

import com.streamflex.core.logger.Logger
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.extractors.common.BaseExtractor

class CloudyExtractor : BaseExtractor() {
    override val hostType = HostType.CLOUDY

    override suspend fun extract(source: ProviderSource): ExtractionResult {
        Logger.w("[Cloudy] Extracting requires specific API logic. Returning empty for now.")
        return emptyResult()
    }
}
