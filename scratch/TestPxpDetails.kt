import com.cinetheta.core.parser.HtmlParser
import java.io.File

fun main() {
    val html = File("/tmp/pxp_details.html").readText()
    val doc = HtmlParser.parse(html, "https://piratexplay.cc")
    val articles = doc.select("section.episodes article.episodes")
    println("articles count: \${articles.size}")
    
    val seasonMap = mutableMapOf<Int, Int>()
    
    for (article in articles) {
        val aElem = article.selectFirst("a.lnk-blk")
        var epUrl = aElem?.attr("abs:href").takeIf { !it.isNullOrBlank() } ?: aElem?.attr("href") ?: ""
        
        if (epUrl.isBlank()) continue
        
        val numStr = article.selectFirst(".num-epi")?.text()?.trim() ?: ""
        println("epUrl: \$epUrl, numStr: \$numStr")
        seasonMap[1] = seasonMap.getOrDefault(1, 0) + 1
    }
    
    println("Valid episodes count: \${seasonMap[1]}")
}
