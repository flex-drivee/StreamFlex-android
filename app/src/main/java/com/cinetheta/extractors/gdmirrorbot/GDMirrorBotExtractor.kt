package com.cinetheta.extractors.gdmirrorbot

import com.cinetheta.core.logger.Logger
import com.cinetheta.domain.models.ExtractionResult
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.extractors.common.BaseExtractor

class GDMirrorBotExtractor : BaseExtractor() {
    override val hostType = HostType.GDMIRRORBOT

    override suspend fun extract(source: ProviderSource): ExtractionResult {
        Logger.w("[GDMirrorBot] Extracting requires API reverse engineering. Returning empty for now.")
        return emptyResult()
    }
}
