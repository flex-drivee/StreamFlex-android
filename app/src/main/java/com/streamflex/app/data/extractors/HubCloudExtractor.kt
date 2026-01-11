package com.streamflex.app.data.extractors

import com.streamflex.app.data.models.StreamLink
import com.streamflex.app.utils.Unpacker
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

object HubCloudExtractor {
    // ⚡ Client that follows redirects automatically
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun extract(url: String): List<StreamLink> {
        val links = mutableListOf<StreamLink>()

        try {
            println("StreamFlex: [Extractor] Resolving -> $url")

            // 1. Follow the redirect chain to find the real host (GamerXYT, HubStream, etc.)
            val (finalUrl, html) = followRedirectsAndGetHtml(url)
            println("StreamFlex: [Extractor] Landed on Host -> $finalUrl")

            // 2. Identify the Host and Extract

            // --- CASE A: HubStream / HDStream4u (The Best for Streaming) ---
            if (finalUrl.contains("hubstream") || finalUrl.contains("hdstream4u")) {
                println("StreamFlex: Detected HubStream (Streaming Mode)")
                if (html.contains("eval(function(p,a,c,k,e,d)")) {
                    val unpacked = Unpacker.unpack(html)
                    if (unpacked != null) {
                        // Find .m3u8 in the unpacked code
                        val m3u8Matcher = Pattern.compile("""(https?://[^"']+\.m3u8[^"']*)""").matcher(unpacked)
                        while (m3u8Matcher.find()) {
                            val streamUrl = m3u8Matcher.group(1)!!
                            println("StreamFlex: Found M3U8 -> $streamUrl")
                            links.add(StreamLink(streamUrl, "HubStream (HLS)", true))
                        }
                    }
                }
            }

            // --- CASE B: GamerXYT / FSL (MKV Files) ---
            else if (finalUrl.contains("gamerxyt") || finalUrl.contains("fsl.firecdn") || finalUrl.contains("hexupload")) {
                println("StreamFlex: Detected GamerXYT (Download Mode)")
                if (html.contains("eval(function(p,a,c,k,e,d)")) {
                    val unpacked = Unpacker.unpack(html)
                    if (unpacked != null) {
                        // Find .mkv or .mp4 in the unpacked code
                        val videoMatcher = Pattern.compile("""(https?://[^"']+\.(?:mkv|mp4)[^"']*)""").matcher(unpacked)
                        while (videoMatcher.find()) {
                            val fileUrl = videoMatcher.group(1)!!
                            println("StreamFlex: Found MKV/MP4 -> $fileUrl")
                            links.add(StreamLink(fileUrl, "FSL (MKV)", false))
                        }
                    }
                }
            }

            // --- CASE C: PixelDrain (Direct MKV) ---
            else if (finalUrl.contains("pixeldrain")) {
                println("StreamFlex: Detected PixelDrain")
                val idMatcher = Pattern.compile("""pixeldrain\.com/u/([a-zA-Z0-9]+)""").matcher(finalUrl)
                if (idMatcher.find()) {
                    val id = idMatcher.group(1)
                    val directUrl = "https://pixeldrain.com/api/file/$id"
                    println("StreamFlex: Found PixelDrain API -> $directUrl")
                    links.add(StreamLink(directUrl, "PixelDrain (MKV)", false))
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return links
    }

    // Helper: Recursively follows HubCloud/HubDrive redirects until it hits the final host
    private fun followRedirectsAndGetHtml(url: String): Pair<String, String> {
        var currentUrl = url
        var html = ""

        // Max 5 redirects to prevent infinite loops
        for (i in 0..5) {
            val request = Request.Builder()
                .url(currentUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .build()

            val response = client.newCall(request).execute()
            html = response.body?.string() ?: ""
            val responseUrl = response.request.url.toString()

            // 1. Check if we are on a "Drive" page (Intermediary)
            if (html.contains("hubcloud") || html.contains("hubdrive")) {
                // Look for the "Download" or "Watch" button link
                val redirectMatcher = Pattern.compile("""<a href="([^"]+)"[^>]*>(?:Download|Watch)</a>""").matcher(html)
                if (redirectMatcher.find()) {
                    currentUrl = redirectMatcher.group(1)!!
                    continue // Loop again with new URL
                }
            }

            // If we are here, we are likely on the final page (GamerXYT, PixelDrain, etc.)
            return Pair(responseUrl, html)
        }
        return Pair(currentUrl, html)
    }
}