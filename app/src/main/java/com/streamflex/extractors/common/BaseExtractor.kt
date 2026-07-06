package com.streamflex.extractors.common

import com.streamflex.core.network.detector.ContentType
import com.streamflex.core.network.detector.ContentTypeDetector
import com.streamflex.domain.models.HostType
import com.streamflex.domain.models.ProviderSource
import com.streamflex.domain.models.Quality
import com.streamflex.core.network.detector.QualityDetector
import com.streamflex.domain.models.StreamLink

/**
 * Base class for all StreamFlex extractors.
 *
 * A Provider returns ProviderSource objects.
 * An Extractor converts those ProviderSources into playable StreamLinks.
 *
 * Networking, HTML parsing and JSON parsing are handled by shared
 * utilities (HttpClient, HtmlParser, JsonParser, ExtractorHelper).
 */
abstract class BaseExtractor {

    /**
     * Host supported by this extractor.
     */
    abstract val hostType: HostType

    /**
     * Resolve a ProviderSource into one or more playable streams.
     */
    abstract suspend fun extract(
        source: ProviderSource
    ): List<StreamLink>

    /**
     * Creates a StreamLink using the ProviderSource defaults.
     */
    /**
     * Creates a StreamLink using automatic detection wherever possible.
     */
    protected fun createStream(
        source: ProviderSource,
        url: String,
        quality: Quality? = null,
        subtitles: List<com.streamflex.domain.models.Subtitle> = emptyList(),
        audioTracks: List<com.streamflex.domain.models.AudioTrack> = emptyList(),
        fileSize: Long? = null,
        requiresAuth: Boolean = false
    ): StreamLink {

        val detectedQuality = quality
            ?: if (source.quality != Quality.UNKNOWN) {
                source.quality
            } else {
                QualityDetector.detect(url)
            }

        val contentType =
            ContentTypeDetector.detect(url)

        return StreamLink(

            name = buildName(
                source,
                detectedQuality
            ),

            url = url,

            quality = detectedQuality,

            host = source.hostType,

            contentType = contentType,

            headers = source.headers,

            cookies = source.cookies,

            subtitles = subtitles,

            audioTracks = audioTracks,

            fileSize = fileSize,

            adaptive = ContentTypeDetector.isAdaptive(contentType),

            requiresAuth = requiresAuth,

            referer = source.referer
        )
    }

    /**
     * Generates a readable stream name.
     */
    private fun buildName(
        source: ProviderSource,
        quality: Quality
    ): String {

        return buildString {

            append(source.provider)

            if (quality != Quality.UNKNOWN) {
                append(" • ")
                append(quality.label)
            }

            append(" • ")
            append(source.host)

        }
    }


    /**
     * Checks whether this extractor can handle the given source.
     */
    fun supports(
        source: ProviderSource
    ): Boolean {

        return source.hostType == hostType
    }
}