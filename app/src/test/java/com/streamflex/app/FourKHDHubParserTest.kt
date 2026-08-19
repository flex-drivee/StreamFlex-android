package com.streamflex.app

import org.jsoup.Jsoup
import org.junit.Test
import java.io.File

class FourKHDHubParserTest {

    @Test
    fun testParseMovieHtml() {
        val file = File("movie.html")
        if (!file.exists()) {
            println("movie.html not found!")
            return
        }

        val doc = Jsoup.parse(file, "UTF-8", "https://4khdhub.one")
        
        // Find all accordion buttons that have the quality title
        val accordions = doc.select("button.accordion-button")
        println("Found \${accordions.size} accordions.")

        for (accordion in accordions) {
            val titleElement = accordion.selectFirst(".flex-1")
            if (titleElement != null) {
                val fullTitle = titleElement.text()
                // the title usually contains the quality e.g., "Spider-Man: Far From Home... (2160p 4K BluRay...)"
                println("Accordion Title: \$fullTitle")

                // Extract badges from the title element
                val badges = titleElement.select(".badge").map { it.text() }
                println("  Badges: \$badges")
                
                // Determine quality string
                var quality = "Unknown"
                if (fullTitle.contains("2160p", ignoreCase = true) || fullTitle.contains("4K", ignoreCase = true)) quality = "4K"
                else if (fullTitle.contains("1080p", ignoreCase = true)) quality = "1080p"
                else if (fullTitle.contains("720p", ignoreCase = true)) quality = "720p"
                else if (fullTitle.contains("480p", ignoreCase = true)) quality = "480p"
                
                val isHevc = fullTitle.contains("HEVC", ignoreCase = true) || fullTitle.contains("x265", ignoreCase = true)
                if (isHevc) quality += " HEVC"
                
                println("  Extracted Quality: \$quality")
                
                // Now find the associated content div (it's the next sibling or we can find it by ID if referenced)
                val targetId = accordion.attr("aria-controls")
                val contentDiv = doc.getElementById(targetId)
                if (contentDiv != null) {
                    val links = contentDiv.select("a[href]")
                    for (link in links) {
                        val url = link.absUrl("href")
                        if (url.contains("hubcloud", ignoreCase = true) || url.contains("hdstream4u", ignoreCase = true) || url.contains("drive", ignoreCase = true)) {
                            println("    Found Link: \$url")
                        }
                    }
                } else {
                    println("  -> No content div found for ID: \$targetId")
                }
            }
        }
    }
}
