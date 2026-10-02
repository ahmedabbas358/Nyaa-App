package com.aniflow.feature.library.search

import com.aniflow.feature.library.model.VideoTrackInfo

enum class LibrarySortBy {
    Name,
    DateAdded,
    LastModified,
    EpisodeCount,
    Size
}

enum class SortDirection {
    Ascending,
    Descending
}

data class LibraryFilterCriteria(
    val query: String? = null,
    val storageLocationId: String? = null,
    val resolution: String? = null,
    val codec: String? = null,
    val isCompleteOnly: Boolean = false,
    val isMissingFilesOnly: Boolean = false
)

data class SearchableLibraryItem(
    val id: String,
    val animeTitle: String,
    val seasonNumber: Int,
    val episodeNumber: Double?,
    val episodeTitle: String?,
    val fileName: String,
    val path: String,
    val storageLocationId: String,
    val sizeBytes: Long,
    val dateAddedEpochMillis: Long,
    val lastModifiedEpochMillis: Long,
    val videoTrack: VideoTrackInfo? = null,
    val releaseGroup: String? = null,
    val isOffline: Boolean = false
)

data class DuplicateEpisodeGroup(
    val animeTitle: String,
    val seasonNumber: Int,
    val episodeNumber: Double,
    val files: List<SearchableLibraryItem>
)

/**
 * LibrarySearchEngine (Sections 97, 98, 99, 149, 150, 151, 152).
 * Provides multi-faceted searching, sorting, filtering (by storage location, quality, completeness),
 * and duplicate episode candidate detection.
 */
class LibrarySearchEngine {

    fun filterAndSort(
        items: List<SearchableLibraryItem>,
        criteria: LibraryFilterCriteria,
        sortBy: LibrarySortBy = LibrarySortBy.Name,
        direction: SortDirection = SortDirection.Ascending
    ): List<SearchableLibraryItem> {
        val queryLower = criteria.query?.trim()?.lowercase()

        var filtered = items.filter { item ->
            // Query match across title, episode, filename, group, codec
            if (queryLower != null && queryLower.isNotEmpty()) {
                val matchesTitle = item.animeTitle.lowercase().contains(queryLower)
                val matchesEpTitle = item.episodeTitle?.lowercase()?.contains(queryLower) == true
                val matchesFile = item.fileName.lowercase().contains(queryLower)
                val matchesGroup = item.releaseGroup?.lowercase()?.contains(queryLower) == true
                val matchesCodec = item.videoTrack?.codec?.lowercase()?.contains(queryLower) == true
                if (!matchesTitle && !matchesEpTitle && !matchesFile && !matchesGroup && !matchesCodec) {
                    return@filter false
                }
            }

            // Storage Location filter
            if (criteria.storageLocationId != null && item.storageLocationId != criteria.storageLocationId) {
                return@filter false
            }

            // Resolution filter
            if (criteria.resolution != null) {
                val itemRes = "${item.videoTrack?.height}p"
                if (!itemRes.equals(criteria.resolution, ignoreCase = true)) return@filter false
            }

            // Codec filter
            if (criteria.codec != null) {
                if (!item.videoTrack?.codec.equals(criteria.codec, ignoreCase = true)) return@filter false
            }

            true
        }

        // Sorting
        filtered = when (sortBy) {
            LibrarySortBy.Name -> filtered.sortedWith(compareBy({ it.animeTitle }, { it.seasonNumber }, { it.episodeNumber ?: 0.0 }))
            LibrarySortBy.DateAdded -> filtered.sortedBy { it.dateAddedEpochMillis }
            LibrarySortBy.LastModified -> filtered.sortedBy { it.lastModifiedEpochMillis }
            LibrarySortBy.Size -> filtered.sortedBy { it.sizeBytes }
            LibrarySortBy.EpisodeCount -> filtered.sortedBy { it.episodeNumber ?: 0.0 }
        }

        return if (direction == SortDirection.Descending) filtered.reversed() else filtered
    }

    fun findDuplicateEpisodes(items: List<SearchableLibraryItem>): List<DuplicateEpisodeGroup> {
        return items.filter { it.episodeNumber != null }
            .groupBy { Triple(it.animeTitle.lowercase(), it.seasonNumber, it.episodeNumber!!) }
            .filter { it.value.size > 1 }
            .map { (key, group) ->
                DuplicateEpisodeGroup(
                    animeTitle = group.first().animeTitle,
                    seasonNumber = key.second,
                    episodeNumber = key.third,
                    files = group
                )
            }
    }
}
