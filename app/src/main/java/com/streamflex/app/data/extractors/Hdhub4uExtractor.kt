package com.streamflex.app.data.extractors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * This extractor returns video URLs that PlayerActivity can play.
 * For now, we return a test MP4 to validate playback.
 */
class Hdhub4uExtractor {

    suspend fun extractVideoLinks(pageUrl: String): List<String> =
        withContext(Dispatchers.IO) {

            // TEMP: test playback link
            listOf(
                "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            )
        }
}
