package com.cinetheta.providers.youtube

import com.cinetheta.domain.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.NewPipe

class YouTubeDetails {

    suspend fun load(searchResult: SearchResult): ProviderResult? {
        val videoId = searchResult.id
        val watchUrl = "${YouTubeConfig.YOUTUBE_URL}/watch?v=$videoId"

        return withContext(Dispatchers.IO) {
            try {
                // Ensure NewPipe is initialized (safe to call multiple times if we use a flag in Downloader, but NewPipe.init is generally safe if done once. We will just ensure it)
                try {
                    NewPipeDownloader.init()
                } catch (e: Exception) {
                    // Already initialized
                }

                val extractor = ServiceList.YouTube.getStreamExtractor(watchUrl)
                extractor.fetchPage()
                val info = StreamInfo.getInfo(extractor)

                val title = info.name ?: searchResult.title
                val description = info.description?.content ?: ""
                val sources = mutableListOf<ProviderSource>()

                // Prepare audio tracks for DASH videos
                val myAudioTracks = info.audioStreams.orEmpty().mapIndexed { index, audio ->
                    AudioTrack(
                        language = "Audio ${index + 1}",
                        label = audio.averageBitrate?.let { "${it / 1000}kbps" } ?: "Audio",
                        url = audio.content,
                        isDefault = index == 0
                    )
                }

                // 1. DASH Streams: High-res (1080p/4K) video-only streams with separate audio
                val videoOnlyStreams = info.videoOnlyStreams.orEmpty()
                for (video in videoOnlyStreams) {
                    val qualityLabel = video.resolution ?: video.quality ?: ""
                    sources.add(
                        ProviderSource(
                            provider = YouTubeConfig.PROVIDER_NAME,
                            host     = "YouTube ($qualityLabel)",
                            hostType = HostType.DIRECT,
                            url      = video.content,
                            isDirect = true,
                            quality  = Quality.fromLabel(qualityLabel),
                            audioTracks = myAudioTracks
                        )
                    )
                }

                // 2. MP4 Streams: Combined audio/video streams (Usually max 360p or 720p)
                val videoStreams = info.videoStreams.orEmpty()
                for (video in videoStreams) {
                    val qualityLabel = video.resolution ?: video.quality ?: ""
                    sources.add(
                        ProviderSource(
                            provider = YouTubeConfig.PROVIDER_NAME,
                            host     = "YouTube ($qualityLabel Combined)",
                            hostType = HostType.DIRECT,
                            url      = video.content,
                            isDirect = true,
                            quality  = Quality.fromLabel(qualityLabel)
                        )
                    )
                }

                // 3. HLS Adaptive Stream: Contains all resolutions auto-switched
                if (info.hlsUrl != null) {
                    sources.add(
                        ProviderSource(
                            provider = YouTubeConfig.PROVIDER_NAME,
                            host     = "YouTube (Auto HLS)",
                            hostType = HostType.M3U8,
                            url      = info.hlsUrl,
                            isDirect = true,
                            quality  = Quality.UNKNOWN
                        )
                    )
                }

                if (sources.isEmpty()) return@withContext null

                ProviderResult(
                    id        = videoId,
                    providerId = YouTubeConfig.PROVIDER_ID,
                    title     = title,
                    detailUrl = searchResult.url,
                    mediaType = MediaType.UNKNOWN,
                    sources   = sources,
                    poster    = searchResult.poster,
                    overview  = description
                )
            } catch (e: Exception) {
                e.let { com.cinetheta.core.logger.Logger.e("Exception", it) }
                null
            }
        }
    }
}
