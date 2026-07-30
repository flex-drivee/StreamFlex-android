package com.streamflex.core.network

import com.streamflex.core.constants.Constants
import com.streamflex.core.logger.Logger
import com.streamflex.core.network.interceptor.LoggingInterceptor
import com.streamflex.core.network.interceptor.RefererInterceptor
import com.streamflex.core.network.interceptor.RetryInterceptor
import com.streamflex.core.network.interceptor.SecurityInterceptor
import com.streamflex.core.network.interceptor.UserAgentInterceptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.JavaNetCookieJar
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit

/**
 * StreamFlexHttpClient
 *
 * The single, authoritative HTTP client for all StreamFlex network operations.
 * Every provider, extractor, and engine component routes requests through here.
 *
 * ─── Design Principles ─────────────────────────────────────────────────────
 *
 * 1. ONE base client, per-request customisation via [NetworkRequest].
 *    Providers never construct OkHttpClient directly — they build a
 *    [NetworkRequest] and call [execute]. This keeps OkHttp as a
 *    private implementation detail.
 *
 * 2. Interceptor chain (applied to every request):
 *    ┌────────────────────────────────────────────────┐
 *    │  SecurityInterceptor   (HTTPS + redirect guard) │  ← outermost
 *    │  UserAgentInterceptor  (inject UA if absent)    │
 *    │  RefererInterceptor    (preserve Referer/Origin)│
 *    │  LoggingInterceptor    (req → / ← resp timing)  │
 *    │  RetryInterceptor      (IO retry + backoff)      │  ← innermost
 *    └────────────────────────────────────────────────┘
 *
 * 3. Cookie jar is session-scoped and shared across providers.
 *    Some providers (e.g. HDHub4u) require session cookies from their
 *    homepage to serve content pages. The shared jar handles this.
 *    Use [clearCookies] between sessions if needed.
 *
 * 4. Redirect behaviour:
 *    - OkHttp follows 301/302 automatically when [NetworkRequest.followRedirects] = true.
 *    - SecurityInterceptor blocks redirects to http:// targets.
 *    - Max hops: [Constants.MAX_REDIRECT_HOPS] (8).
 *
 * 5. Always returns [NetworkResult] — never throws to the caller.
 *    All exceptions are caught and wrapped in [NetworkResult.Exception].
 *
 * 6. Coroutines-friendly: [execute] is a suspend function that runs
 *    network I/O on [Dispatchers.IO], keeping the main thread clean.
 *
 * ─── Inspired by ───────────────────────────────────────────────────────────
 * CloudStream's `app` object (their singleton HTTP client used by all providers).
 * We improve on it with: explicit rate-limit support via [RateLimiter],
 * cleaner error hierarchy via [NetworkResult], and strict security rules.
 *
 * ─── Usage ─────────────────────────────────────────────────────────────────
 * ```kotlin
 * // Simple GET
 * val result = StreamFlexHttpClient.get("https://example.com")
 *
 * // With Referer and custom headers (e.g. for FileMoon extraction)
 * val result = StreamFlexHttpClient.get(
 *     url     = "https://filemoon.sx/e/abc123",
 *     headers = mapOf("Referer" to "https://filemoon.sx/")
 * )
 *
 * // POST with JSON body
 * val result = StreamFlexHttpClient.post(
 *     url     = "https://api.example.com/search",
 *     body    = """{"q":"Inception"}""".toByteArray(),
 *     headers = mapOf("Content-Type" to "application/json")
 * )
 *
 * // Handle result
 * when (result) {
 *     is NetworkResult.Success  -> result.data.bodyAsString()
 *     is NetworkResult.Error    -> log("HTTP ${result.code}: ${result.message}")
 *     is NetworkResult.Timeout  -> log("Timed out")
 *     is NetworkResult.Exception -> log("Exception: ${result.throwable}")
 *     else -> {}
 * }
 * ```
 */
object StreamFlexHttpClient {

    private const val TAG = "StreamFlexHttpClient"

    // ─── Cookie Jar ───────────────────────────────────────────────────────────
    private val cookieManager = CookieManager().apply {
        setCookiePolicy(CookiePolicy.ACCEPT_ALL)
    }

    // ─── Base OkHttpClient ────────────────────────────────────────────────────
    /**
     * Shared, lazily-initialised OkHttpClient.
     *
     * All interceptors are applied to every request.
     * Per-request customisations (redirect, timeout, cookies) are applied
     * via [buildClientFor] which creates a cheap derivative of this client.
     *
     * Lazy init: OkHttp creates thread pools and connection pools at build time.
     * Deferring until first use avoids slowing down app startup.
     */
    private val baseClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // ─── Cookie Jar ────────────────────────────────────────────────
            .cookieJar(JavaNetCookieJar(cookieManager))

            // ─── Redirect ──────────────────────────────────────────────────
            // SecurityInterceptor also validates redirect targets.
            .followRedirects(true)
            .followSslRedirects(true)

            // ─── Timeouts ──────────────────────────────────────────────────
            .connectTimeout(Constants.CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(Constants.READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .writeTimeout(Constants.WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS)

            // ─── Interceptor Chain ─────────────────────────────────────────
            // Applied in order, outermost first.
            .addInterceptor(SecurityInterceptor())     // HTTPS + redirect guard
            .addInterceptor(UserAgentInterceptor())    // Default UA injection
            .addInterceptor(RefererInterceptor())      // Preserve Referer/Origin
            .addInterceptor(LoggingInterceptor())      // Request/response logs
            .addInterceptor(RetryInterceptor())        // Retry on IO failure

            .build()
    }

    // ─── NoCookieJar ──────────────────────────────────────────────────────────
    private object NoCookieJar : okhttp3.CookieJar {
        override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<okhttp3.Cookie>) = Unit
        override fun loadForRequest(url: okhttp3.HttpUrl): List<okhttp3.Cookie> = emptyList()
    }

    // ─── Per-Request Client ───────────────────────────────────────────────────
    /**
     * Derives a lightweight OkHttpClient from [baseClient] with per-request
     * overrides applied. OkHttp client derivation reuses connection pools and
     * thread pools — this is O(1) and very cheap.
     */
    private fun buildClientFor(request: NetworkRequest): OkHttpClient {
        val builder = baseClient.newBuilder()

        // Redirect override
        builder.followRedirects(request.followRedirects)
        builder.followSslRedirects(request.followRedirects)

        // Timeout override (if caller specifies non-default)
        if (request.timeout != Constants.READ_TIMEOUT_MS) {
            builder.connectTimeout(request.timeout, TimeUnit.MILLISECONDS)
            builder.readTimeout(request.timeout, TimeUnit.MILLISECONDS)
            builder.writeTimeout(request.timeout, TimeUnit.MILLISECONDS)
        }

        // Cookie jar override
        if (!request.useCookies) {
            builder.cookieJar(NoCookieJar)
        }

        return builder.build()
    }

    // ─── Core Execute ─────────────────────────────────────────────────────────

    /**
     * Executes a [NetworkRequest] and returns a [NetworkResult].
     *
     * This is the only method that touches OkHttp directly.
     * All other methods (get, post, etc.) delegate here.
     *
     * Always runs on [Dispatchers.IO]. Never throws to the caller.
     */
    suspend fun execute(request: NetworkRequest): NetworkResult<NetworkResponse> =
        withContext(Dispatchers.IO) {
            try {
                val okHttpRequest = buildOkHttpRequest(request)
                val client = buildClientFor(request)

                val startMs = System.currentTimeMillis()
                val response = client.newCall(okHttpRequest).execute()
                val durationMs = System.currentTimeMillis() - startMs

                val contentType = response.header("Content-Type")
                val contentLength = response.header("Content-Length")?.toLongOrNull() ?: -1L

                val bodyBytes = response.body?.bytes()

                if (response.isSuccessful) {
                    NetworkResult.Success(
                        NetworkResponse(
                            url           = response.request.url.toString(),
                            code          = response.code,
                            message       = response.message,
                            isSuccessful  = true,
                            body          = bodyBytes,
                            headers       = response.headers.toMultimap(),
                            contentType   = contentType,
                            contentLength = contentLength,
                            responseTime  = durationMs
                        )
                    )
                } else {
                    NetworkResult.Error(
                        code     = response.code,
                        message  = response.message,
                        response = NetworkResponse(
                            url           = response.request.url.toString(),
                            code          = response.code,
                            message       = response.message,
                            isSuccessful  = false,
                            body          = bodyBytes,
                            headers       = response.headers.toMultimap(),
                            contentType   = contentType,
                            contentLength = contentLength,
                            responseTime  = durationMs
                        )
                    )
                }

            } catch (e: java.net.SocketTimeoutException) {
                Logger.w(message = "Request timed out: ${request.url}", tag = TAG)
                NetworkResult.Timeout(message = "Request timed out: ${e.message}")

            } catch (e: java.net.UnknownHostException) {
                Logger.w(message = "DNS failure: ${request.url}", tag = TAG)
                NetworkResult.NetworkError(message = "DNS failure: ${e.message}")

            } catch (e: SecurityException) {
                Logger.w(message = "Security blocked: ${e.message}", tag = TAG)
                NetworkResult.Exception(throwable = e)

            } catch (e: Exception) {
                Logger.e(
                    message   = "Request failed: ${request.url}",
                    throwable = e,
                    tag       = TAG
                )
                NetworkResult.Exception(throwable = e)
            }
        }

    // ─── OkHttp Request Builder ───────────────────────────────────────────────

    private fun buildOkHttpRequest(request: NetworkRequest): Request {
        val builder = Request.Builder().url(request.url)

        // Apply all headers from NetworkRequest
        request.headers.forEach { (key, value) -> builder.addHeader(key, value) }

        // Apply Referer / Origin from NetworkRequest fields
        request.referer?.let { builder.header("Referer", it) }
        request.origin?.let  { builder.header("Origin",  it) }

        // Apply cookies as Cookie header (supplement the CookieJar)
        if (request.cookies.isNotEmpty()) {
            val cookieHeader = request.cookies.entries
                .joinToString("; ") { (k, v) -> "$k=$v" }
            builder.addHeader("Cookie", cookieHeader)
        }

        // HTTP method + body
        val mediaType = "application/octet-stream".toMediaTypeOrNull()

        when (request.method) {
            HttpMethod.GET     -> builder.get()
            HttpMethod.DELETE  -> builder.delete()
            HttpMethod.HEAD    -> builder.head()
            HttpMethod.OPTIONS -> builder.method("OPTIONS", null)
            HttpMethod.POST    -> builder.post(
                request.body?.toRequestBody(mediaType) ?: ByteArray(0).toRequestBody(mediaType)
            )
            HttpMethod.PUT     -> builder.put(
                request.body?.toRequestBody(mediaType) ?: ByteArray(0).toRequestBody(mediaType)
            )
            HttpMethod.PATCH   -> builder.patch(
                request.body?.toRequestBody(mediaType) ?: ByteArray(0).toRequestBody(mediaType)
            )
        }

        return builder.build()
    }

    // ─── Convenience Extensions ───────────────────────────────────────────────

    /**
     * GET request. The most common operation in stream resolution.
     *
     * @param url     Target URL. Must be https://.
     * @param headers Additional headers (e.g. Referer for extractors).
     * @param referer Shorthand for the Referer header.
     * @param cookies Session cookies to send with this request.
     * @param timeout Custom read timeout in ms (default: 30s).
     */
    suspend fun get(
        url     : String,
        headers : Map<String, String> = emptyMap(),
        referer : String?             = null,
        origin  : String?             = null,
        cookies : Map<String, String> = emptyMap(),
        timeout : Long                = Constants.READ_TIMEOUT_MS
    ): NetworkResult<NetworkResponse> = execute(
        NetworkRequest(
            url            = url,
            method         = HttpMethod.GET,
            headers        = headers,
            referer        = referer,
            origin         = origin,
            cookies        = cookies,
            timeout        = timeout,
            followRedirects = true,
            useCookies     = true
        )
    )

    /**
     * POST request with a raw byte body.
     * Use for JSON search APIs (e.g. Typesense, Algolia) and AJAX endpoints.
     *
     * @param url         Target URL.
     * @param body        Raw body bytes. Pass JSON as [String.toByteArray()].
     * @param headers     Additional headers. Set "Content-Type" here.
     * @param referer     Shorthand for Referer header.
     * @param followRedirects  Set false for endpoints that should not follow 302.
     */
    suspend fun post(
        url             : String,
        body            : ByteArray?          = null,
        headers         : Map<String, String> = emptyMap(),
        referer         : String?             = null,
        origin          : String?             = null,
        cookies         : Map<String, String> = emptyMap(),
        timeout         : Long                = Constants.READ_TIMEOUT_MS,
        followRedirects : Boolean             = true
    ): NetworkResult<NetworkResponse> = execute(
        NetworkRequest(
            url             = url,
            method          = HttpMethod.POST,
            headers         = headers,
            referer         = referer,
            origin          = origin,
            cookies         = cookies,
            body            = body,
            timeout         = timeout,
            followRedirects = followRedirects,
            useCookies      = true
        )
    )

    /**
     * POST with a JSON string body — the most common POST pattern.
     */
    suspend fun postJson(
        url     : String,
        json    : String,
        headers : Map<String, String> = emptyMap(),
        referer : String?             = null,
        origin  : String?             = null
    ): NetworkResult<NetworkResponse> = post(
        url     = url,
        body    = json.toByteArray(Charsets.UTF_8),
        headers = mapOf("Content-Type" to "application/json") + headers,
        referer = referer,
        origin  = origin
    )

    /**
     * POST with a form body (application/x-www-form-urlencoded).
     * Used by some older streaming providers that use PHP forms.
     */
    suspend fun postForm(
        url     : String,
        form    : Map<String, String>,
        headers : Map<String, String> = emptyMap(),
        referer : String?             = null
    ): NetworkResult<NetworkResponse> {
        val encoded = form.entries
            .joinToString("&") { (k, v) ->
                "${java.net.URLEncoder.encode(k, "UTF-8")}=${java.net.URLEncoder.encode(v, "UTF-8")}"
            }
        return post(
            url     = url,
            body    = encoded.toByteArray(Charsets.UTF_8),
            headers = mapOf("Content-Type" to "application/x-www-form-urlencoded") + headers,
            referer = referer
        )
    }

    /**
     * HEAD request — used to resolve final URL after redirect chain
     * without downloading the full body.
     *
     * Useful in the Redirect Resolver stage (Stage 6) to efficiently
     * trace where a gateway URL resolves to.
     */
    suspend fun head(
        url     : String,
        headers : Map<String, String> = emptyMap(),
        referer : String?             = null
    ): NetworkResult<NetworkResponse> = execute(
        NetworkRequest(
            url            = url,
            method         = HttpMethod.HEAD,
            headers        = headers,
            referer        = referer,
            followRedirects = false,  // HEAD is used to DETECT redirects, not follow them
            useCookies     = true,
            timeout        = Constants.CONNECT_TIMEOUT_MS
        )
    )

    // ─── Cookie Management ────────────────────────────────────────────────────

    /** Clears all session cookies. Call between user sessions if needed. */
    fun clearCookies() {
        cookieManager.cookieStore.removeAll()
    }

    /** Returns the current cookies for a given domain (for debug/testing). */
    fun getCookiesForDomain(domain: String): List<String> {
        return try {
            cookieManager.cookieStore.get(java.net.URI("https://$domain"))
                .map { "${it.name}=${it.value}" }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
