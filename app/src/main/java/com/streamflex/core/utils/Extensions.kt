package com.streamflex.core.utils

import java.security.MessageDigest

/**
 * Returns null if the string is blank.
 */
fun String?.nullIfBlank(): String? {
    return if (this.isNullOrBlank()) null else this
}

/**
 * Returns empty string if null.
 */
fun String?.orEmptyString(): String {
    return this ?: ""
}

/**
 * Safe lowercase.
 */
fun String.lowercaseSafe(): String {
    return lowercase()
}

/**
 * Safe uppercase.
 */
fun String.uppercaseSafe(): String {
    return uppercase()
}

/**
 * Returns true if string contains text (case-insensitive).
 */
fun String.containsIgnoreCase(other: String): Boolean {
    return contains(other, ignoreCase = true)
}

/**
 * MD5 hash.
 */
fun String.md5(): String {

    val digest = MessageDigest
        .getInstance("MD5")
        .digest(toByteArray())

    return digest.joinToString("") {
        "%02x".format(it)
    }
}

/**
 * SHA-256 hash.
 */
fun String.sha256(): String {

    val digest = MessageDigest
        .getInstance("SHA-256")
        .digest(toByteArray())

    return digest.joinToString("") {
        "%02x".format(it)
    }
}

/**
 * Returns file extension.
 */
fun String.fileExtension(): String? {

    val index = lastIndexOf('.')

    if (index == -1) return null

    return substring(index + 1)
}

/**
 * True if URL looks like HTTP/HTTPS.
 */
fun String.isHttpUrl(): Boolean {
    return startsWith("http://") || startsWith("https://")
}

/**
 * Removes duplicate items while preserving order.
 */
fun <T> Iterable<T>.distinctOrdered(): List<T> {
    return distinct()
}

/**
 * Returns null if collection is empty.
 */
fun <T> Collection<T>.nullIfEmpty(): Collection<T>? {
    return if (isEmpty()) null else this
}

/**
 * Converts milliseconds to seconds.
 */
fun Long.toSeconds(): Long = this / 1000L

/**
 * Converts seconds to milliseconds.
 */
fun Long.toMilliseconds(): Long = this * 1000L