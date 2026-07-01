package com.streamflex.core.network.detector

import com.streamflex.domain.models.M3U8Info

object M3U8Detector {

    fun detect(
        url: String,
        content: String
    ): M3U8Info {

        val text = content.trim()

        if (!text.startsWith("#EXTM3U")) {
            return M3U8Info(
                isM3U8 = false,
                isMasterPlaylist = false,
                isMediaPlaylist = false,
                isLiveStream = false,
                variants = emptyList(),
                qualities = emptyList()
            )
        }

        val variants = mutableListOf<String>()
        val qualities = mutableListOf<String>()

        val isMaster = text.contains("#EXT-X-STREAM-INF")

        val isMedia =
            text.contains("#EXTINF")

        val isLive =
            !text.contains("#EXT-X-ENDLIST")

        val lines = text.lines()

        for (i in lines.indices) {

            val line = lines[i]

            if (line.startsWith("#EXT-X-STREAM-INF")) {

                Regex("RESOLUTION=\\d+x(\\d+)")
                    .find(line)
                    ?.groupValues
                    ?.get(1)
                    ?.let {
                        qualities.add("${it}p")
                    }

                if (i + 1 < lines.size) {

                    variants.add(
                        lines[i + 1].trim()
                    )
                }
            }
        }

        return M3U8Info(
            isM3U8 = true,
            isMasterPlaylist = isMaster,
            isMediaPlaylist = isMedia,
            isLiveStream = isLive,
            variants = variants,
            qualities = qualities
        )
    }

}