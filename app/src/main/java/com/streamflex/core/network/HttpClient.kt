package com.streamflex.core.network

import com.streamflex.core.logger.Logger
import okhttp3.JavaNetCookieJar
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit

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

            .build()
    }

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

    fun execute(request: NetworkRequest): NetworkResult<NetworkResponse> {

        return try {

            val builder = Request.Builder()
                .url(request.url)
                .headers(request.headers)

            when (request.method.uppercase()) {

                "POST" -> {
                    builder.post(
                        request.body?.toRequestBody(
                            "application/octet-stream".toMediaTypeOrNull()
                        ) ?: ByteArray(0).toRequestBody()
                    )
                }

                "PUT" -> {
                    builder.put(
                        request.body?.toRequestBody(
                            "application/octet-stream".toMediaTypeOrNull()
                        ) ?: ByteArray(0).toRequestBody()
                    )
                }

                "DELETE" -> builder.delete()

                else -> builder.get()
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
                    headers = response.headers,
                    url = response.request.url.toString(),
                    isSuccessful = response.isSuccessful
                )
            )

        } catch (e: Exception) {

            Logger.e(
                tag = "HttpClient",
                message = "Network request failed",
                throwable = e
            )

            NetworkResult.Error(e)

        }
    }

    fun clearCookies() {
        cookieManager.cookieStore.removeAll()
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