package com.cinetheta.extractors.shared

import kotlin.math.pow

/**
 * P.A.C.K.E.R. JavaScript unpacker.
 * Decodes obfuscated scripts matching `eval(function(p,a,c,k,e,d)...)`.
 */
class JsUnpacker(private val packedJS: String?) {

    fun detect(): Boolean {
        val js = packedJS?.replace(" ", "") ?: return false
        return Regex("""eval\(function\(p,a,c,k,e,[rd]""").containsMatchIn(js)
    }

    fun unpack(): String? {
        val js = packedJS ?: return null
        return try {
            val match = Regex(
                """(?s)\}\s*\('(.*)',\s*(.*?),\s*(\d+),\s*'(.*?)'\.split\('\|'\)"""
            ).find(js) ?: Regex(
                """(?s)\}\s*\("(.*)",\s*(.*?),\s*(\d+),\s*"(.*?)"\.split\("\|"\)"""
            ).find(js) ?: Regex(
                """(?s)\}\s*\((['"])(.*?)\1,\s*(.*?),\s*(\d+),\s*(['"])(.*?)\5\.split\(\5\|\5\)"""
            ).find(js) ?: return null

            val payload = (if (match.groupValues.size >= 7) match.groupValues[2] else match.groupValues[1])
                .replace("\\'", "'").replace("\\\"", "\"")
            val radixStr = if (match.groupValues.size >= 7) match.groupValues[3] else match.groupValues[2]
            val countStr = if (match.groupValues.size >= 7) match.groupValues[4] else match.groupValues[3]
            val symtabRaw = if (match.groupValues.size >= 7) match.groupValues[6] else match.groupValues[4]
            val symtab = symtabRaw.split("|").toTypedArray()

            var radix = radixStr.toIntOrNull() ?: 36
            val count = countStr.toIntOrNull() ?: 0

            if (symtab.size != count && count > 0 && symtab.size < count) {
                // Return best effort if count mismatch
            }

            val unbase = Unbase(radix)
            val wordRegex = Regex("""\b[a-zA-Z0-9_]+\b""")
            val decoded = StringBuilder(payload)
            var replaceOffset = 0

            wordRegex.findAll(payload).forEach { wordMatch ->
                val word = wordMatch.value
                val x = unbase.unbase(word)
                val value = if (x in symtab.indices) symtab[x] else null
                if (!value.isNullOrEmpty()) {
                    decoded.setRange(
                        wordMatch.range.first + replaceOffset,
                        wordMatch.range.last + 1 + replaceOffset,
                        value
                    )
                    replaceOffset += value.length - word.length
                }
            }
            decoded.toString()
        } catch (_: Exception) {
            null
        }
    }

    private inner class Unbase(private val radix: Int) {
        private val ALPHABET_62 = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private val ALPHABET_95 =
            " !\"#$%&\\'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"
        private var alphabet: String? = null
        private var dictionary: HashMap<String, Int>? = null

        fun unbase(str: String): Int {
            var ret = 0
            if (alphabet == null) {
                ret = str.toIntOrNull(radix) ?: 0
            } else {
                val tmp = StringBuilder(str).reverse().toString()
                for (i in tmp.indices) {
                    val charStr = tmp.substring(i, i + 1)
                    val dictVal = dictionary?.get(charStr) ?: 0
                    ret += (radix.toDouble().pow(i.toDouble()) * dictVal).toInt()
                }
            }
            return ret
        }

        init {
            if (radix > 36) {
                when {
                    radix < 62 -> alphabet = ALPHABET_62.substring(0, radix)
                    radix in 63..94 -> alphabet = ALPHABET_95.substring(0, radix)
                    radix == 62 -> alphabet = ALPHABET_62
                    radix == 95 -> alphabet = ALPHABET_95
                }
                dictionary = HashMap(95)
                for (i in 0 until (alphabet?.length ?: 0)) {
                    dictionary?.put(alphabet!!.substring(i, i + 1), i)
                }
            }
        }
    }

    companion object {
        fun unpack(script: String): String? {
            val unpacker = JsUnpacker(script)
            return if (unpacker.detect()) unpacker.unpack() ?: script else script
        }
    }
}
