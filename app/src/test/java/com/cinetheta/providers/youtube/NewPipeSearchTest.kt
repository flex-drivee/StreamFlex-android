package com.cinetheta.providers.youtube

import org.junit.Test
import org.schabi.newpipe.extractor.search.SearchEngine
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchQueryHandlerFactory

class NewPipeSearchTest {
    @Test
    fun testSearch() {
        NewPipeDownloader.init()
        val search = ServiceList.YouTube.searchEngine.search("test")
        println("Found: ${search.items.size}")
    }
}
