package com.cinetheta.providers.youtube

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import com.cinetheta.core.network.CineThetaHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Headers.Companion.toHeaders

class NewPipeDownloader : Downloader() {
    
    companion object {
        private val client = CineThetaHttpClient.okHttpClient
        
        fun init() {
            try {
                val locale = java.util.Locale.getDefault()
                val language = locale.language.ifBlank { "en" }
                val country = locale.country.ifBlank { "US" }
                
                org.schabi.newpipe.extractor.NewPipe.init(
                    NewPipeDownloader(),
                    org.schabi.newpipe.extractor.localization.Localization(language),
                    org.schabi.newpipe.extractor.localization.ContentCountry(country)
                )
            } catch (e: Exception) {
                // Ignore if already initialized
            }
        }
    }
    
    override fun execute(request: Request): Response {
        val builder = okhttp3.Request.Builder()
            .url(request.url())
            .method(request.httpMethod(), request.dataToSend()?.toRequestBody())
            
        request.headers()?.let { map ->
            val flatHeaders = mutableMapOf<String, String>()
            map.forEach { (key, list) ->
                if (list.isNotEmpty()) flatHeaders[key] = list[0]
            }
            builder.headers(flatHeaders.toHeaders())
        }
        
        val call = client.newCall(builder.build())
        val response = call.execute()
        
        val responseHeaders = mutableMapOf<String, List<String>>()
        for (i in 0 until response.headers.size) {
            val name = response.headers.name(i)
            val value = response.headers.value(i)
            val list = responseHeaders[name]?.toMutableList() ?: mutableListOf()
            list.add(value)
            responseHeaders[name] = list
        }
        
        return Response(
            response.code,
            response.message,
            responseHeaders,
            response.body?.string() ?: "",
            response.request.url.toString()
        )
    }
}
