package com.streamflex.extractors.gdmirrorbot

import com.streamflex.core.logger.Logger
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.extractors.common.BaseExtractor

class GDMirrorBotExtractor : BaseExtractor() {
    override val hostType = HostType.GDMIRRORBOT

    override suspend fun extract(source: ProviderSource): ExtractionResult {
        Logger.w("[GDMirrorBot] Extracting requires API reverse engineering. Returning empty for now.")
        return emptyResult()
    }
}
