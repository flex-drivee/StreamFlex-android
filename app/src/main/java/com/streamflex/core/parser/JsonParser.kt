package com.streamflex.core.parser

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParser

/**
 * Central JSON utility used across StreamFlex.
 *
 * Keeps providers and extractors independent from Gson APIs.
 */
object JsonParser {

    val gson: Gson = GsonBuilder()
        .serializeNulls()
        .setLenient()
        .create()

    /**
     * Convert JSON string into a Kotlin object.
     */
    inline fun <reified T> fromJson(json: String): T? {
        return runCatching {
            gson.fromJson(json, T::class.java)
        }.getOrNull()
    }

    /**
     * Convert any object into JSON.
     */
    fun toJson(any: Any): String {
        return gson.toJson(any)
    }

    /**
     * Parse into JsonElement.
     */
    fun parse(json: String): JsonElement? {
        return runCatching {
            JsonParser.parseString(json)
        }.getOrNull()
    }

    /**
     * Pretty-print JSON.
     */
    fun pretty(json: String): String? {
        return runCatching {
            GsonBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(JsonParser.parseString(json))
        }.getOrNull()
    }

    /**
     * Returns true if the string is valid JSON.
     */
    fun isValid(json: String): Boolean {
        return runCatching {
            JsonParser.parseString(json)
            true
        }.getOrElse { false }
    }

    /**
     * Read a String property safely.
     */
    fun string(
        element: JsonElement?,
        key: String
    ): String? {
        return runCatching {
            element
                ?.asJsonObject
                ?.get(key)
                ?.takeUnless { it.isJsonNull }
                ?.asString
        }.getOrNull()
    }

    /**
     * Read an Int property safely.
     */
    fun int(
        element: JsonElement?,
        key: String
    ): Int? {
        return runCatching {
            element
                ?.asJsonObject
                ?.get(key)
                ?.takeUnless { it.isJsonNull }
                ?.asInt
        }.getOrNull()
    }

    /**
     * Read a Boolean property safely.
     */
    fun boolean(
        element: JsonElement?,
        key: String
    ): Boolean? {
        return runCatching {
            element
                ?.asJsonObject
                ?.get(key)
                ?.takeUnless { it.isJsonNull }
                ?.asBoolean
        }.getOrNull()
    }

    /**
     * Read a Double property safely.
     */
    fun double(
        element: JsonElement?,
        key: String
    ): Double? {
        return runCatching {
            element
                ?.asJsonObject
                ?.get(key)
                ?.takeUnless { it.isJsonNull }
                ?.asDouble
        }.getOrNull()
    }

    /**
     * Read a nested object safely.
     */
    fun objectOf(
        element: JsonElement?,
        key: String
    ): JsonElement? {
        return runCatching {
            element
                ?.asJsonObject
                ?.get(key)
        }.getOrNull()
    }

    /**
     * Read an array safely.
     */
    fun array(
        element: JsonElement?,
        key: String
    ): List<JsonElement> {
        return runCatching {
            element
                ?.asJsonObject
                ?.getAsJsonArray(key)
                ?.toList()
                ?: emptyList()
        }.getOrElse { emptyList() }
    }
}