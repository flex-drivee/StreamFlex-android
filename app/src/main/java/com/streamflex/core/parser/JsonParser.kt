package com.streamflex.core.parser

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject

object JsonParser {

    private val gson = Gson()

    fun parse(json: String): JsonElement {
        return gson.fromJson(json, JsonElement::class.java)
    }

    fun parseObject(json: String): JsonObject {
        return gson.fromJson(json, JsonObject::class.java)
    }

    fun toJson(any: Any): String {
        return gson.toJson(any)
    }

    fun <T> fromJson(json: String, clazz: Class<T>): T {
        return gson.fromJson(json, clazz)
    }
}