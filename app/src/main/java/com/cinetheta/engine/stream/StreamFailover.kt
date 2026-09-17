package com.cinetheta.engine.stream

import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.StreamLink

/**
 * Builds a smart failover chain.
 *
 * The first stream is the primary stream.
 * The remaining streams are ordered as fallback options.
 */
object StreamFailover {

    /**
     * Build a failover chain.
     */
    fun build(
        streams: List<StreamLink>
    ): List<StreamLink> {

        if (streams.isEmpty()) {
            return emptyList()
        }

        // Deduplicate URLs while preserving the strict prioritized order from StreamSorter:
        // 1st: StreamRuby (if available)
        // 2nd: Abyss (1080p -> 720p -> 360p)
        // 3rd: Others (AWSStream, GDMirrorBot, etc.)
        val usedUrls = mutableSetOf<String>()
        val result = mutableListOf<StreamLink>()

        for (stream in streams) {
            if (usedUrls.add(stream.url)) {
                result += stream
            }
        }

        return result
    }

    /**
     * Primary stream.
     */
    fun primary(
        streams: List<StreamLink>
    ): StreamLink? {

        return build(streams).firstOrNull()
    }

    /**
     * Fallback stream.
     */
    fun fallback(
        streams: List<StreamLink>
    ): StreamLink? {

        return build(streams).drop(1).firstOrNull()
    }

    /**
     * Host priority for stream ordering.
     *
     * Lower value = higher priority (shown first / tried first).
     *
     * HDHub / 4KHDHub order:
     *   FSL  (HubCloud / workers.dev)  → priority 0
     *   FSL2 (HubDrive / HubCdn / HbLinks) → priority 1–3
     *   PixelDrain                      → priority 4
     *   Direct media (.mp4/.mkv/.m3u8)  → priority 5–7
     *   Other streaming hosts           → priority 10–15
     *   Redirect / BuzzerLinks/download → priority 90 (last)
     *
     * Anime order (AnimeDekho / ToonStream):
     *   StreamRuby                      → priority 0
     *   Abyss / PlayHydrax              → priority 1
     *   GDMirrorBot                     → priority 2
     *   Cloudy / TurboVid               → priority 3–4
     *   StreamUp / Xerver               → priority 5–6
     *   Vidmoly                         → priority 80 (always last — drops mid-play)
     */
    private fun hostPriority(
        stream: StreamLink
    ): Int {
        val url = stream.url.lowercase()

        return when {
            // ── Anime: StreamRuby first (0), AWSStream second (1), Abyss third (2) ─
            stream.host == HostType.STREAMRUBY -> 0
            stream.host == HostType.AWS_STREAM -> 1
            stream.host == HostType.ABYSS -> 2
            stream.host == HostType.GDMIRRORBOT -> 3
            stream.host == HostType.CLOUDY -> 4
            stream.host == HostType.TURBOVID -> 5
            stream.host == HostType.STREAMUP -> 6
            stream.host == HostType.XERVER -> 7
            stream.host == HostType.BLAKITE -> 8

            // ── HDHub / 4KHDHub: Google first ────────────────────────────────
            url.contains("googleusercontent.com") || stream.host == HostType.GOOGLE_VIDEO -> 7

            // ── FSL (HubCloud / workers.dev) ─────────────────────────────────
            url.contains("workers.dev") || stream.host == HostType.HUBCLOUD -> 8

            // ── FSL2 (HubDrive → HubCdn → HbLinks) ──────────────────────────
            stream.host == HostType.HUBDRIVE -> 9
            stream.host == HostType.HUBCDN -> 10
            stream.host == HostType.HBLINKS -> 11

            // ── PixelDrain ───────────────────────────────────────────────────
            stream.host == HostType.PIXELDRAIN || url.contains("pixeldrain") -> 12

            // ── Direct media ─────────────────────────────────────────────────
            url.endsWith(".m3u8") -> 13
            url.endsWith(".mpd") -> 14
            url.endsWith(".mp4") || url.endsWith(".mkv") -> 15

            // ── Other extractors (Streamtape, FileMoon, Mixdrop, Dood) ───────
            stream.host == HostType.STREAMTAPE -> 20
            stream.host == HostType.FILEMOON -> 21
            stream.host == HostType.MIXDROP -> 22
            stream.host == HostType.DOOD -> 23
            stream.host == HostType.HDSTREAM4U -> 24
            stream.host == HostType.VIDSTACK -> 25

            // ── Vidmoly: always last for anime (drops mid-play) ──────────────
            stream.host == HostType.VIDMOLY -> 80

            // ── BuzzerLinks / download redirects: absolute last ───────────────
            stream.host == HostType.REDIRECT -> 90

            else -> 50
        }
    }
}