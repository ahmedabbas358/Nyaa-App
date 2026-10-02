package com.aniflow.core.network

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.core.common.result.ErrorType
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.IOException
import java.util.concurrent.TimeUnit

object HttpClientFactory {
    fun createOkHttpClient(
        connectTimeoutSec: Long = 15,
        readTimeoutSec: Long = 20,
        enableLogging: Boolean = false
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(connectTimeoutSec, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSec, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)

        if (enableLogging) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            builder.addInterceptor(logging)
        }

        return builder.build()
    }
}

/**
 * Enforces Section 70 (Provider Rate Control).
 * Limits outgoing requests per second to avoid triggering server rate limits or IP blocks.
 */
class RequestLimiter(private val minIntervalMs: Long = 500L) {
    private val mutex = Mutex()
    private var lastRequestTime = 0L

    suspend fun acquire() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime
            if (elapsed < minIntervalMs) {
                delay(minIntervalMs - elapsed)
            }
            lastRequestTime = System.currentTimeMillis()
        }
    }
}

/**
 * Resilient Retry Policy with exponential backoff.
 */
class RetryPolicy(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 1000L,
    private val maxDelayMs: Long = 8000L,
    private val factor: Double = 2.0
) {
    suspend fun <T> executeWithRetry(
        action: suspend (attempt: Int) -> T
    ): AniFlowResult<T> {
        var currentDelay = initialDelayMs
        var lastException: Exception? = null

        for (attempt in 1..maxRetries) {
            try {
                val result = action(attempt)
                return AniFlowResult.Success(result)
            } catch (e: IOException) {
                lastException = e
                if (attempt == maxRetries) break
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
            } catch (e: Exception) {
                return AniFlowResult.Error(ErrorType.UnknownError(e), e.message ?: "Execution failed", e)
            }
        }

        return AniFlowResult.Error(
            ErrorType.NetworkError,
            "Network request failed after $maxRetries attempts: ${lastException?.message}",
            lastException
        )
    }
}
