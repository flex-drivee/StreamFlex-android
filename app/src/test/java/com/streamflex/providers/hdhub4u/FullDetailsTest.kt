package com.streamflex.providers.hdhub4u

import org.junit.Test
import org.jsoup.Jsoup
import java.io.File
import com.streamflex.core.network.detector.HostDetector

class FullDetailsTest {
    @Test
    fun testHDHubDetails() {
        val html = File("/root/.gemini/antigravity-cli/brain/18aa5d38-9e18-46b2-938f-709fdf8bec38/scratch/reacher.html").readText()
        val doc = Jsoup.parse(html)
        val details = HDHubDetails()
        
        val seasons = details.parseSeasons(doc, "https://new5.hdhub4u.cl/reacher")
        for (s in seasons) {
            println("Season ${s.number}")
            for (e in s.episodes) {
                println("  Episode ${e.number}: ${e.sources.size} sources")
                for (src in e.sources) {
                    println("    - [${src.hostType}] ${src.url}")
                }
            }
        }
    }
}
