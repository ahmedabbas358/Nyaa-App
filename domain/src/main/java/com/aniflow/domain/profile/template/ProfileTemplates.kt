package com.aniflow.domain.profile.template

import com.aniflow.domain.identity.ProfileId
import com.aniflow.domain.profile.model.AudioPreference
import com.aniflow.domain.profile.model.AutomationPolicy
import com.aniflow.domain.profile.model.BatchMode
import com.aniflow.domain.profile.model.BatchPreference
import com.aniflow.domain.profile.model.CodecPreference
import com.aniflow.domain.profile.model.ConsistencyPreference
import com.aniflow.domain.profile.model.DownloadPolicy
import com.aniflow.domain.profile.model.DubPreference
import com.aniflow.domain.profile.model.EntityPreference
import com.aniflow.domain.profile.model.NetworkPolicy
import com.aniflow.domain.profile.model.NetworkPolicyMode
import com.aniflow.domain.profile.model.ProfileLimits
import com.aniflow.domain.profile.model.ProfilePreferences
import com.aniflow.domain.profile.model.ProfileTemplateType
import com.aniflow.domain.profile.model.ResolutionPreference
import com.aniflow.domain.profile.model.SizePreference
import com.aniflow.domain.profile.model.SizeRange
import com.aniflow.domain.profile.model.SizeStrategy
import com.aniflow.domain.profile.model.SourcePreference
import com.aniflow.domain.profile.model.StoragePolicy
import com.aniflow.domain.profile.model.SubtitleFormatPreference
import com.aniflow.domain.profile.model.SubtitlePreference
import com.aniflow.domain.profile.model.SubtitleRequirement
import com.aniflow.domain.profile.model.UserProfile
import com.aniflow.domain.valueobject.LanguageCode
import com.aniflow.domain.valueobject.MediaSource
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec
import java.util.UUID

/**
 * Pre-defined Profile Templates (Section 37).
 * Templates serve as blueprints for generating UserProfiles without baking static profiles into the system.
 */
object ProfileTemplates {

    fun createFromTemplate(
        type: ProfileTemplateType,
        customId: ProfileId = ProfileId("prof_${UUID.randomUUID()}"),
        isDefault: Boolean = false
    ): UserProfile {
        return when (type) {
            ProfileTemplateType.Balanced -> createBalanced(customId, isDefault)
            ProfileTemplateType.HighQuality -> createHighQuality(customId, isDefault)
            ProfileTemplateType.SmallSize -> createSmallSize(customId, isDefault)
            ProfileTemplateType.Archive -> createArchive(customId, isDefault)
            ProfileTemplateType.Mobile -> createMobile(customId, isDefault)
        }
    }

    fun createBalanced(id: ProfileId, isDefault: Boolean = true): UserProfile {
        return UserProfile(
            id = id,
            name = "Balanced",
            description = "Great balance between video quality and file size (1080p HEVC, 500MB-2.5GB).",
            enabled = true,
            isDefault = isDefault,
            preferences = ProfilePreferences(
                resolution = ResolutionPreference(
                    preferred = Resolution.R1080p,
                    allowed = setOf(Resolution.R1080p, Resolution.R720p)
                ),
                codec = CodecPreference(
                    preferred = VideoCodec.HEVC,
                    allowed = setOf(VideoCodec.HEVC, VideoCodec.AVC, VideoCodec.AV1)
                ),
                audio = AudioPreference(
                    preferredLanguage = LanguageCode.JAPANESE,
                    dubPreference = DubPreference.OriginalJapanese
                ),
                subtitles = SubtitlePreference(
                    requirement = SubtitleRequirement.Required,
                    preferredLanguages = listOf(LanguageCode.ENGLISH, LanguageCode.ARABIC)
                ),
                source = SourcePreference(
                    preferred = MediaSource.WebRip,
                    allowed = setOf(MediaSource.BluRay, MediaSource.WebRip, MediaSource.HDTV)
                ),
                size = SizePreference(
                    preferredRange = SizeRange(500L * 1024L * 1024L, 2500L * 1024L * 1024L),
                    strategy = SizeStrategy.PreferSmaller,
                    weight = 1.0
                ),
                batch = BatchPreference(mode = BatchMode.NoPreference),
                consistency = ConsistencyPreference()
            ),
            limits = ProfileLimits(maxConcurrentDownloads = 3),
            networkPolicy = NetworkPolicy(mode = NetworkPolicyMode.WiFiOnly),
            downloadPolicy = DownloadPolicy(maxConcurrentTasks = 3, autoRetry = true)
        )
    }

    fun createHighQuality(id: ProfileId, isDefault: Boolean = false): UserProfile {
        return UserProfile(
            id = id,
            name = "High Quality",
            description = "Maximum fidelity prioritizing 1080p/4K BluRay releases with lossless audio.",
            enabled = true,
            isDefault = isDefault,
            preferences = ProfilePreferences(
                resolution = ResolutionPreference(
                    preferred = Resolution.R1080p,
                    allowed = setOf(Resolution.R1080p, Resolution.R1440p, Resolution.R2160p)
                ),
                codec = CodecPreference(
                    preferred = VideoCodec.HEVC,
                    allowed = setOf(VideoCodec.HEVC, VideoCodec.AV1, VideoCodec.AVC)
                ),
                audio = AudioPreference(
                    preferredLanguage = LanguageCode.JAPANESE,
                    requireLossless = true,
                    dubPreference = DubPreference.MultiAudio
                ),
                subtitles = SubtitlePreference(
                    requirement = SubtitleRequirement.Required,
                    format = SubtitleFormatPreference.SoftOnly
                ),
                source = SourcePreference(
                    preferred = MediaSource.BluRay,
                    allowed = setOf(MediaSource.BluRay, MediaSource.WebRip),
                    priorityList = listOf(MediaSource.BluRay, MediaSource.WebRip)
                ),
                size = SizePreference(
                    strategy = SizeStrategy.NoLimit,
                    weight = 0.5
                ),
                consistency = ConsistencyPreference(preferSameGroup = true, preferSameSource = true)
            ),
            networkPolicy = NetworkPolicy(mode = NetworkPolicyMode.WiFiOnly)
        )
    }

    fun createSmallSize(id: ProfileId, isDefault: Boolean = false): UserProfile {
        return UserProfile(
            id = id,
            name = "Small Size",
            description = "Compact encodes targeting minimal storage consumption (720p/Mini-1080p, < 700MB).",
            enabled = true,
            isDefault = isDefault,
            preferences = ProfilePreferences(
                resolution = ResolutionPreference(
                    preferred = Resolution.R720p,
                    allowed = setOf(Resolution.R720p, Resolution.R1080p, Resolution.R576p)
                ),
                codec = CodecPreference(
                    preferred = VideoCodec.HEVC,
                    allowed = setOf(VideoCodec.HEVC, VideoCodec.AV1)
                ),
                size = SizePreference(
                    maxBytes = 750L * 1024L * 1024L,
                    strategy = SizeStrategy.PreferSmaller,
                    weight = 2.0
                )
            ),
            networkPolicy = NetworkPolicy(mode = NetworkPolicyMode.AnyNetwork)
        )
    }

    fun createArchive(id: ProfileId, isDefault: Boolean = false): UserProfile {
        return UserProfile(
            id = id,
            name = "Archive",
            description = "Permanent collections: Full season batches, 1080p BluRay, strictly consistent.",
            enabled = true,
            isDefault = isDefault,
            preferences = ProfilePreferences(
                resolution = ResolutionPreference(
                    preferred = Resolution.R1080p,
                    required = Resolution.R1080p
                ),
                codec = CodecPreference(
                    preferred = VideoCodec.HEVC,
                    allowed = setOf(VideoCodec.HEVC, VideoCodec.AVC)
                ),
                source = SourcePreference(
                    preferred = MediaSource.BluRay,
                    required = MediaSource.BluRay
                ),
                batch = BatchPreference(mode = BatchMode.RequireBatch),
                consistency = ConsistencyPreference(
                    preferSameGroup = true,
                    preferSameUploader = true,
                    preferSameCodec = true,
                    preferSameResolution = true,
                    preferSameSource = true
                )
            ),
            storagePolicy = StoragePolicy(minimumFreeSpaceBytes = 10L * 1024L * 1024L * 1024L) // 10 GB
        )
    }

    fun createMobile(id: ProfileId, isDefault: Boolean = false): UserProfile {
        return UserProfile(
            id = id,
            name = "Mobile",
            description = "Optimized for on-the-go devices with limited battery, storage, and metered data.",
            enabled = true,
            isDefault = isDefault,
            preferences = ProfilePreferences(
                resolution = ResolutionPreference(
                    preferred = Resolution.R720p,
                    allowed = setOf(Resolution.R720p, Resolution.R480p)
                ),
                size = SizePreference(
                    maxBytes = 500L * 1024L * 1024L,
                    strategy = SizeStrategy.PreferSmaller
                )
            ),
            networkPolicy = NetworkPolicy(mode = NetworkPolicyMode.WiFiOnly),
            downloadPolicy = DownloadPolicy(maxConcurrentTasks = 1, allowCellular = false, pauseOnLowStorage = true)
        )
    }
}
