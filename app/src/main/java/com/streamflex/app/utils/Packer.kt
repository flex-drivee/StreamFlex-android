package com.streamflex.app.utils

import java.util.regex.Pattern

object Unpacker {
    fun unpack(packed: String): String? {
        try {
            // Find the P,A,C,K,E,D block
            val regex = "return\\s+p\\}\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*['\"]([^'\"]+)['\"]\\.split\\('\\|'\\)"
            val matcher = Pattern.compile(regex).matcher(packed)

            if (matcher.find()) {
                val payload = matcher.group(1) ?: return null
                val radix = matcher.group(2)?.toInt() ?: return null
                val count = matcher.group(3)?.toInt() ?: return null
                val keywords = matcher.group(4)?.split("|")?.toTypedArray() ?: return null

                // Logic to map the compressed tokens back to original words
                val unpacked = payload.replace(Regex("\\b\\w+\\b")) { result ->
                    val token = result.value
                    val index = Integer.parseInt(token, radix)
                    if (index < keywords.size && keywords[index].isNotEmpty()) {
                        keywords[index]
                    } else {
                        token
                    }
                }
                return unpacked
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}