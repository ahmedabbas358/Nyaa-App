package com.aniflow.provider.core.model

import com.aniflow.domain.valueobject.UrlValue
import java.time.Instant

/**
 * Normalized identity for uploaders or release groups reported by providers (Section 15).
 */
data class ProviderIdentity(
    val providerId: String?,
    val displayName: String
)

/**
 * Provider-level normalized transport model (Section 14).
 * Decouples provider HTTP/HTML DTOs from pure Domain Release entities.
 */
data class ProviderRelease(
    val providerReleaseId: String?,
    val title: String,
    val detailsUrl: UrlValue?,
    val torrentUrl: UrlValue?,
    val magnetUri: String?,
    val sizeBytes: Long?,
    val seeders: Int?,
    val leechers: Int?,
    val downloads: Long?,
    val publishedAt: Instant?,
    val category: ProviderCategory?,
    val uploader: ProviderIdentity? = null,
    val releaseGroup: String? = null,
    val isTrusted: Boolean = false,
    val isRemake: Boolean = false,
    val description: String? = null,
    val rawMetadata: Map<String, String> = emptyMap()
) {
    val hasTorrentOrMagnet: Boolean
        get() = torrentUrl != null || !magnetUri.isNullOrBlank()
}
