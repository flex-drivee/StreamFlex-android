package com.streamflex.app

import org.junit.Assert.*
import org.junit.Test
import java.net.URI
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class DownloadEngineTest {

    @Test
    fun testAes128HlsDecryption() {
        val keyBytes = "1234567890123456".toByteArray(Charsets.UTF_8)
        val ivBytes = "abcdefghijklmnop".toByteArray(Charsets.UTF_8)
        val originalData = "STREAMFLEX_HLS_VIDEO_SEGMENT_DATA_TEST_123456789".toByteArray(Charsets.UTF_8)

        // Encrypt with AES/CBC/PKCS7Padding (or PKCS5Padding)
        val keySpec = SecretKeySpec(keyBytes, "AES")
        val ivSpec = IvParameterSpec(ivBytes)
        val encryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        encryptCipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        val encrypted = encryptCipher.doFinal(originalData)

        assertNotEquals(String(originalData), String(encrypted))

        // Decrypt
        val decryptCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        decryptCipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        val decrypted = decryptCipher.doFinal(encrypted)

        assertArrayEquals(originalData, decrypted)
        assertEquals("STREAMFLEX_HLS_VIDEO_SEGMENT_DATA_TEST_123456789", String(decrypted))
    }

    @Test
    fun testM3u8PlaylistParsingRegex() {
        val sampleM3u8 = """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-TARGETDURATION:10
            #EXT-X-MEDIA-SEQUENCE:0
            #EXT-X-KEY:METHOD=AES-128,URI="https://stream.server.com/key.bin",IV=0x1234567890abcdef1234567890abcdef
            #EXTINF:9.009,
            segment_000.ts
            #EXTINF:9.009,
            segment_001.ts
            #EXTINF:4.500,
            segment_002.ts
            #EXT-X-ENDLIST
        """.trimIndent()

        val lines = sampleM3u8.lines()
        val segments = mutableListOf<String>()
        var keyUri: String? = null

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXT-X-KEY:")) {
                val uriMatch = Regex("""URI="([^"]+)"""").find(trimmed)
                keyUri = uriMatch?.groupValues?.get(1)
            } else if (trimmed.endsWith(".ts")) {
                segments.add(trimmed)
            }
        }

        assertEquals("https://stream.server.com/key.bin", keyUri)
        assertEquals(3, segments.size)
        assertEquals("segment_000.ts", segments[0])
        assertEquals("segment_001.ts", segments[1])
        assertEquals("segment_002.ts", segments[2])
    }

    @Test
    fun testUriResolution() {
        val baseUrl = "https://cdn.example.com/hls/master.m3u8"
        val relative = "segments/chunk_1.ts"
        val resolved = URI(baseUrl).resolve(relative).toString()

        assertEquals("https://cdn.example.com/hls/segments/chunk_1.ts", resolved)
    }
}
