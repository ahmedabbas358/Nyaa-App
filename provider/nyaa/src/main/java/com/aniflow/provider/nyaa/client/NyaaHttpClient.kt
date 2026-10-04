package com.aniflow.provider.nyaa.client

import com.aniflow.provider.core.error.ProviderError
import com.aniflow.provider.nyaa.config.NyaaProviderConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Dedicated HTTP client adapter for Nyaa requests (Sections 20, 21, 22, 24).
 * Handles headers, timeouts, status codes, and converts network responses to Provider errors.
 */
class NyaaHttpClient(
    private val config: NyaaProviderConfig = NyaaProviderConfig.DEFAULT,
    private val baseOkHttpClient: OkHttpClient = OkHttpClient()
) {

    private val client: OkHttpClient = baseOkHttpClient.newBuilder()
        .dns(ResilientNyaaDns())
        .connectTimeout(config.connectTimeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(config.readTimeoutMs, TimeUnit.MILLISECONDS)
        .writeTimeout(config.writeTimeoutMs, TimeUnit.MILLISECONDS)
        .callTimeout(config.callTimeoutMs, TimeUnit.MILLISECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun get(url: String): HttpResponse = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", config.userAgent)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9,ja;q=0.8")
            .get()
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            handleResponse(response, url)
        } catch (e: SocketTimeoutException) {
            throw ProviderError.Timeout(config.callTimeoutMs)
        } catch (e: IOException) {
            throw ProviderError.NetworkFailure("I/O error connecting to $url: ${e.message}", e)
        }
    }

    private fun handleResponse(response: Response, url: String): HttpResponse {
        response.use { res ->
            val code = res.code
            val bodyString = res.body?.string().orEmpty()

            when (code) {
                in 200..299 -> {
                    return HttpResponse(
                        statusCode = code,
                        body = bodyString,
                        contentType = res.header("Content-Type")
                    )
                }
                404 -> throw ProviderError.NotFound("Resource not found at $url")
                429 -> {
                    val retryAfterSeconds = res.header("Retry-After")?.toLongOrNull()
                    throw ProviderError.RateLimited(retryAfterSeconds)
                }
                403 -> throw ProviderError.Blocked("HTTP 403 Forbidden from Nyaa")
                in 500..599 -> throw ProviderError.Unavailable("Nyaa server error HTTP $code: $bodyString")
                else -> throw ProviderError.InvalidResponse(code, "Unexpected HTTP code $code from $url")
            }
        }
    }

    data class HttpResponse(
        val statusCode: Int,
        val body: String,
        val contentType: String?
    )
}
