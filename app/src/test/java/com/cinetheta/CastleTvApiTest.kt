package com.cinetheta

import org.junit.Test
import kotlinx.coroutines.runBlocking
import com.cinetheta.providers.castletv.CastleTvCrypto
import kotlinx.serialization.json.*
import java.net.URL
import java.net.HttpURLConnection

class CastleTvApiTest {
    @Test
    fun testLanguageId() = runBlocking {
        try {
            val searchUrl = "https://api.hlowb.com/film-api/v1.1.0/movie/searchByKeyword?channel=IndiaA&clientType=1&keyword=Reacher"
            val searchData = CastleTvCrypto.fetchAndDecrypt(searchUrl)
            val movieId = searchData["data"]?.jsonObject?.get("rows")?.jsonArray?.get(0)?.jsonObject?.get("id")?.jsonPrimitive?.content ?: return@runBlocking
            println("Movie ID: $movieId")
            
            val detailsUrl = "https://api.hlowb.com/film-api/v1.9.9/movie?channel=IndiaA&clientType=1&lang=en-US&movieId=$movieId"
            val detailsData = CastleTvCrypto.fetchAndDecrypt(detailsUrl)
            val sMovieId = detailsData["data"]?.jsonObject?.get("seasons")?.jsonArray?.get(0)?.jsonObject?.get("movieId")?.jsonPrimitive?.content ?: return@runBlocking
            println("Season Movie ID: $sMovieId")
            
            val sUrl = "https://api.hlowb.com/film-api/v1.9.9/movie?channel=IndiaA&clientType=1&lang=en-US&movieId=$sMovieId"
            val sData = CastleTvCrypto.fetchAndDecrypt(sUrl)
            val ep1 = sData["data"]?.jsonObject?.get("episodes")?.jsonArray?.get(0)?.jsonObject ?: return@runBlocking
            val epId = ep1["id"]?.jsonPrimitive?.content ?: return@runBlocking
            println("Episode ID: $epId")
            
            val tracks = ep1["tracks"]?.jsonArray
            println("Tracks: $tracks")
            
            var langIdToTest = "1018"
            if (tracks != null) {
                for (track in tracks) {
                    val langName = track.jsonObject["languageName"]?.jsonPrimitive?.content
                    val langId = track.jsonObject["languageId"]?.jsonPrimitive?.content
                    if (langName == "Hindi" && langId != null) {
                        langIdToTest = langId
                    }
                }
            }
            
            println("Testing with langId: $langIdToTest")
            
            val variants = listOf(
                Pair("String langId", ",\n  \"languageId\": \"$langIdToTest\""),
                Pair("Int langId", ",\n  \"languageId\": $langIdToTest"),
                Pair("String trackId", ",\n  \"trackId\": \"$langIdToTest\""),
                Pair("String audioTrack", ",\n  \"audioTrack\": \"$langIdToTest\""),
                Pair("String track", ",\n  \"track\": \"$langIdToTest\"")
            )
            
            for (variant in variants) {
                val jsonBody = """
                {
                  "mode": "1",
                  "appMarket": "GuanWang",
                  "clientType": "1",
                  "woolUser": "false",
                  "apkSignKey": "ED0955EB04E67A1D9F3305B95454FED485261475",
                  "androidVersion": "13",
                  "movieId": "$sMovieId",
                  "episodeId": "$epId",
                  "isNewUser": "true",
                  "resolution": ""${variant.second}
                }
                """.trimIndent()
                
                val url = "https://api.hlowb.com/film-api/v2.0.1/movie/getVideo2?clientType=1&packageName=com.external.castle&channel=IndiaA&lang=en-US"
                try {
                    val dec = CastleTvCrypto.postAndDecrypt(url, jsonBody)
                    val vUrl = dec["data"]?.jsonObject?.get("videoUrl")?.jsonPrimitive?.content
                    println("${variant.first}: SUCCESS -> ${vUrl?.take(80)}")
                } catch (e: Exception) {
                    println("${variant.first}: ERROR -> ${e.message}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
