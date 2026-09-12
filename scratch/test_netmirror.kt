import java.net.URL

fun main() {
    val q = "Weak+Hero"
    listOf("hs", "dp", "nf", "pv").forEach { ott ->
        val url = if (ott == "nf") {
            "https://net52.cc/mobile/search.php?s=$q"
        } else {
            "https://net52.cc/mobile/$ott/search.php?s=$q"
        }
        try {
            val res = URL(url).readText()
            println("$ott -> $res")
        } catch (e: Exception) {
            println("$ott error: $e")
        }
    }
}
