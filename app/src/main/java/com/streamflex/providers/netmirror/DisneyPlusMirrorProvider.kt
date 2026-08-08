package com.streamflex.providers.netmirror

class DisneyPlusMirrorProvider : BaseNetMirrorProvider() {
    override val id = NetMirrorConfig.PROVIDER_ID_DISNEY
    override val name = "Disney Plus Mirror"
    override val ottType = NetMirrorConfig.OTT_DISNEY
}
