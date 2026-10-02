package com.aniflow.domain.service

import com.aniflow.domain.model.aggregate.download.GroupingMode
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.model.aggregate.release.ReleaseEpisode
import com.aniflow.domain.model.aggregate.release.ReleaseEpisodeRelationType

/**
 * Rich result container for release grouping (Section 68).
 */
data class GroupingResult(
    val groups: Map<String, List<Release>>,
    val episodeMappings: List<ReleaseEpisode>,
    val unmatchedReleases: List<Release> = emptyList(),
    val ambiguousReleases: List<Release> = emptyList()
)

/**
 * Domain service executing intelligent grouping across discovered releases (Section 68, 69, 70, 71).
 */
object ReleaseGroupingService {

    fun group(
        releases: List<Release>,
        mode: GroupingMode = GroupingMode.AnimeSeason
    ): GroupingResult {
        val groups = mutableMapOf<String, MutableList<Release>>()
        val episodeMappings = mutableListOf<ReleaseEpisode>()
        val ambiguous = mutableListOf<Release>()
        val unmatched = mutableListOf<Release>()

        for (release in releases) {
            val key = when (mode) {
                GroupingMode.Anime -> {
                    release.animeIdentity?.normalizedTitle ?: release.normalizedTitle
                }
                GroupingMode.AnimeSeason -> {
                    val title = release.animeIdentity?.normalizedTitle ?: release.normalizedTitle
                    val season = release.seasonHint?.toString() ?: "S01"
                    "$title | $season"
                }
                GroupingMode.AnimeUploader -> {
                    val title = release.animeIdentity?.normalizedTitle ?: release.normalizedTitle
                    val uploader = release.uploader?.name ?: "Unknown Uploader"
                    "$title | $uploader"
                }
                GroupingMode.AnimeReleaseGroup -> {
                    val title = release.animeIdentity?.normalizedTitle ?: release.normalizedTitle
                    val group = release.releaseGroup?.name ?: "No Group"
                    "$title | $group"
                }
                GroupingMode.AnimeTechnicalProfile -> {
                    val title = release.animeIdentity?.normalizedTitle ?: release.normalizedTitle
                    val res = release.technical.resolution?.displayName ?: "Unknown Res"
                    val codec = release.technical.videoCodec?.displayName ?: "Unknown Codec"
                    "$title | $res | $codec"
                }
                GroupingMode.Custom -> release.normalizedTitle
            }

            if (key.isBlank()) {
                unmatched.add(release)
            } else if (release.parseInfo.needsManualReview) {
                ambiguous.add(release)
                groups.getOrPut(key) { mutableListOf() }.add(release)
            } else {
                groups.getOrPut(key) { mutableListOf() }.add(release)
            }
        }

        return GroupingResult(
            groups = groups,
            episodeMappings = episodeMappings,
            unmatchedReleases = unmatched,
            ambiguousReleases = ambiguous
        )
    }
}
