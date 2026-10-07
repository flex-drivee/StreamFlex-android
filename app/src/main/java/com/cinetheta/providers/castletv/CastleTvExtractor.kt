package com.cinetheta.providers.castletv

import android.net.Uri
import com.cinetheta.domain.models.ExtractionResult
import com.cinetheta.domain.models.HostType
import com.cinetheta.domain.models.ProviderSource
import com.cinetheta.domain.models.Subtitle
import com.cinetheta.extractors.common.BaseExtractor
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CastleTvExtractor : BaseExtractor() {
    
    override val hostType = HostType.CASTLETV
    
    override suspend fun extract(source: ProviderSource): ExtractionResult {
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(source.url)
                val movieId = uri.getQueryParameter("movieId") ?: return@withContext emptyResult()
                val episodeId = uri.getQueryParameter("episodeId") ?: return@withContext emptyResult()
                val languageId = uri.getQueryParameter("languageId")
                
                val videoApiUrl = "https://api.hlowb.com/film-api/v2.0.1/movie/getVideo2?clientType=1&packageName=com.external.castle&channel=IndiaA&lang=en-US"
                
                val langStr = if (languageId != null) ",\n  \"languageId\": $languageId" else ""
                
                val jsonBody = """
                {
                  "mode": "1",
                  "appMarket": "GuanWang",
                  "clientType": "1",
                  "woolUser": "false",
                  "apkSignKey": "ED0955EB04E67A1D9F3305B95454FED485261475",
                  "androidVersion": "13",
                  "movieId": "$movieId",
                  "episodeId": "$episodeId",
                  "isNewUser": "true",
                  "resolution": ""$langStr
                }
                """.trimIndent()
                
                val decryptedVideo = CastleTvCrypto.postAndDecrypt(videoApiUrl, jsonBody)
                val videoData = decryptedVideo["data"]?.jsonObject
                
                val videoUrl = videoData?.get("videoUrl")?.jsonPrimitive?.content ?: return@withContext emptyResult()
                
                val subtitles = mutableListOf<Subtitle>()
                val subsArray = videoData.get("subtitles")?.jsonArray
                if (subsArray != null) {
                    for (sub in subsArray) {
                        val subUrl = sub.jsonObject["url"]?.jsonPrimitive?.content ?: continue
                        val title = sub.jsonObject["title"]?.jsonPrimitive?.content ?: "English"
                        subtitles.add(
                            Subtitle(
                                url = subUrl,
                                language = title
                            )
                        )
                    }
                }
                
                val stream = createStream(
                    source = source,
                    url = videoUrl,
                    subtitles = subtitles
                )
                
                result(stream)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyResult()
            }
        }
    }
}
