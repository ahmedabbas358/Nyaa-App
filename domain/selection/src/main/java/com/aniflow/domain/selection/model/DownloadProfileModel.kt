package com.aniflow.domain.selection.model

import com.aniflow.domain.identity.DownloadProfileId
import com.aniflow.domain.valueobject.BitDepth
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.LanguageCode
import com.aniflow.domain.valueobject.MediaSource
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.VideoCodec

enum class SelectionStrategy {
    QualityFirst,
    SizeFirst,
    UploaderFirst,
    AvailabilityFirst,
    ConsistencyFirst,
    Balanced,
    Manual
}

enum class ConsistencyPolicy {
    PerEpisode,
    SeasonConsistent,
    PreferSingleUploader,
    PreferSingleReleaseGroup,
    QualityConsistent
}

enum class FallbackMode {
    NoFallback,
    ControlledFallback,
    AnyCompatible
}

enum class SelectionPolicyType {
    BestCompatible,
    PreferredUploaderFirst,
    StrictProfile,
    MinimalSize,
    MaximumQuality,
    ManualOnly,
    PreferenceOnly
}

data class SelectionWeights(
    val resolution: Int = 30,
    val uploader: Int = 25,
    val releaseGroup: Int = 20,
    val codec: Int = 20,
    val subtitle: Int = 15,
    val audio: Int = 10,
    val source: Int = 10,
    val size: Int = 10,
    val availability: Int = 10,
    val consistency: Int = 10,
    val bitDepth: Int = 5,
    val hdr: Int = 5
) {
    fun validate() {
        require(resolution >= 0) { "resolution weight cannot be negative" }
        require(uploader >= 0) { "uploader weight cannot be negative" }
        require(releaseGroup >= 0) { "releaseGroup weight cannot be negative" }
        require(codec >= 0) { "codec weight cannot be negative" }
        require(subtitle >= 0) { "subtitle weight cannot be negative" }
        require(audio >= 0) { "audio weight cannot be negative" }
        require(source >= 0) { "source weight cannot be negative" }
        require(size >= 0) { "size weight cannot be negative" }
        require(availability >= 0) { "availability weight cannot be negative" }
    }

    companion object {
        val Default = SelectionWeights()
        val QualityFirst get() = QualityHeavy
        val SizeFirst get() = SizeHeavy
        val Balanced get() = Default

        val QualityHeavy = SelectionWeights(
            resolution = 40,
            codec = 30,
            source = 20,
            bitDepth = 15,
            hdr = 10,
            uploader = 15,
            subtitle = 15,
            audio = 15,
            size = 5,
            availability = 5
        )

        val SizeHeavy = SelectionWeights(
            size = 45,
            codec = 30,
            resolution = 20,
            uploader = 10,
            subtitle = 15,
            audio = 10,
            source = 5,
            availability = 10
        )

        val UploaderHeavy = SelectionWeights(
            uploader = 45,
            resolution = 25,
            codec = 20,
            subtitle = 15,
            audio = 10,
            source = 10,
            size = 10,
            availability = 10
        )
    }
}

/**
 * Complete strongly typed preferences model for candidate evaluation.
 */
data class UserSelectionPreferences(
    val resolution: ResolutionPreference = ResolutionPreference(),
    val codec: CodecPreference = CodecPreference(),
    val uploader: UploaderPreference = UploaderPreference(),
    val releaseGroup: ReleaseGroupPreference = ReleaseGroupPreference(),
    val subtitles: SubtitlePolicy = SubtitlePolicy(),
    val audioLanguage: LanguageRule = LanguageRule(primary = listOf(LanguageCode.JAPANESE)),
    val sizePolicy: SizePolicy = SizePolicy(),
    val seederPolicy: SeederPolicy = SeederPolicy(),
    val source: SourcePreference = SourcePreference(),
    val hdr: HdrPreference = HdrPreference(),
    val bitDepth: BitDepthPreference = BitDepthPreference(),
    val audioChannels: AudioChannelsPreference = AudioChannelsPreference(),
    val multiAudio: MultiAudioPreference = MultiAudioPreference.Any,
    val fallbackMode: FallbackMode = FallbackMode.ControlledFallback,
    val fallbackTiers: List<FallbackTier> = emptyList(),
    val consistencyPolicy: ConsistencyPolicy = ConsistencyPolicy.PerEpisode
)

/**
 * Rich domain DownloadProfile holding policy, constraints, weights, and preferences.
 */
data class DownloadProfile(
    val id: DownloadProfileId = DownloadProfileId(java.util.UUID.randomUUID().toString()),
    val name: String,
    val schemaVersion: Int = 1,
    val hardConstraints: List<HardConstraint> = emptyList(),
    val preferences: UserSelectionPreferences = UserSelectionPreferences(),
    val weights: SelectionWeights = SelectionWeights.Default,
    val policy: SelectionPolicyType = SelectionPolicyType.BestCompatible,
    val strategy: SelectionStrategy = SelectionStrategy.Balanced,
    val isDefault: Boolean = false
) {
    init {
        require(name.isNotBlank()) { "Profile name cannot be blank" }
        require(schemaVersion >= 1) { "schemaVersion must be >= 1" }
        weights.validate()
        preferences.sizePolicy.hardMaxBytes?.let { max ->
            preferences.sizePolicy.hardMinBytes?.let { min ->
                require(max >= min) { "Maximum size cannot be less than minimum size" }
            }
        }
        require(preferences.seederPolicy.minSeeders >= 0) { "Minimum seeders must be >= 0" }
    }

    fun duplicate(newName: String, newId: DownloadProfileId = DownloadProfileId(java.util.UUID.randomUUID().toString())): DownloadProfile =
        copy(
            id = newId,
            name = newName,
            isDefault = false
        )

    companion object {
        fun anime1080pArchive(id: String = "profile-archive-1080p"): DownloadProfile =
            DownloadProfile(
                id = DownloadProfileId(id),
                name = "Anime 1080p Archive",
                hardConstraints = listOf(
                    HardConstraint.RequireResolution(Resolution.R1080p),
                    HardConstraint.RequireAudioLanguage(LanguageCode.JAPANESE),
                    HardConstraint.RequireSubtitleLanguage(LanguageCode.ENGLISH),
                    HardConstraint.MaxFileSize(ByteSize.fromGigabytes(2.5)),
                    HardConstraint.MinSeeders(5)
                ),
                preferences = UserSelectionPreferences(
                    resolution = ResolutionPreference(ResolutionMatchMode.Exact, Resolution.R1080p),
                    codec = CodecPreference(preferredCodecs = listOf(VideoCodec.HEVC, VideoCodec.AV1, VideoCodec.AVC)),
                    bitDepth = BitDepthPreference(BitDepth.Bit10, isRequired = false),
                    source = SourcePreference(preferred = setOf(MediaSource.WebRip, MediaSource.BluRay)),
                    sizePolicy = SizePolicy.ofGigabytes(hardMaxGb = 2.5, preferredMaxGb = 2.0),
                    seederPolicy = SeederPolicy(minSeeders = 5, preferredSeeders = 15)
                ),
                weights = SelectionWeights.QualityHeavy,
                policy = SelectionPolicyType.BestCompatible,
                strategy = SelectionStrategy.QualityFirst
            )

        fun mobile720p(id: String = "profile-mobile-720p"): DownloadProfile =
            DownloadProfile(
                id = DownloadProfileId(id),
                name = "Mobile 720p",
                hardConstraints = listOf(
                    HardConstraint.RequireSubtitleLanguage(LanguageCode.ENGLISH),
                    HardConstraint.MaxFileSize(ByteSize.fromGigabytes(1.2))
                ),
                preferences = UserSelectionPreferences(
                    resolution = ResolutionPreference(ResolutionMatchMode.Prefer, Resolution.R720p),
                    codec = CodecPreference(
                        preferredCodecs = listOf(VideoCodec.AVC, VideoCodec.HEVC),
                        codecRanks = mapOf(VideoCodec.AVC to 100, VideoCodec.HEVC to 80, VideoCodec.AV1 to 50)
                    ),
                    sizePolicy = SizePolicy.ofGigabytes(hardMaxGb = 1.2, preferredMaxGb = 0.8),
                    seederPolicy = SeederPolicy(minSeeders = 1, preferredSeeders = 5)
                ),
                weights = SelectionWeights.SizeHeavy,
                policy = SelectionPolicyType.MinimalSize,
                strategy = SelectionStrategy.SizeFirst
            )

        fun balanced1080p(id: String = "profile-balanced-1080p"): DownloadProfile =
            DownloadProfile(
                id = DownloadProfileId(id),
                name = "Balanced 1080p",
                hardConstraints = listOf(
                    HardConstraint.RequireSubtitleLanguage(LanguageCode.ENGLISH),
                    HardConstraint.MinSeeders(3)
                ),
                preferences = UserSelectionPreferences(
                    resolution = ResolutionPreference(ResolutionMatchMode.Prefer, Resolution.R1080p),
                    codec = CodecPreference(),
                    sizePolicy = SizePolicy.ofGigabytes(hardMaxGb = 3.0, preferredMaxGb = 2.0)
                ),
                weights = SelectionWeights.Default,
                policy = SelectionPolicyType.BestCompatible,
                strategy = SelectionStrategy.Balanced,
                isDefault = true
            )
    }
}
