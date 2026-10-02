package com.aniflow.download.http

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class RangeProbeResult(
    val supportsRange: Boolean,
    val contentLength: Long?,
    val etag: String?,
    val lastModified: String?
)

/**
 * Probes HTTP servers to verify Range request support and content length (Section 53, 54, 61, 62).
 */
class RangeProbe(
    private val httpClient: OkHttpClient
) {
    suspend fun probe(url: String, headers: Map<String, String> = emptyMap()): RangeProbeResult = withContext(Dispatchers.IO) {
        val headRequest = Request.Builder()
            .url(url)
            .head()
            .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
            .build()

        try {
            httpClient.newCall(headRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val acceptRanges = response.header("Accept-Ranges")
                    val contentLength = response.header("Content-Length")?.toLongOrNull()
                    val etag = response.header("ETag")
                    val lastModified = response.header("Last-Modified")

                    val supportsRange = acceptRanges?.equals("bytes", ignoreCase = true) == true ||
                        (contentLength != null && contentLength > 0)

                    return@withContext RangeProbeResult(
                        supportsRange = supportsRange,
                        contentLength = contentLength,
                        etag = etag,
                        lastModified = lastModified
                    )
                }
            }
        } catch (_: Exception) {
            // Fallback: try small GET range request
        }

        // Fallback: probe with a 0-0 range request
        try {
            val rangeRequest = Request.Builder()
                .url(url)
                .header("Range", "bytes=0-0")
                .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
                .build()

            httpClient.newCall(rangeRequest).execute().use { response ->
                val supportsRange = response.code == 206
                val contentRange = response.header("Content-Range")
                val totalLength = contentRange?.substringAfterLast('/')?.toLongOrNull()
                val etag = response.header("ETag")
                val lastModified = response.header("Last-Modified")

                RangeProbeResult(
                    supportsRange = supportsRange,
                    contentLength = totalLength,
                    etag = etag,
                    lastModified = lastModified
                )
            }
        } catch (e: Exception) {
            RangeProbeResult(
                supportsRange = false,
                contentLength = null,
                etag = null,
                lastModified = null
            )
        }
    }
}
