package com.aniflow.domain.intelligence.grouping

import com.aniflow.domain.intelligence.model.EpisodeCoverage
import com.aniflow.domain.intelligence.model.EpisodeCoverageStatus
import com.aniflow.domain.intelligence.model.EpisodeReleaseGroup
import com.aniflow.domain.intelligence.model.NormalizedRelease

/**
 * Grouping modes determining the hierarchy of media grouping (Section 47, 57, 102).
 */
enum class IntelligenceGroupingMode {
    Anime,
    AnimeSeason,
    AnimeUploader,
    AnimeReleaseGroup,
    AnimeTechnical,
    AnimeUploaderTechnical,
    Custom
}

/**
 * Field dimensions for custom grouping rules (Section 103).
 */
enum class GroupingField {
    Anime,
    Season,
    Uploader,
    ReleaseGroup,
    Resolution,
    Codec,
    Source
}

data class GroupingRule(
    val fields: List<GroupingField> = listOf(GroupingField.Anime, GroupingField.Season)
)

/**
 * Group summary metrics computed on-demand (Section 58, 104).
 */
data class GroupSummary(
    val releaseCount: Int,
    val coveredEpisodeCount: Int,
    val uploaderCount: Int,
    val releaseGroupCount: Int,
    val qualityRange: String,
    val sizeRange: String
)

/**
 * Media group container produced by ReleaseGroupingEngine (Section 48, 58, 104).
 */
data class ReleaseGroupResult(
    val groupKey: String,
    val displayName: String,
    val animeTitle: String,
    val seasonNumber: Int?,
    val episodeGroups: List<EpisodeReleaseGroup>,
    val releases: List<NormalizedRelease>,
    val isAmbiguousGroup: Boolean = false
) {
    val summary: GroupSummary by lazy {
        val uniqueUploaders = releases.mapNotNull { it.uploader?.name }.distinct().size
        val uniqueGroups = releases.mapNotNull { it.releaseGroup?.name }.distinct().size
        val resolutions = releases.mapNotNull { it.technical.resolution?.displayName }.distinct()
        val qualityStr = if (resolutions.isEmpty()) "Unknown" else resolutions.joinToString(", ")
        val coveredEpisodes = episodeGroups.filter { it.episodeNumber > 0 }.size

        GroupSummary(
            releaseCount = releases.size,
            coveredEpisodeCount = coveredEpisodes,
            uploaderCount = uniqueUploaders,
            releaseGroupCount = uniqueGroups,
            qualityRange = qualityStr,
            sizeRange = ""
        )
    }
}

/**
 * Result container encapsulating grouped media, unmatched, and ambiguous releases (Section 48, 55).
 */
data class GroupingResult(
    val groups: List<ReleaseGroupResult>,
    val unmatched: List<NormalizedRelease> = emptyList(),
    val ambiguous: List<NormalizedRelease> = emptyList(),
    val totalReleasesProcessed: Int = 0
)

/**
 * High-performance O(N) Release Grouping Engine (Section 47, 48, 49, 102, 103, 104, 105).
 * Retains all candidate releases per episode without deleting or dropping alternatives.
 */
class ReleaseGroupingEngine(
    private val defaultMode: IntelligenceGroupingMode = IntelligenceGroupingMode.AnimeSeason
) {

    fun group(
        releases: List<NormalizedRelease>,
        mode: IntelligenceGroupingMode = defaultMode,
        customRule: GroupingRule = GroupingRule()
    ): GroupingResult {
        if (releases.isEmpty()) {
            return GroupingResult(emptyList())
        }

        val buckets = LinkedHashMap<String, MutableList<NormalizedRelease>>()
        val unmatched = mutableListOf<NormalizedRelease>()
        val ambiguous = mutableListOf<NormalizedRelease>()

        // 1. Grouping into buckets in O(N)
        for (release in releases) {
            if (release.animeCandidate.isNullOrBlank()) {
                unmatched.add(release)
                continue
            }
            if (release.confidence.needsManualReview) {
                ambiguous.add(release)
            }

            val key = buildGroupKey(release, mode, customRule)
            buckets.getOrPut(key) { mutableListOf() }.add(release)
        }

        // 2. Build structured EpisodeReleaseGroup hierarchy per bucket
        val results = mutableListOf<ReleaseGroupResult>()
        for ((groupKey, groupReleases) in buckets) {
            val sample = groupReleases.first()
            val anime = sample.animeCandidate ?: "Unknown Anime"
            val season = sample.seasonCandidate ?: 1

            val episodeMap = LinkedHashMap<Int, MutableList<NormalizedRelease>>()

            for (rel in groupReleases) {
                when (val coverage = rel.episodes) {
                    is EpisodeCoverage.Single -> {
                        episodeMap.getOrPut(coverage.episode) { mutableListOf() }.add(rel)
                    }
                    is EpisodeCoverage.Range -> {
                        for (ep in coverage.from..coverage.to) {
                            episodeMap.getOrPut(ep) { mutableListOf() }.add(rel)
                        }
                    }
                    is EpisodeCoverage.Set -> {
                        for (ep in coverage.episodes) {
                            episodeMap.getOrPut(ep) { mutableListOf() }.add(rel)
                        }
                    }
                    is EpisodeCoverage.Season -> {
                        episodeMap.getOrPut(0) { mutableListOf() }.add(rel)
                    }
                    is EpisodeCoverage.Series -> {
                        episodeMap.getOrPut(0) { mutableListOf() }.add(rel)
                    }
                    is EpisodeCoverage.Unknown -> {
                        episodeMap.getOrPut(0) { mutableListOf() }.add(rel)
                    }
                }
            }

            val episodeGroups = episodeMap.toSortedMap().map { (epNum, candidates) ->
                EpisodeReleaseGroup(
                    episodeNumber = epNum,
                    candidates = candidates,
                    selectedRelease = null, // Selection is performed downstream in Step 7
                    coverageStatus = EpisodeCoverageStatus.Available
                )
            }

            val isGroupAmbiguous = groupReleases.any { it.confidence.needsManualReview }
            val displayName = buildDisplayName(sample, mode)

            results.add(
                ReleaseGroupResult(
                    groupKey = groupKey,
                    displayName = displayName,
                    animeTitle = anime,
                    seasonNumber = season,
                    episodeGroups = episodeGroups,
                    releases = groupReleases,
                    isAmbiguousGroup = isGroupAmbiguous
                )
            )
        }

        return GroupingResult(
            groups = results,
            unmatched = unmatched,
            ambiguous = ambiguous,
            totalReleasesProcessed = releases.size
        )
    }

    private fun buildGroupKey(
        release: NormalizedRelease,
        mode: IntelligenceGroupingMode,
        rule: GroupingRule
    ): String {
        val anime = release.animeCandidate?.lowercase() ?: "unknown"
        val season = release.seasonCandidate ?: 1
        val uploader = release.uploader?.name?.lowercase() ?: "anonymous"
        val group = release.releaseGroup?.name?.lowercase() ?: "nogroup"
        val res = release.technical.resolution?.displayName ?: "anyres"
        val codec = release.technical.codec?.displayName ?: "anycodec"

        return when (mode) {
            IntelligenceGroupingMode.Anime -> anime
            IntelligenceGroupingMode.AnimeSeason -> "$anime|s$season"
            IntelligenceGroupingMode.AnimeUploader -> "$anime|u:$uploader"
            IntelligenceGroupingMode.AnimeReleaseGroup -> "$anime|g:$group"
            IntelligenceGroupingMode.AnimeTechnical -> "$anime|s$season|$res|$codec"
            IntelligenceGroupingMode.AnimeUploaderTechnical -> "$anime|u:$uploader|$res"
            IntelligenceGroupingMode.Custom -> {
                rule.fields.joinToString("|") { field ->
                    when (field) {
                        GroupingField.Anime -> anime
                        GroupingField.Season -> "s$season"
                        GroupingField.Uploader -> "u:$uploader"
                        GroupingField.ReleaseGroup -> "g:$group"
                        GroupingField.Resolution -> "r:$res"
                        GroupingField.Codec -> "c:$codec"
                        GroupingField.Source -> "src:${release.source?.displayName ?: "unknown"}"
                    }
                }
            }
        }
    }

    private fun buildDisplayName(release: NormalizedRelease, mode: IntelligenceGroupingMode): String {
        val anime = release.animeCandidate ?: "Unknown Anime"
        val season = release.seasonCandidate ?: 1

        return when (mode) {
            IntelligenceGroupingMode.Anime -> anime
            IntelligenceGroupingMode.AnimeSeason -> "$anime - Season $season"
            IntelligenceGroupingMode.AnimeUploader -> "$anime (by ${release.uploader?.name ?: "Unknown"})"
            IntelligenceGroupingMode.AnimeReleaseGroup -> "$anime [${release.releaseGroup?.name ?: "No Group"}]"
            IntelligenceGroupingMode.AnimeTechnical -> "$anime - S$season [${release.technical.resolution?.displayName ?: "Res"}]"
            IntelligenceGroupingMode.AnimeUploaderTechnical -> "$anime (${release.uploader?.name ?: "Uploader"}) [${release.technical.resolution?.displayName}]"
            IntelligenceGroupingMode.Custom -> "$anime S$season"
        }
    }
}

/**
 * Backward compatibility alias for GroupingEngine.
 */
typealias GroupingEngine = ReleaseGroupingEngine
