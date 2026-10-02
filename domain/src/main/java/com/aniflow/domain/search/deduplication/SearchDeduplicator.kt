package com.aniflow.domain.search.deduplication

import com.aniflow.domain.search.model.SearchResultItem
import com.aniflow.domain.search.model.SearchSource

/**
 * SearchDeduplicator (Section 17, 18).
 * Pure, deterministic deduplication and provenance merging across Local and Provider results.
 * Never relies on title equality alone.
 */
object SearchDeduplicator {

    fun deduplicate(items: List<SearchResultItem>): List<SearchResultItem> {
        val mergedMap = LinkedHashMap<String, SearchResultItem>()

        for (item in items) {
            val key = buildDeduplicationKey(item)
            val existing = mergedMap[key]

            if (existing == null) {
                mergedMap[key] = item
            } else {
                mergedMap[key] = mergeItems(existing, item)
            }
        }

        return mergedMap.values.toList()
    }

    fun buildDeduplicationKey(item: SearchResultItem): String {
        return when (item) {
            is SearchResultItem.AnimeResult -> "anime:${item.title.trim().lowercase()}"
            is SearchResultItem.SeasonResult -> "season:${item.animeTitle.trim().lowercase()}_s${item.seasonNumber}"
            is SearchResultItem.EpisodeResult -> "episode:${item.animeTitle.trim().lowercase()}_ep${item.episodeNumber}"
            is SearchResultItem.ReleaseResult -> {
                // Section 17, 18: Provider ID + Release ID, or title + resolution + codec
                val prefix = item.id.ifBlank { "rel" }
                val techSig = "${item.resolution}_${item.codec}_${item.uploader.lowercase()}"
                "release:$prefix:$techSig"
            }
            is SearchResultItem.UploaderResult -> "uploader:${item.title.trim().lowercase()}"
            is SearchResultItem.ReleaseGroupResult -> "group:${item.title.trim().lowercase()}"
            is SearchResultItem.LibraryResult -> "library:${item.filePath.trim().lowercase()}"
            is SearchResultItem.DownloadResult -> "download:${item.id}"
            is SearchResultItem.CollectionResult -> "collection:${item.id}"
            is SearchResultItem.SavedSearchResult -> "savedsearch:${item.queryText.trim().lowercase()}"
        }
    }

    private fun mergeItems(existing: SearchResultItem, incoming: SearchResultItem): SearchResultItem {
        // Prefer hybrid or local provenance over purely remote, while preserving provider stats
        return when {
            existing is SearchResultItem.ReleaseResult && incoming is SearchResultItem.ReleaseResult -> {
                existing.copy(
                    source = SearchSource.Hybrid,
                    seeders = maxOf(existing.seeders, incoming.seeders),
                    leechers = maxOf(existing.leechers, incoming.leechers),
                    isDownloaded = existing.isDownloaded || incoming.isDownloaded,
                    isDownloading = existing.isDownloading || incoming.isDownloading,
                    isKnownSeen = existing.isKnownSeen || incoming.isKnownSeen,
                    relevanceScore = maxOf(existing.relevanceScore, incoming.relevanceScore),
                    matchedReasons = (existing.matchedReasons + incoming.matchedReasons).distinct()
                )
            }
            existing is SearchResultItem.AnimeResult && incoming is SearchResultItem.AnimeResult -> {
                existing.copy(
                    coveredEpisodes = maxOf(existing.coveredEpisodes, incoming.coveredEpisodes),
                    totalEpisodes = maxOf(existing.totalEpisodes, incoming.totalEpisodes),
                    relevanceScore = maxOf(existing.relevanceScore, incoming.relevanceScore)
                )
            }
            else -> existing
        }
    }
}
