package com.streamflex.providers.netmirror

class PrimeVideoMirrorProvider : BaseNetMirrorProvider() {
    override val id = NetMirrorConfig.PROVIDER_ID_PRIME
    override val name = "Prime Video Mirror"
    override val ottType = NetMirrorConfig.OTT_PRIME
}
