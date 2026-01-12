package com.streamflex.app.utils

import com.google.gson.Gson

inline fun <reified T> String?.parsed(): T? {
    return try {
        if (this == null) return null
        Gson().fromJson(this, T::class.java)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
