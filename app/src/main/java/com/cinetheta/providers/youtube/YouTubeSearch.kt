package com.cinetheta.providers.youtube

import com.cinetheta.domain.models.MediaType
import com.cinetheta.domain.models.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class YouTubeSearch {

    suspend fun search(query: String, page: Int = 1): List<SearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                try {
                    NewPipeDownloader.init()
                } catch (e: Exception) {
                    // Already initialized
                }

                val extractor = ServiceList.YouTube.getSearchExtractor(query)
                extractor.fetchPage()
                val items = extractor.initialPage.items
                
                val results = mutableListOf<SearchResult>()
                
                for (item in items) {
                    // We only want videos
                    if (item is StreamInfoItem) {
                        val videoId = item.url.substringAfter("v=", "").substringBefore("&")
                        if (videoId.isEmpty()) continue
                        
                        val title = item.name ?: ""
                        val author = item.uploaderName ?: ""
                        val duration = item.duration
                        val views = item.viewCount
                        
                        results.add(
                            SearchResult(
                                id           = videoId,
                                url          = item.url,
                                providerId   = YouTubeConfig.PROVIDER_ID,
                                providerName = YouTubeConfig.PROVIDER_NAME,
                                title        = title,
                                mediaType    = MediaType.UNKNOWN,
                                poster       = item.thumbnails?.lastOrNull()?.url,
                                overview     = "By $author  •  ${duration / 60}m ${duration % 60}s" +
                                               (if (views > 0) "  •  $views views" else "")
                            )
                        )
                    }
                }
                results
            } catch (e: Exception) {
                e.let { com.cinetheta.core.logger.Logger.e("Exception", it) }
                emptyList()
            }
        }
    }
}
