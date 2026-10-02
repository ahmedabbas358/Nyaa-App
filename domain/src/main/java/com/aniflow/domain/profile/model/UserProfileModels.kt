package com.aniflow.domain.profile.model

import com.aniflow.domain.identity.ProfileId
import com.aniflow.domain.valueobject.AudioChannels
import com.aniflow.domain.valueobject.LanguageCode
import com.aniflow.domain.valueobject.MediaSource
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec
import java.time.Instant

const val PROFILE_SCHEMA_VERSION = 1
const val RULE_SCHEMA_VERSION = 1

/**
 * Root domain model for UserProfile in the AniFlow Control Plane (STEP 27).
 * Pure domain model with no Android Context dependencies.
 */
data class UserProfile(
    val id: ProfileId,
    val name: String,
    val description: String? = null,
    val enabled: Boolean = true,
    val isDefault: Boolean = false,
    val preferences: ProfilePreferences,
    val limits: ProfileLimits = ProfileLimits(),
    val networkPolicy: NetworkPolicy = NetworkPolicy(),
    val storagePolicy: StoragePolicy = StoragePolicy(),
    val downloadPolicy: DownloadPolicy = DownloadPolicy(),
    val automationPolicy: AutomationPolicy = AutomationPolicy(),
    val version: Int = 1,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(name.isNotBlank()) { "Profile name cannot be blank" }
    }
}

/**
 * Encapsulates all fine-grained preferences: Hard Constraints vs Soft Preferences (Section 7, 8).
 */
data class ProfilePreferences(
    val resolution: ResolutionPreference = ResolutionPreference(),
    val codec: CodecPreference = CodecPreference(),
    val audio: AudioPreference = AudioPreference(),
    val subtitles: SubtitlePreference = SubtitlePreference(),
    val source: SourcePreference = SourcePreference(),
    val size: SizePreference = SizePreference(),
    val releaseGroup: EntityPreference = EntityPreference(),
    val uploader: EntityPreference = EntityPreference(),
    val batch: BatchPreference = BatchPreference(),
    val consistency: ConsistencyPreference = ConsistencyPreference()
)

/**
 * Resolution preferences with clear separation of Required vs Forbidden vs Preferred (Section 9).
 */
data class ResolutionPreference(
    val preferred: Resolution? = Resolution.R1080p,
    val allowed: Set<Resolution> = setOf(Resolution.R1080p, Resolution.R720p),
    val required: Resolution? = null,
    val forbidden: Set<Resolution> = emptySet()
)

/**
 * Video codec preferences (Section 10).
 */
data class CodecPreference(
    val preferred: VideoCodec? = VideoCodec.HEVC,
    val allowed: Set<VideoCodec> = setOf(VideoCodec.HEVC, VideoCodec.AVC, VideoCodec.AV1),
    val required: VideoCodec? = null,
    val forbidden: Set<VideoCodec> = emptySet()
)

/**
 * Audio track & language preferences (Section 11).
 */
data class AudioPreference(
    val preferredLanguage: LanguageCode = LanguageCode.JAPANESE,
    val allowedLanguages: Set<LanguageCode> = setOf(LanguageCode.JAPANESE, LanguageCode.ENGLISH, LanguageCode.MULTI),
    val requiredLanguage: LanguageCode? = null,
    val forbiddenLanguages: Set<LanguageCode> = emptySet(),
    val channels: AudioChannels? = null,
    val requireLossless: Boolean = false,
    val dubPreference: DubPreference = DubPreference.OriginalJapanese
)

enum class DubPreference {
    OriginalJapanese,
    EnglishDub,
    ArabicDub,
    MultiAudio,
    Any
}

/**
 * Subtitle preferences (Section 12).
 */
data class SubtitlePreference(
    val requirement: SubtitleRequirement = SubtitleRequirement.Required,
    val preferredLanguages: List<LanguageCode> = listOf(LanguageCode.ENGLISH, LanguageCode.ARABIC),
    val allowedLanguages: Set<LanguageCode> = setOf(LanguageCode.ENGLISH, LanguageCode.ARABIC),
    val format: SubtitleFormatPreference = SubtitleFormatPreference.Any,
    val signsAndSongsOnly: Boolean = false
)

enum class SubtitleRequirement {
    Required,
    Preferred,
    Optional,
    Forbidden
}

enum class SubtitleFormatPreference {
    SoftOnly,
    HardcodedOnly,
    Any
}

/**
 * Media source preference (Section 13).
 */
data class SourcePreference(
    val preferred: MediaSource? = MediaSource.BluRay,
    val allowed: Set<MediaSource> = setOf(MediaSource.BluRay, MediaSource.WebRip, MediaSource.HDTV),
    val required: MediaSource? = null,
    val forbidden: Set<MediaSource> = emptySet(),
    val priorityList: List<MediaSource> = listOf(MediaSource.BluRay, MediaSource.WebRip, MediaSource.HDTV, MediaSource.DVD)
)

/**
 * File size preferences (Section 14).
 */
data class SizePreference(
    val minBytes: Long? = null,
    val maxBytes: Long? = null,
    val preferredRange: SizeRange? = null,
    val strategy: SizeStrategy = SizeStrategy.NoLimit,
    val weight: Double = 1.0
)

data class SizeRange(val minBytes: Long, val maxBytes: Long) {
    init {
        require(minBytes <= maxBytes) { "minBytes ($minBytes) cannot exceed maxBytes ($maxBytes)" }
    }
}

enum class SizeStrategy {
    NoLimit,
    PreferSmaller,
    PreferLarger,
    StrictLimit,
    TargetRange
}

/**
 * Entity preferences for Release Groups and Uploaders (Section 15).
 */
data class EntityPreference(
    val preferred: List<String> = emptyList(), // Ordered priority
    val allowed: Set<String> = emptySet(),
    val avoid: Set<String> = emptySet(),
    val forbidden: Set<String> = emptySet()
)

/**
 * Batch preferences (Section 16).
 */
data class BatchPreference(
    val mode: BatchMode = BatchMode.NoPreference,
    val scope: BatchScope = BatchScope.Any
)

enum class BatchMode {
    PreferBatch,
    RequireBatch,
    AvoidBatch,
    NoPreference
}

enum class BatchScope {
    CompleteSeason,
    PartialBatch,
    EpisodeRange,
    IndividualEpisodes,
    Any
}

/**
 * Consistency preferences across season / anime (Section 17).
 */
data class ConsistencyPreference(
    val preferSameGroup: Boolean = true,
    val preferSameUploader: Boolean = true,
    val preferSameCodec: Boolean = true,
    val preferSameResolution: Boolean = true,
    val preferSameSource: Boolean = true,
    val preferSameAudio: Boolean = true
)

/**
 * Profile resource limits (Section 2).
 */
data class ProfileLimits(
    val maxConcurrentDownloads: Int = 3,
    val maxBandwidthBytesPerSec: Long? = null,
    val maxDailyDownloadBytes: Long? = null,
    val minDiskFreeSpaceBytes: Long = 1024L * 1024L * 1024L // 1 GB
)

/**
 * Download execution policy (Section 18).
 */
data class DownloadPolicy(
    val maxConcurrentTasks: Int = 3,
    val maxConcurrentPerProfile: Int? = null,
    val allowCellular: Boolean = false,
    val allowMetered: Boolean = false,
    val allowRoaming: Boolean = false,
    val pauseOnLowStorage: Boolean = true,
    val autoRetry: Boolean = true,
    val maxRetryAttempts: Int = 3
)

/**
 * Network policy (Section 19).
 */
data class NetworkPolicy(
    val mode: NetworkPolicyMode = NetworkPolicyMode.WiFiOnly,
    val temporaryOverride: Boolean = false,
    val overrideExpiresAt: Instant? = null
)

enum class NetworkPolicyMode {
    WiFiOnly,
    AnyNetwork,
    MeteredAllowed,
    MeteredForbidden,
    RoamingAllowed,
    RoamingForbidden
}

/**
 * Storage management policy (Section 20).
 */
data class StoragePolicy(
    val minimumFreeSpaceBytes: Long = 2L * 1024L * 1024L * 1024L, // 2 GB
    val preferredStorageRoot: String? = null,
    val overflowStorageRoot: String? = null,
    val allowMultipleLocations: Boolean = true,
    val lowStorageAction: LowStorageAction = LowStorageAction.Pause
)

enum class LowStorageAction {
    Pause,
    Notify,
    RejectNewDownload,
    AllowWithWarning
}

/**
 * Automation behavioral policy (Section 21).
 */
data class AutomationPolicy(
    val automationEnabled: Boolean = true,
    val autoDownload: Boolean = false,
    val autoUpgrade: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val requireConfirmation: Boolean = false,
    val largeBatchConfirmation: Boolean = true,
    val lowConfidenceConfirmation: Boolean = true,
    val storageConflictConfirmation: Boolean = true
)

/**
 * Immutable snapshot of a resolved profile captured during execution (Section 33).
 */
data class ProfileSnapshot(
    val profileId: ProfileId,
    val profileName: String,
    val version: Int,
    val resolvedAt: Instant = Instant.now(),
    val preferences: ProfilePreferences,
    val policySummary: Map<String, String> = emptyMap()
)

/**
 * Pre-configured profile templates (Section 37).
 */
enum class ProfileTemplateType {
    Balanced,
    HighQuality,
    SmallSize,
    Archive,
    Mobile
}
