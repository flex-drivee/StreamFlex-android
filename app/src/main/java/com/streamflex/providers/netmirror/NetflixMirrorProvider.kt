package com.streamflex.providers.netmirror

class NetflixMirrorProvider : BaseNetMirrorProvider() {
    override val id = NetMirrorConfig.PROVIDER_ID_NETFLIX
    override val name = "Netflix Mirror"
    override val ottType = NetMirrorConfig.OTT_NETFLIX
}
