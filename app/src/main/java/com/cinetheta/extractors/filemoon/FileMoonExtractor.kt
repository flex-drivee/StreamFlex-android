package com.cinetheta.extractors.filemoon

import com.cinetheta.domain.models.ExtractionResult
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.extractors.common.BaseExtractor
import com.cinetheta.extractors.hdstream4u.HdStream4uExtractor

/**
 * FileMoon video extractor.
 *
 * FileMoon uses packed JavaScript (eval(function(p,a,c,k,e,d)...)) containing
 * direct HLS (.m3u8) streams. Handled by unpacking scripts via [HdStream4uExtractor].
 */
class FileMoonExtractor : BaseExtractor() {
    override val hostType = HostType.FILEMOON
    private val delegate = HdStream4uExtractor()

    override suspend fun extract(source: ProviderSource): ExtractionResult {
        return delegate.extract(source)
    }
}
