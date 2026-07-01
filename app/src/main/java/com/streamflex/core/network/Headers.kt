package com.streamflex.core.network

import com.streamflex.core.constants.Constants

object Headers {

    fun defaultHeaders(): Map<String, String> {
        return mapOf(
            "User-Agent" to Constants.DEFAULT_USER_AGENT
        )
    }

    fun referer(url: String): Map<String, String> {
        return mapOf(
            "User-Agent" to Constants.DEFAULT_USER_AGENT,
            "Referer" to url
        )
    }

    fun custom(
        referer: String? = null,
        origin: String? = null
    ): MutableMap<String, String> {

        val headers = mutableMapOf(
            "User-Agent" to Constants.DEFAULT_USER_AGENT
        )

        referer?.let {
            headers["Referer"] = it
        }

        origin?.let {
            headers["Origin"] = it
        }

        return headers
    }
}