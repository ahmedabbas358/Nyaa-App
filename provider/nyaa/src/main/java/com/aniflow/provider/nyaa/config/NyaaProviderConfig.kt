package com.aniflow.provider.nyaa.config

import com.aniflow.domain.valueobject.UrlValue

/**
 * Centralized configuration for the Nyaa provider adapter (Sections 19, 22, 23, 87).
 * Prevents hardcoding "https://nyaa.si/" across multiple source files.
 */
data class NyaaProviderConfig(
    val baseUrl: UrlValue = UrlValue.HttpsUrl("https://nyaa.si/"),
    val userAgent: String = "AniFlow/1.0.0 (Android; Release Discovery Client)",
    val connectTimeoutMs: Long = 15_000L,
    val readTimeoutMs: Long = 20_000L,
    val writeTimeoutMs: Long = 15_000L,
    val callTimeoutMs: Long = 30_000L,
    val requestIntervalMs: Long = 500L,
    val maxConcurrentRequests: Int = 2,
    val maxRetries: Int = 3
) {
    companion object {
        val DEFAULT = NyaaProviderConfig()
    }
}
