import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.NewPipe

fun main() {
    NewPipe.init(org.schabi.newpipe.extractor.downloader.Downloader { request -> 
        null
    })
    val kiosk = ServiceList.YouTube.kioskList.defaultKiosk
    println("Kiosk ID: ${kiosk.id}, Name: ${kiosk.name}")
}
