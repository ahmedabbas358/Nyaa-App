package com.aniflow.domain.identity

import com.aniflow.domain.valueobject.EpisodeRange
import com.aniflow.domain.valueobject.InfoHash
import com.aniflow.domain.valueobject.SeasonNumber

/**
 * Multi-tiered identity resolution for Anime entities.
 * Prevents naive title merging while recognizing identical titles across various naming styles.
 * Hierarchy:
 * 1. External Provider ID (e.g. AniList ID, MAL ID)
 * 2. Known Canonical ID (internal catalog ID)
 * 3. Normalized Title + Media Type
 * 4. Title Similarity match (confidence threshold)
 */
data class AnimeIdentity(
    val externalProviderId: String? = null,
    val canonicalId: AnimeId? = null,
    val normalizedTitle: String,
    val rawTitle: String,
    val synonyms: Set<String> = emptySet(),
    val matchConfidence: Float = 1.0f
) {
    init {
        require(normalizedTitle.isNotBlank()) { "AnimeIdentity normalizedTitle cannot be blank" }
        require(matchConfidence in 0.0f..1.0f) { "matchConfidence must be between 0.0 and 1.0" }
    }

    /**
     * Determines whether two identities represent the same Anime.
     * Strict order: provider ID match -> canonical ID match -> exact normalized title match.
     */
    fun matches(other: AnimeIdentity): Boolean {
        if (externalProviderId != null && other.externalProviderId != null) {
            return externalProviderId.equals(other.externalProviderId, ignoreCase = true)
        }
        if (canonicalId != null && other.canonicalId != null) {
            return canonicalId == other.canonicalId
        }
        if (normalizedTitle.equals(other.normalizedTitle, ignoreCase = true)) {
            return true
        }
        return synonyms.any { s -> other.synonyms.any { it.equals(s, ignoreCase = true) } }
    }
}

/**
 * Multi-factor identity for a Release to prevent duplicate downloads and track sources.
 * Incorporates:
 * - Provider ID + Provider-assigned item ID
 * - InfoHash (for torrents/magnets)
 * - Canonical Source URI / ID
 * - Normalized title + Episode range
 * - Technical profile signature (Resolution + Codec + Audio)
 */
data class ReleaseIdentity(
    val providerId: ProviderId,
    val providerReleaseId: String?,
    val infoHash: InfoHash?,
    val canonicalSourceUrl: String?,
    val normalizedTitle: String,
    val episodeRange: EpisodeRange?,
    val technicalSignature: String?
) {
    init {
        require(normalizedTitle.isNotBlank()) { "ReleaseIdentity normalizedTitle cannot be blank" }
    }

    /**
     * Checks if this identity is an exact or strong match with another.
     */
    fun isSameRelease(other: ReleaseIdentity): Boolean {
        // Same provider and same item ID is an exact match
        if (providerId == other.providerId && !providerReleaseId.isNullOrBlank() && providerReleaseId == other.providerReleaseId) {
            return true
        }
        // Identical InfoHash is cryptographic proof of identical torrent content
        if (infoHash != null && other.infoHash != null && infoHash == other.infoHash) {
            return true
        }
        // Same canonical URL
        if (!canonicalSourceUrl.isNullOrBlank() && canonicalSourceUrl == other.canonicalSourceUrl) {
            return true
        }
        // Fallback: Exact normalized title + matching episode range
        return normalizedTitle.equals(other.normalizedTitle, ignoreCase = true) &&
            episodeRange != null && episodeRange == other.episodeRange
    }
}

/**
 * Composite identity for an Episode within an Anime / Season.
 */
data class EpisodeIdentity(
    val animeId: AnimeId,
    val seasonNumber: SeasonNumber,
    val episodeNumberMajor: Int,
    val episodeNumberMinor: Int = 0
)

/**
 * Identity specifically for download operations, distinguishing multi-file releases.
 */
data class DownloadIdentity(
    val releaseId: ReleaseId,
    val fileRelativePath: String,
    val infoHash: InfoHash? = null
)

/**
 * Universal media identity connecting local library items to anime/season/episode.
 */
data class MediaIdentity(
    val animeId: AnimeId?,
    val seasonNumber: SeasonNumber?,
    val episodeRange: EpisodeRange?,
    val canonicalTitle: String
)
