package com.cinetheta.extractors.driveleech

import com.cinetheta.domain.models.HostType
import com.cinetheta.extractors.driveseed.DriveseedExtractor

class DriveleechExtractor : DriveseedExtractor() {
    override val hostType: HostType = HostType.DRIVELEECH
    override val name: String = "Driveleech"
}
