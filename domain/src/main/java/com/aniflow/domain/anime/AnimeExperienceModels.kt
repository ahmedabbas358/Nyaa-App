package com.aniflow.domain.anime

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.AnimeTitleId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.SeasonId
import com.aniflow.domain.valueobject.EpisodeNumber
import com.aniflow.domain.valueobject.LanguageCode
import java.time.Instant

/**
 * Step 20 — Anime Status (Section 4).
 * Reflects genuine lifecycle state without guessing from isolated search results.
 */
enum class AnimeStatus {
    Unknown,
    Ongoing,
    Completed,
    Hiatus,
    Cancelled
}

/**
 * Step 20 — Media Type (Section 5).
 * Distinguishes series, movies, and specials clearly.
 */
enum class MediaType {
    Series,
    Movie,
    Special,
    OVA,
    ONA,
    Unknown
}

/**
 * Step 20 — Anime Title Types and Sources (Section 6).
 */
enum class AnimeTitleType {
    Canonical,
    Official,
    Alias,
    Native,
    Romanized,
    UserDefined,
    ProviderObserved
}

enum class TitleSource {
    Provider,
    LibraryScan,
    UserOverride,
    ExternalMetadata,
    Unknown
}

/**
 * Step 20 — Anime Title Model (Section 6).
 */
data class AnimeTitle(
    val id: AnimeTitleId,
    val animeId: AnimeId,
    val value: String,
    val normalizedValue: String,
    val language: LanguageCode? = null,
    val type: AnimeTitleType = AnimeTitleType.Official,
    val source: TitleSource = TitleSource.Provider
)

/**
 * Step 20 — Anime Entity (Section 3).
 * Decoupled aggregate root that does NOT embed all releases or files directly.
 */
data class Anime(
    val id: AnimeId,
    val canonicalTitle: String,
    val titles: List<AnimeTitle> = emptyList(),
    val year: Int? = null,
    val mediaType: MediaType = MediaType.Series,
    val status: AnimeStatus = AnimeStatus.Unknown,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(canonicalTitle.isNotBlank()) { "Anime canonicalTitle cannot be blank" }
    }

    val displayYear: String get() = year?.toString() ?: "Unknown Year"
}

/**
 * Step 20 — Season Type & Source (Section 10, 11).
 */
enum class SeasonType {
    Regular,
    Part,
    Cour,
    Special,
    Unknown
}

enum class SeasonSource {
    Discovered,
    UserDefined,
    Imported
}

/**
 * Step 20 — Season Entity (Section 10, 11, 12).
 * Allows number to be null when release does not specify season (e.g. One Piece E1050).
 */
data class Season(
    val id: SeasonId,
    val animeId: AnimeId,
    val number: Int?,
    val title: String? = null,
    val type: SeasonType = SeasonType.Regular,
    val expectedEpisodeCount: Int? = null,
    val source: SeasonSource = SeasonSource.Discovered
) {
    val displayTitle: String get() = title ?: if (number != null) "Season $number" else "Season (Unknown)"
}

/**
 * Step 20 — Episode Type & Expectation Source (Section 14, 16, 17).
 */
enum class EpisodeType {
    Regular,
    Special,
    OVA,
    OAD,
    ONA,
    Unknown
}

enum class EpisodeExpectationSource {
    User,
    ProviderMetadata,
    ImportedMetadata,
    KnownSeasonStructure,
    Unknown
}

/**
 * Step 20 — Episode Entity (Section 13, 14, 15).
 * Supports both seasonal episode number and absolute numbering (e.g. S02E03, absolute 27).
 */
data class Episode(
    val id: EpisodeId,
    val animeId: AnimeId,
    val seasonId: SeasonId?,
    val number: EpisodeNumber?,
    val absoluteNumber: Int? = null,
    val type: EpisodeType = EpisodeType.Regular,
    val title: String? = null,
    val expected: Boolean = false,
    val expectationSource: EpisodeExpectationSource = EpisodeExpectationSource.Unknown,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    val displayNumber: String get() = number?.displayString ?: absoluteNumber?.let { "#$it" } ?: "Special"
}

/**
 * Step 20 — ReleaseEpisode Relation (Section 25, 26, 27, 28).
 */
enum class ReleaseEpisodeRelation {
    Primary,
    Contained,
    Possible,
    Inferred,
    UserMapped
}

data class ReleaseEpisode(
    val releaseId: ReleaseId,
    val episodeId: EpisodeId,
    val relation: ReleaseEpisodeRelation = ReleaseEpisodeRelation.Primary,
    val confidence: Float = 1.0f
)

/**
 * Step 20 — User Mapping Persistence (Section 64, 66).
 */
enum class MappingScope {
    ThisRelease,
    ThisAnime,
    ThisProvider,
    Global
}

data class UserMapping(
    val id: String,
    val originalReleaseId: ReleaseId?,
    val titleSignature: String,
    val field: String,
    val oldValue: String?,
    val newValue: String,
    val scope: MappingScope = MappingScope.ThisAnime,
    val createdAt: Instant = Instant.now()
)

/**
 * Step 20 — Review Queue (Section 64, 65).
 */
enum class ReviewIssue {
    AmbiguousAnime,
    AmbiguousSeason,
    AmbiguousEpisode,
    ConflictingMetadata,
    UnknownMediaType,
    PossibleBatch
}

enum class ReviewStatus {
    Pending,
    Confirmed,
    Edited,
    Ignored
}

data class ReviewQueueItem(
    val id: String,
    val releaseId: ReleaseId,
    val rawTitle: String,
    val detectedAnime: String?,
    val detectedSeason: Int?,
    val detectedEpisode: Int?,
    val issue: ReviewIssue,
    val candidateAnimeList: List<Anime> = emptyList(),
    val status: ReviewStatus = ReviewStatus.Pending
)

/**
 * Step 20 — Anime Experience Repository Contract (Section 67, 68).
 * Manages clean domain entities for Anime, Seasons, Episodes, and ReleaseEpisode mappings.
 */
interface AnimeExperienceRepository {
    suspend fun getAnime(id: AnimeId): Anime?
    fun observeAnime(id: AnimeId): kotlinx.coroutines.flow.Flow<Anime?>
    suspend fun findAnimeByTitle(title: String): Anime?
    fun observeAllAnime(): kotlinx.coroutines.flow.Flow<List<Anime>>
    suspend fun saveAnime(anime: Anime)
    suspend fun deleteAnime(id: AnimeId)

    suspend fun getSeasons(animeId: AnimeId): List<Season>
    fun observeSeasons(animeId: AnimeId): kotlinx.coroutines.flow.Flow<List<Season>>
    suspend fun getSeason(id: SeasonId): Season?
    fun observeSeason(id: SeasonId): kotlinx.coroutines.flow.Flow<Season?>
    suspend fun saveSeason(season: Season)
    suspend fun deleteSeason(id: SeasonId)

    suspend fun getEpisodes(seasonId: SeasonId): List<Episode>
    fun observeEpisodes(seasonId: SeasonId): kotlinx.coroutines.flow.Flow<List<Episode>>
    suspend fun getEpisode(id: EpisodeId): Episode?
    fun observeEpisode(id: EpisodeId): kotlinx.coroutines.flow.Flow<Episode?>
    suspend fun getEpisodesByAnimeId(animeId: AnimeId): List<Episode>
    suspend fun saveEpisode(episode: Episode)
    suspend fun deleteEpisode(id: EpisodeId)

    suspend fun getReleasesForEpisode(episodeId: EpisodeId): List<ReleaseEpisode>
    suspend fun linkRelease(mapping: ReleaseEpisode)
    suspend fun unlinkRelease(releaseId: ReleaseId, episodeId: EpisodeId)
}

