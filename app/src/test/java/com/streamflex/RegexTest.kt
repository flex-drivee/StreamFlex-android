package com.streamflex

object RegexTest {
    @JvmStatic
    fun main(args: Array<String>) {
        val regex = Regex("(\\d+)x(\\d+)")
        val url = "/episode/hunter-x-hunter-hindi-dub-3x137/"
        val match = regex.find(url)
        println("Season: \${match?.groupValues?.getOrNull(1)}")
        println("Episode: \${match?.groupValues?.getOrNull(2)}")
    }
}
