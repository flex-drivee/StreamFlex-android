import org.schabi.newpipe.extractor.ServiceList

fun main() {
    try {
        val list = ServiceList.YouTube.kioskList.list
        list.forEach { 
            println("ID: ${it.id} - NAME: ${it.name}")
        }
    } catch(e: Exception) {
        println("ERROR: ${e.message}")
    }
}
