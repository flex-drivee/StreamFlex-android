package com.streamflex.providers.hdhub4u

import org.junit.Test
import org.jsoup.Jsoup
import java.io.File
import com.streamflex.core.network.detector.HostDetector
import com.streamflex.domain.models.HostType

class ReacherTest {

    val EPISODE_NUM_REGEX = Regex("""(?:ep(?:isode)?[.\s_-]?|[eE]\s*0*)(\d{1,3})|(?:\b[sS]\d{1,2}[eE]0*(\d{1,3}))""", RegexOption.IGNORE_CASE)

    private fun extractEpNumber(urlOrText: String): Int? {
        val match = EPISODE_NUM_REGEX.find(urlOrText) ?: return null
        return match.groupValues[1].takeIf { it.isNotBlank() }?.toIntOrNull()
            ?: match.groupValues[2].takeIf { it.isNotBlank() }?.toIntOrNull()
    }

    @Test
    fun testFindWatchLinks() {
        val html = File("/root/.gemini/antigravity-cli/brain/18aa5d38-9e18-46b2-938f-709fdf8bec38/scratch/reacher.html").readText()
        val doc = Jsoup.parse(html)
        val allAnchors = doc.select("a[href]")
        for (a in allAnchors) {
            val href = a.attr("href")
            if (!href.contains("greenmountmotors")) continue
            val text = a.text().trim()
            if (text != "Watch") continue
            
            // Check hops
            var prev = a.previousSibling()
            var hops = 0
            var foundEp: Int? = null
            while (prev != null && hops < 8) {
                val t = when (prev) {
                    is org.jsoup.nodes.TextNode -> prev.text()
                    is org.jsoup.nodes.Element -> prev.text()
                    else -> ""
                }.trim()

                if (t.isNotBlank()) {
                    val ep = extractEpNumber(t)
                    if (ep != null) {
                        foundEp = ep
                        break
                    }
                }
                prev = prev.previousSibling()
                hops++
            }
            
            println("Watch Link -> href: ${href.take(50)}..., foundEp: $foundEp")
        }
    }
}
