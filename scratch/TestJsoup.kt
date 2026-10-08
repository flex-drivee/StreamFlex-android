import org.jsoup.Jsoup
import java.io.File

fun main() {
    val file = File("/tmp/pxp_details.html")
    val doc = Jsoup.parse(file, "UTF-8")
    val articles = doc.select("section.episodes article.episodes")
    println("Articles found: \${articles.size}")
}
