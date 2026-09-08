package com.cinetheta.extractors.streamup

import com.cinetheta.core.logger.Logger
import com.cinetheta.domain.models.ExtractionResult
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.extractors.common.BaseExtractor

class StreamUpExtractor : BaseExtractor() {
    override val hostType = HostType.STREAMUP

    override suspend fun extract(source: ProviderSource): ExtractionResult {
        Logger.w("[StreamUp] Extracting requires bypass. Returning empty for now.")
        return emptyResult()
    }
}
