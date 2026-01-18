package com.streamflex.app.data.models.content

data class ProviderSource(
    val providerId: String,      // e.g. "hdhub4u", "hdo", "streamsb"
    val sourceType: SourceType,  // DIRECT_URL, HLS, DASH, EMBED, MAGNET, etc.
    val quality: String? = null, // "1080p", "720p", "CAM", null...
    val url: String,             // playable media file OR embed link
    val headers: Map<String, String> = emptyMap() // referer, user-agent...
)

enum class SourceType {
    DIRECT_URL,
    HLS,
    DASH,
    EMBED,
    TORRENT,
    OTHER
}
