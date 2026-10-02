import org.schabi.newpipe.extractor.ServiceList
import com.cinetheta.providers.youtube.NewPipeDownloader

fun main() {
    try {
        NewPipeDownloader.init()
    } catch(e: Exception) {}
    
    try {
        val list = ServiceList.YouTube.kioskList.list
        list.forEach { 
            println("KIOSK ID: ${it.id} - NAME: ${it.name}")
        }
    } catch(e: Exception) {
        println("ERROR: ${e.message}")
    }
}
