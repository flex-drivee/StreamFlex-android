import org.schabi.newpipe.extractor.stream.StreamInfo

fun main() {
    val date = StreamInfo.getInfo("").uploadDate
    println(date)
}
