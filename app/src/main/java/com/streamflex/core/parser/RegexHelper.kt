package com.streamflex.core.parser

object RegexHelper {

    fun find(text: String, pattern: String): String? {
        return Regex(pattern).find(text)?.value
    }

    fun findGroup(text: String, pattern: String, group: Int): String? {
        return Regex(pattern).find(text)?.groups?.get(group)?.value
    }

    fun findAll(text: String, pattern: String): List<String> {
        return Regex(pattern)
            .findAll(text)
            .map { it.value }
            .toList()
    }

    fun matches(text: String, pattern: String): Boolean {
        return Regex(pattern).containsMatchIn(text)
    }
}