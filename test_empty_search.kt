import kotlinx.coroutines.runBlocking
import com.cinetheta.providers.moviebox.*
import com.cinetheta.core.cache.CacheManager

fun main() = runBlocking {
    val p = MovieBoxProvider()
    val res = p.search("")
    println(res.take(5))
}
