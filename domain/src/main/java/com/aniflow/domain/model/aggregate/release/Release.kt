package com.aniflow.domain.model.aggregate.release

import com.aniflow.domain.identity.AnimeIdentity
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.ReleaseIdentity
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.EpisodeRange
import com.aniflow.domain.valueobject.InfoHash
import com.aniflow.domain.valueobject.ParseInfo
import com.aniflow.domain.valueobject.ReleaseTechnicalMetadata
import com.aniflow.domain.valueobject.SeasonNumber
import com.aniflow.domain.valueobject.UrlValue
import java.time.Instant

/**
 * High-level categorization of the release structure (Section 16).
 */
enum class ReleaseType {
    SingleEpisode,
    MultiEpisode,
    Batch,
    Season,
    CompleteSeries,
    Movie,
    OVA,
    Special,
    Unknown
}

/**
 * Concrete download source for the release (Section 30).
 */
sealed interface ReleaseSource {
    data class Torrent(
        val infoHash: InfoHash,
        val torrentUrl: UrlValue.TorrentUrl? = null,
        val magnetUri: UrlValue.MagnetUri? = null
    ) : ReleaseSource

    data class DirectHttp(
        val url: UrlValue.HttpsUrl
    ) : ReleaseSource

    data class HttpFile(
        val url: UrlValue.HttpsUrl
    ) : ReleaseSource

    data object Unknown : ReleaseSource
}

/**
 * Dynamic network availability metrics (Section 29).
 * Decoupled from static release identity as these values fluctuate constantly.
 */
data class ReleaseAvailability(
    val size: ByteSize? = null,
    val seeders: Int? = null,
    val leechers: Int? = null,
    val completedDownloads: Long? = null,
    val lastCheckedAt: Instant? = null
) {
    init {
        if (seeders != null) require(seeders >= 0) { "seeders cannot be negative" }
        if (leechers != null) require(leechers >= 0) { "leechers cannot be negative" }
        if (completedDownloads != null) require(completedDownloads >= 0) { "completedDownloads cannot be negative" }
    }

    val isHealthy: Boolean get() = (seeders ?: 0) >= 3
}

/**
 * The core Release Aggregate Root (Section 15).
 * Represents an external discovery item from any anime provider (e.g. Nyaa).
 */
data class Release(
    val id: ReleaseId,
    val provider: ProviderRef,
    val providerReleaseId: String?,
    val title: String,
    val normalizedTitle: String,
    val releaseType: ReleaseType = ReleaseType.SingleEpisode,
    val animeIdentity: AnimeIdentity? = null,
    val seasonHint: SeasonNumber? = null,
    val episodeRange: EpisodeRange? = null,
    val uploader: Uploader? = null,
    val releaseGroup: ReleaseGroup? = null,
    val technical: ReleaseTechnicalMetadata = ReleaseTechnicalMetadata(null, null, emptyList(), emptyList(), null, null),
    val availability: ReleaseAvailability = ReleaseAvailability(),
    val source: ReleaseSource = ReleaseSource.Unknown,
    val publishedAt: Instant? = null,
    val identity: ReleaseIdentity,
    val parseInfo: ParseInfo = ParseInfo.perfect(),
    val discoveredAt: Instant = Instant.now(),
    val lastUpdatedAt: Instant = Instant.now()
) {
    init {
        require(title.isNotBlank()) { "Release title cannot be blank" }
        require(normalizedTitle.isNotBlank()) { "Release normalizedTitle cannot be blank" }
    }

    val isBatch: Boolean
        get() = releaseType == ReleaseType.Batch ||
            releaseType == ReleaseType.MultiEpisode ||
            releaseType == ReleaseType.Season ||
            releaseType == ReleaseType.CompleteSeries ||
            (episodeRange != null && episodeRange.isBatch)
}
