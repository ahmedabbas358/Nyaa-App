package com.aniflow.domain.search.model

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.CollectionId
import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseGroupId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.SavedSearchId
import com.aniflow.domain.identity.SearchHistoryId
import com.aniflow.domain.identity.SearchPresetId
import com.aniflow.domain.identity.SearchSessionId
import com.aniflow.domain.identity.SeasonId
import com.aniflow.domain.identity.UploaderId
import java.time.Instant

/**
 * SearchScope (Section 2).
 * Strongly-typed query boundaries avoiding ambiguous searches.
 */
sealed interface SearchScope {
    data object Universal : SearchScope
    data class Anime(val animeId: AnimeId? = null) : SearchScope
    data class Season(val seasonId: SeasonId? = null) : SearchScope
    data class Episode(val episodeId: EpisodeId? = null) : SearchScope
    data class Release(val releaseId: ReleaseId? = null) : SearchScope
    data class Uploader(val uploaderId: UploaderId? = null) : SearchScope
    data class ReleaseGroup(val groupId: ReleaseGroupId? = null) : SearchScope
    data object Library : SearchScope
    data object Downloads : SearchScope
    data class Collections(val collectionId: CollectionId? = null) : SearchScope
    data object SavedSearches : SearchScope
    data class Provider(val providerId: String) : SearchScope
}

/**
 * Context-Aware Search context (Section 22).
 */
data class SearchContext(
    val animeTitle: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Double? = null,
    val uploaderName: String? = null,
    val libraryLocation: String? = null
)

enum class SearchOperator {
    Equals,
    NotEquals,
    Contains,
    StartsWith,
    GreaterThan,
    GreaterThanOrEqual,
    LessThan,
    LessThanOrEqual,
    InList,
    NotInList
}

sealed interface SearchValue {
    data class Text(val value: String) : SearchValue
    data class Number(val value: Double) : SearchValue
    data class BooleanVal(val value: Boolean) : SearchValue
    data class StringList(val values: List<String>) : SearchValue
}

enum class FieldCapability {
    LocalOnly,
    ProviderOnly,
    Hybrid
}

enum class SearchField(val capability: FieldCapability) {
    // Identity
    Title(FieldCapability.Hybrid),
    AlternativeTitle(FieldCapability.LocalOnly),
    Anime(FieldCapability.Hybrid),
    Season(FieldCapability.Hybrid),
    Episode(FieldCapability.Hybrid),
    Year(FieldCapability.Hybrid),

    // Release
    Uploader(FieldCapability.Hybrid),
    ReleaseGroup(FieldCapability.Hybrid),
    Provider(FieldCapability.Hybrid),
    ReleaseName(FieldCapability.Hybrid),
    Batch(FieldCapability.Hybrid),
    Movie(FieldCapability.Hybrid),
    Special(FieldCapability.Hybrid),

    // Technical
    Resolution(FieldCapability.Hybrid),
    Codec(FieldCapability.Hybrid),
    BitDepth(FieldCapability.Hybrid),
    HDR(FieldCapability.Hybrid),
    Source(FieldCapability.Hybrid),
    Container(FieldCapability.Hybrid),
    Audio(FieldCapability.Hybrid),
    Subtitle(FieldCapability.Hybrid),
    Language(FieldCapability.Hybrid),

    // Availability
    Size(FieldCapability.Hybrid),
    Seeds(FieldCapability.ProviderOnly),
    Peers(FieldCapability.ProviderOnly),
    Date(FieldCapability.Hybrid),

    // Local State
    InLibrary(FieldCapability.LocalOnly),
    Downloaded(FieldCapability.LocalOnly),
    Downloading(FieldCapability.LocalOnly),
    Queued(FieldCapability.LocalOnly),
    Missing(FieldCapability.LocalOnly),
    Favorite(FieldCapability.LocalOnly),
    Upgradeable(FieldCapability.LocalOnly),
    ReviewRequired(FieldCapability.LocalOnly)
}

/**
 * Structured Search Expression (Section 3, 4).
 * Boolean AST representing complex queries (AND, OR, NOT, Groups).
 */
sealed interface SearchExpression {

    data class Condition(
        val field: SearchField,
        val operator: SearchOperator,
        val value: SearchValue
    ) : SearchExpression {
        fun toQuerySnippet(): String {
            val valStr = when (value) {
                is SearchValue.Text -> "\"${value.value}\""
                is SearchValue.Number -> value.value.toString()
                is SearchValue.BooleanVal -> value.value.toString()
                is SearchValue.StringList -> "[${value.values.joinToString()}]"
            }
            return "${field.name} ${operator.name} $valStr"
        }
    }

    data class And(val children: List<SearchExpression>) : SearchExpression
    data class Or(val children: List<SearchExpression>) : SearchExpression
    data class Not(val child: SearchExpression) : SearchExpression

    fun toQueryString(): String {
        return when (this) {
            is Condition -> toQuerySnippet()
            is And -> children.joinToString(" AND ", prefix = "(", postfix = ")") { it.toQueryString() }
            is Or -> children.joinToString(" OR ", prefix = "(", postfix = ")") { it.toQueryString() }
            is Not -> "NOT (${child.toQueryString()})"
        }
    }
}

/**
 * SearchQuery (Section 2).
 */
data class SearchQuery(
    val rawText: String,
    val normalizedText: String,
    val expression: SearchExpression? = null,
    val scope: SearchScope = SearchScope.Universal,
    val context: SearchContext? = null
)

enum class SearchSource {
    Local,
    Provider,
    Hybrid
}

/**
 * SearchResultItem (Section 16).
 * Unified polymorphic result item covering all app entities.
 */
sealed interface SearchResultItem {
    val id: String
    val title: String
    val source: SearchSource
    val relevanceScore: Int
    val matchedReasons: List<String>

    data class AnimeResult(
        override val id: String,
        override val title: String,
        override val source: SearchSource = SearchSource.Local,
        override val relevanceScore: Int = 100,
        override val matchedReasons: List<String> = emptyList(),
        val totalEpisodes: Int = 0,
        val coveredEpisodes: Int = 0,
        val posterUrl: String? = null
    ) : SearchResultItem

    data class GroupedAnimeResult(
        override val id: String,
        override val title: String,
        override val source: SearchSource = SearchSource.Hybrid,
        override val relevanceScore: Int = 98,
        override val matchedReasons: List<String> = emptyList(),
        val seasonNumber: Int = 1,
        val totalEpisodes: Int = 0,
        val episodeNumbers: List<Int> = emptyList(),
        val availableUploaders: List<String> = emptyList(),
        val availableResolutions: List<String> = emptyList(),
        val totalSizeBytes: Long = 0L,
        val formattedSize: String = "0 B",
        val releaseIds: List<String> = emptyList(),
        val releases: List<ReleaseResult> = emptyList(),
        val posterUrl: String? = null
    ) : SearchResultItem

    data class SeasonResult(
        override val id: String,
        override val title: String,
        override val source: SearchSource = SearchSource.Local,
        override val relevanceScore: Int = 90,
        override val matchedReasons: List<String> = emptyList(),
        val animeTitle: String,
        val seasonNumber: Int,
        val episodeCount: Int
    ) : SearchResultItem

    data class EpisodeResult(
        override val id: String,
        override val title: String,
        override val source: SearchSource = SearchSource.Local,
        override val relevanceScore: Int = 85,
        override val matchedReasons: List<String> = emptyList(),
        val animeTitle: String,
        val episodeNumber: Double,
        val isInLibrary: Boolean = false
    ) : SearchResultItem

    data class ReleaseResult(
        override val id: String,
        override val title: String,
        override val source: SearchSource = SearchSource.Provider,
        override val relevanceScore: Int = 80,
        override val matchedReasons: List<String> = emptyList(),
        val animeTitle: String? = null,
        val resolution: String = "1080p",
        val codec: String = "HEVC",
        val uploader: String = "",
        val releaseGroup: String? = null,
        val sizeFormatted: String = "0 B",
        val seeders: Int = 0,
        val leechers: Int = 0,
        val magnetUri: String? = null,
        val isBatch: Boolean = false,
        val isDownloaded: Boolean = false,
        val isDownloading: Boolean = false,
        val isKnownSeen: Boolean = false
    ) : SearchResultItem

    data class UploaderResult(
        override val id: String,
        override val title: String,
        override val source: SearchSource = SearchSource.Local,
        override val relevanceScore: Int = 70,
        override val matchedReasons: List<String> = emptyList(),
        val totalUploadsCount: Int = 0
    ) : SearchResultItem

    data class ReleaseGroupResult(
        override val id: String,
        override val title: String,
        override val source: SearchSource = SearchSource.Local,
        override val relevanceScore: Int = 70,
        override val matchedReasons: List<String> = emptyList(),
        val totalReleasesCount: Int = 0
    ) : SearchResultItem

    data class LibraryResult(
        override val id: String,
        override val title: String,
        override val source: SearchSource = SearchSource.Local,
        override val relevanceScore: Int = 95,
        override val matchedReasons: List<String> = emptyList(),
        val filePath: String,
        val sizeFormatted: String
    ) : SearchResultItem

    data class DownloadResult(
        override val id: String,
        override val title: String,
        override val source: SearchSource = SearchSource.Local,
        override val relevanceScore: Int = 90,
        override val matchedReasons: List<String> = emptyList(),
        val taskState: String,
        val progressPercent: Int
    ) : SearchResultItem

    data class CollectionResult(
        override val id: String,
        override val title: String,
        override val source: SearchSource = SearchSource.Local,
        override val relevanceScore: Int = 75,
        override val matchedReasons: List<String> = emptyList(),
        val itemCount: Int
    ) : SearchResultItem

    data class SavedSearchResult(
        override val id: String,
        override val title: String,
        override val source: SearchSource = SearchSource.Local,
        override val relevanceScore: Int = 85,
        override val matchedReasons: List<String> = emptyList(),
        val queryText: String
    ) : SearchResultItem
}

enum class SearchSuggestionType {
    RecentSearch,
    PopularEntity,
    AnimeTitle,
    Uploader,
    ReleaseGroup,
    SavedSearch,
    FilterSuggestion
}

data class SearchSuggestion(
    val id: String,
    val text: String,
    val type: SearchSuggestionType,
    val subtitle: String? = null,
    val score: Int = 0,
    val isPinned: Boolean = false
)

data class SearchHistoryEntry(
    val id: SearchHistoryId,
    val query: String,
    val scope: SearchScope = SearchScope.Universal,
    val filtersSummary: String? = null,
    val timestamp: Instant = Instant.now(),
    val resultCount: Int = 0,
    val isPinned: Boolean = false
)

data class SearchPreset(
    val id: SearchPresetId,
    val name: String,
    val resolution: String? = null,
    val codec: String? = null,
    val audioLanguage: String? = null,
    val subtitleLanguage: String? = null,
    val isBatchOnly: Boolean = false,
    val trustedOnly: Boolean = false,
    val minSeeds: Int? = null,
    val description: String = ""
)

data class SearchSession(
    val id: SearchSessionId,
    val query: SearchQuery,
    val sortField: String = "Seeds",
    val sortAscending: Boolean = false,
    val currentPage: Int = 1,
    val timestamp: Instant = Instant.now()
)

data class SearchSourceState(
    val source: SearchSource,
    val isAvailable: Boolean,
    val resultCount: Int,
    val latencyMs: Long = 0L,
    val isCached: Boolean = false,
    val errorMessage: String? = null
)

data class SearchResponse(
    val results: List<SearchResultItem>,
    val suggestions: List<SearchSuggestion> = emptyList(),
    val sourceStates: List<SearchSourceState> = emptyList(),
    val hasMore: Boolean = false,
    val session: SearchSession
)

/**
 * Release Comparison model (Section 32).
 */
data class ReleaseComparisonItem(
    val releaseId: ReleaseId,
    val title: String,
    val animeTitle: String,
    val resolution: String,
    val codec: String,
    val audio: String,
    val subtitles: String,
    val source: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val seeds: Int,
    val peers: Int,
    val uploader: String,
    val releaseGroup: String?,
    val isBatch: Boolean,
    val provider: String,
    val inLibrary: Boolean = false,
    val isDownloading: Boolean = false
)

data class ComparisonDifference(
    val attributeName: String,
    val valuesByReleaseId: Map<String, String>,
    val hasDifference: Boolean
)

data class ReleaseComparisonResult(
    val items: List<ReleaseComparisonItem>,
    val differences: List<ComparisonDifference>
)
