package com.aniflow.domain.model.aggregate.download

import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.valueobject.SearchQuery

/**
 * Grouping strategies for organizing discovered releases (Section 69 & 70).
 */
enum class GroupingMode {
    Anime,
    AnimeSeason,
    AnimeUploader,
    AnimeReleaseGroup,
    AnimeTechnicalProfile,
    Custom
}

/**
 * Ephemeral session state capturing user selections during search and discovery (Section 104, 105, 106).
 * Strictly prevents polluting Release entity with mutable UI flags like `release.isSelected`.
 */
data class SelectionSession(
    val sessionId: String,
    val query: SearchQuery,
    val groupingMode: GroupingMode = GroupingMode.AnimeSeason,
    val selectedEpisodes: Set<EpisodeId> = emptySet(),
    val selectedReleases: Set<ReleaseId> = emptySet(),
    val excludedReleases: Set<ReleaseId> = emptySet(),
    val manualOverrides: Map<EpisodeId, ReleaseId> = emptyMap()
) {
    fun selectRelease(releaseId: ReleaseId): SelectionSession =
        copy(
            selectedReleases = selectedReleases + releaseId,
            excludedReleases = excludedReleases - releaseId
        )

    fun deselectRelease(releaseId: ReleaseId): SelectionSession =
        copy(
            selectedReleases = selectedReleases - releaseId,
            excludedReleases = excludedReleases + releaseId
        )

    fun overrideForEpisode(episodeId: EpisodeId, releaseId: ReleaseId): SelectionSession =
        copy(
            manualOverrides = manualOverrides + (episodeId to releaseId),
            selectedReleases = selectedReleases + releaseId
        )

    fun isReleaseSelected(releaseId: ReleaseId): Boolean = selectedReleases.contains(releaseId)
}
