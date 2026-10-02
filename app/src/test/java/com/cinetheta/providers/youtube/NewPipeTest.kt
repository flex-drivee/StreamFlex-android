package com.cinetheta.providers.youtube

import org.junit.Test
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.ServiceList

class NewPipeTest {
    @Test
    fun testExtraction() {
        NewPipeDownloader.init()
        val watchUrl = "https://www.youtube.com/watch?v=aqz-KE-bpKQ"
        val info = StreamInfo.getInfo(ServiceList.YouTube, watchUrl)
        println("Streams found:")
        info.videoStreams.forEach {
            println("Video: ${it.quality} - ${it.content}")
        }
    }
}
