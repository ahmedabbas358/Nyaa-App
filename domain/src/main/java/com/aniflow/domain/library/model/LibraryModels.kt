package com.aniflow.domain.library.model

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.FileFingerprint
import com.aniflow.domain.identity.LibraryFileId
import com.aniflow.domain.identity.LibraryItemId
import com.aniflow.domain.identity.MediaAssetId
import com.aniflow.domain.identity.SeasonId
import com.aniflow.domain.identity.StorageId
import com.aniflow.domain.library.probe.MediaMetadata
import com.aniflow.domain.storage.model.StorageLocation
import java.time.Instant

/**
 * LibraryItem represents a logical entity in the local index (Section 3, 4).
 * Not every physical file is a LibraryItem; a LibraryItem groups one or more files.
 */
data class LibraryItem(
    val id: LibraryItemId,
    val type: LibraryItemType,
    val title: String?,
    val animeId: AnimeId?,
    val seasonId: SeasonId?,
    val episodeId: EpisodeId?,
    val state: LibraryItemState,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
)

enum class LibraryItemType {
    Anime,
    Season,
    Episode,
    Movie,
    Special,
    Other,
    Unidentified
}

enum class LibraryItemState {
    Indexed,
    Active,
    Missing,
    Corrupt,
    Archived
}

/**
 * LibraryFile represents a verified physical media file known to the index (Section 5, 6).
 */
data class LibraryFile(
    val id: LibraryFileId,
    val libraryItemId: LibraryItemId?,
    val location: StorageLocation,
    val displayName: String,
    val sizeBytes: Long,
    val modifiedAt: Instant?,
    val mediaMetadata: MediaMetadata?,
    val fingerprint: FileFingerprint?,
    val state: LibraryFileState
)

enum class LibraryFileState {
    Present,
    Missing,
    Moved,
    Unindexed,
    Offline,
    Corrupt,
    Unsupported,
    Unknown
}

/**
 * MediaAsset unifies a primary video file with associated sidecar files (subtitles, audio, fonts) (Section 24).
 */
data class MediaAsset(
    val id: MediaAssetId,
    val primaryFileId: LibraryFileId,
    val relatedFiles: List<LibraryFileId> = emptyList()
)

enum class FingerprintAlgorithm {
    SampledQuickHash,
    Sha256Full,
    Md5
}

enum class FingerprintPolicy {
    None,
    Lightweight,
    OnDuplicateSuspicion,
    Full,
    OnUserRequest
}

enum class DuplicateMediaType {
    ExactDuplicate,       // Same size, same fingerprint, same episode
    TechnicalDuplicate,   // Same episode & resolution, but different codec/bitrate (Alternative version)
    DifferentRelease,     // Same episode, different release group/source
    UnknownRelation
}

data class DuplicateComparison(
    val fileA: LibraryFile,
    val fileB: LibraryFile,
    val type: DuplicateMediaType,
    val recommendation: DuplicateResolutionRecommendation
)

enum class DuplicateResolutionRecommendation {
    KeepBoth,
    KeepFileA,
    KeepFileB,
    AskUser
}

/**
 * Library event stream for background automations and reactive UI updates (Section 99).
 */
sealed interface LibraryEvent {
    data class LibraryFileAdded(val file: LibraryFile) : LibraryEvent
    data class LibraryFileRemoved(val fileId: LibraryFileId) : LibraryEvent
    data class LibraryFileMoved(val fileId: LibraryFileId, val from: StorageLocation, val to: StorageLocation) : LibraryEvent
    data class LibraryFileChanged(val fileId: LibraryFileId) : LibraryEvent
    data class LibraryFileMapped(val fileId: LibraryFileId, val animeId: AnimeId, val episodeId: EpisodeId?) : LibraryEvent
    data class LibraryItemUpdated(val itemId: LibraryItemId) : LibraryEvent
    data class StorageAvailabilityChanged(val storageId: StorageId, val newState: com.aniflow.domain.storage.model.StorageAvailability) : LibraryEvent
    data class DuplicateDetected(val comparison: DuplicateComparison) : LibraryEvent
    data class UpgradeAvailable(val episodeId: EpisodeId, val currentFile: LibraryFile, val candidateReleaseTitle: String) : LibraryEvent
}

/**
 * Library coverage model (Section 100).
 */
data class LibraryCoverage(
    val animeTitle: String,
    val seasonNumber: Int,
    val expectedEpisodes: Int,
    val availableEpisodes: Int,
    val upgradeableEpisodes: Int = 0,
    val missingEpisodeNumbers: List<Int> = emptyList()
) {
    val isComplete: Boolean get() = expectedEpisodes > 0 && availableEpisodes >= expectedEpisodes
    val coveragePercent: Float get() = if (expectedEpisodes > 0) (availableEpisodes.toFloat() / expectedEpisodes.toFloat()) * 100f else 0f
}
