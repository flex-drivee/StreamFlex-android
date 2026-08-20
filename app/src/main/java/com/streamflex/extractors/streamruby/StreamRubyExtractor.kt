package com.streamflex.extractors.streamruby

import com.streamflex.core.logger.Logger
import com.streamflex.domain.models.ExtractionResult
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.extractors.common.BaseExtractor

class StreamRubyExtractor : BaseExtractor() {
    override val hostType = HostType.STREAMRUBY

    override suspend fun extract(source: ProviderSource): ExtractionResult {
        Logger.w("[StreamRuby] Extracting requires JS unpacker. Returning empty for now.")
        return emptyResult()
    }
}
