package com.cinetheta.extractors.cloudy

import com.cinetheta.core.logger.Logger
import com.cinetheta.domain.models.ExtractionResult
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.extractors.common.BaseExtractor

class CloudyExtractor : BaseExtractor() {
    override val hostType = HostType.CLOUDY

    override suspend fun extract(source: ProviderSource): ExtractionResult {
        Logger.w("[Cloudy] Extracting requires specific API logic. Returning empty for now.")
        return emptyResult()
    }
}
