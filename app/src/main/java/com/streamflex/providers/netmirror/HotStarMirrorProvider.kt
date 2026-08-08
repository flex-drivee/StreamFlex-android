package com.streamflex.providers.netmirror

class HotStarMirrorProvider : BaseNetMirrorProvider() {
    override val id = NetMirrorConfig.PROVIDER_ID_HOTSTAR
    override val name = "HotStar Mirror"
    override val ottType = NetMirrorConfig.OTT_HOTSTAR
}
