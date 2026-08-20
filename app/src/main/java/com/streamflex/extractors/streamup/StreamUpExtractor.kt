package com.streamflex.extractors.streamup

import com.streamflex.core.logger.Logger
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.extractors.common.BaseExtractor

class StreamUpExtractor : BaseExtractor() {
    override val hostType = HostType.STREAMUP

    override suspend fun extract(source: ProviderSource): ExtractionResult {
        Logger.w("[StreamUp] Extracting requires bypass. Returning empty for now.")
        return emptyResult()
    }
}
