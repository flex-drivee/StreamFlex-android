package com.streamflex.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.streamflex.core.logger.Logger
import okhttp3.JavaNetCookieJar
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit
import com.streamflex.core.network.interceptor.LoggingInterceptor
import com.streamflex.core.network.interceptor.RetryInterceptor
import com.streamflex.core.network.interceptor.UserAgentInterceptor

object HttpClient {

    private const val DEFAULT_TIMEOUT = 30L

    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }
    private val baseClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(JavaNetCookieJar(cookieManager))
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)

            .addInterceptor(UserAgentInterceptor())
            .addInterceptor(LoggingInterceptor())
            .addInterceptor(RetryInterceptor())
            .addInterceptor(com.streamflex.core.network.interceptor.CloudflareKiller())

            .build()
    }

    fun getOkHttpClient(): OkHttpClient = baseClient

    private fun buildClient(request: NetworkRequest): OkHttpClient {

        val builder = baseClient.newBuilder()

        // Redirect handling
        builder.followRedirects(request.followRedirects)
        builder.followSslRedirects(request.followRedirects)

        // Timeout
        builder.connectTimeout(request.timeout, TimeUnit.MILLISECONDS)
        builder.readTimeout(request.timeout, TimeUnit.MILLISECONDS)
        builder.writeTimeout(request.timeout, TimeUnit.MILLISECONDS)

        // Cookies
        if (!request.useCookies) {
            builder.cookieJar(NoCookieJar)
        }

        return builder.build()
    }

    suspend fun execute(
        request: NetworkRequest
    ): NetworkResult<NetworkResponse> = withContext(Dispatchers.IO) {

        try {

            val builder = Request.Builder()
                .url(request.url)

            request.headers.forEach { (key, value) ->
                builder.addHeader(key, value)
            }

            val contentTypeHeader = request.headers.entries.firstOrNull { it.key.equals("Content-Type", ignoreCase = true) }?.value
            val mediaTypeStr = contentTypeHeader ?: "application/octet-stream"
            val mediaType = mediaTypeStr.toMediaTypeOrNull()

            when (request.method) {

                HttpMethod.GET -> builder.get()

                HttpMethod.POST -> {
                    builder.post(
                        request.body?.toRequestBody(mediaType) ?: ByteArray(0).toRequestBody()
                    )
                }

                HttpMethod.PUT -> {
                    builder.put(
                        request.body?.toRequestBody(mediaType) ?: ByteArray(0).toRequestBody()
                    )
                }

                HttpMethod.DELETE -> builder.delete()

                HttpMethod.HEAD -> builder.head()

                HttpMethod.PATCH -> {
                    builder.patch(
                        request.body?.toRequestBody(mediaType) ?: ByteArray(0).toRequestBody()
                    )
                }

                HttpMethod.OPTIONS -> {
                    builder.method("OPTIONS", null)
                }
            }

            val client = buildClient(request)

            val response = client
                .newCall(builder.build())
                .execute()

            NetworkResult.Success(
                NetworkResponse(
                    code = response.code,
                    message = response.message,
                    body = response.body?.bytes(),
                    headers = response.headers.toMultimap(),
                    url = response.request.url.toString(),
                    isSuccessful = response.isSuccessful
                )
            )

        } catch (e: Exception) {

            Logger.e(
                message = "Network request failed",
                throwable = e,
                tag = "HttpClient"
            )

            NetworkResult.Exception(e)
        }
    }

    fun clearCookies() {
        cookieManager.cookieStore.removeAll()
    }

    fun getCookies(url: String): List<java.net.HttpCookie> {
        return try {
            cookieManager.cookieStore.get(java.net.URI.create(url))
        } catch (e: Exception) {
            emptyList()
        }
    }

    private object NoCookieJar : okhttp3.CookieJar {

        override fun saveFromResponse(
            url: okhttp3.HttpUrl,
            cookies: List<okhttp3.Cookie>
        ) = Unit

        override fun loadForRequest(
            url: okhttp3.HttpUrl
        ): List<okhttp3.Cookie> = emptyList()

    }

}