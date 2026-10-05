package com.aniflow.feature.search.components

import com.aniflow.domain.search.model.SearchResultItem
import com.aniflow.domain.search.model.SearchSource

/**
 * Intelligent O(N) Anime & Season Release Aggregator.
 * Unifies fragmented single-episode releases from Nyaa into structured
 * Season/Anime packages with multi-uploader candidate resolution.
 */
object AnimeReleaseGrouper {

    private val uploaderPrefixRegex = Regex("""^\[([^\]]+)\]\s*""")
    private val tagsRegex = Regex("""\[[^\]]*\]|\([^\)]*\)""")
    private val seasonRegex = Regex("""(?i)(?:season|\bs)\s*([0-9]{1,2})""")
    private val ordinalSeasonRegex = Regex("""(?i)\b([0-9]{1,2})(?:st|nd|rd|th)\s+season\b""")
    private val partRegex = Regex("""(?i)\b(?:part|cour)\s*([0-9]{1,2})\b""")
    private val episodeRegex = Regex("""(?i)(?:-\s*|\bep|\be|\bepisode|\b#)\s*([0-9]{1,4}(?:\.[0-9])?)\b""")

    fun groupReleases(items: List<SearchResultItem>): List<SearchResultItem> {
        val nonReleases = items.filter { it !is SearchResultItem.ReleaseResult }
        val releases = items.filterIsInstance<SearchResultItem.ReleaseResult>()

        if (releases.isEmpty()) return items

        val buckets = LinkedHashMap<String, MutableList<SearchResultItem.ReleaseResult>>()

        for (rel in releases) {
            val (cleanTitle, season) = extractAnimeIdentity(rel.title, rel.animeTitle)
            val key = "${cleanTitle.lowercase()}|s$season"
            buckets.getOrPut(key) { mutableListOf() }.add(rel)
        }

        val groupedList = mutableListOf<SearchResultItem>()

        for ((key, groupReleases) in buckets) {
            val first = groupReleases.first()
            val (cleanTitle, season) = extractAnimeIdentity(first.title, first.animeTitle)

            // Extract distinct episode numbers
            val episodeNumbers = groupReleases.mapNotNull { extractEpisodeNumber(it.title) }
                .distinct()
                .sorted()

            val uploaders = groupReleases.map { rel ->
                rel.uploader.takeIf { it.isNotBlank() && it != "Unknown" }
                    ?: rel.releaseGroup?.takeIf { it.isNotBlank() }
                    ?: Regex("""^\[([^\]]+)\]""").find(rel.title)?.groupValues?.get(1)?.trim()
                    ?: "Nyaa"
            }.distinct().sorted()
            val resolutions = groupReleases.map { it.resolution }.distinct().filter { it.isNotBlank() && it != "Unknown" }

            // Estimate total size
            val totalSizeFormatted = if (groupReleases.size == 1) {
                first.sizeFormatted
            } else {
                "${groupReleases.size} Files (~${estimateTotalSize(groupReleases)})"
            }

            groupedList.add(
                SearchResultItem.GroupedAnimeResult(
                    id = "grouped_${key.hashCode()}",
                    title = cleanTitle,
                    seasonNumber = season,
                    totalEpisodes = if (episodeNumbers.isNotEmpty()) episodeNumbers.size else groupReleases.size,
                    episodeNumbers = episodeNumbers,
                    availableUploaders = uploaders,
                    availableResolutions = resolutions,
                    totalSizeBytes = 0L,
                    formattedSize = totalSizeFormatted,
                    releaseIds = groupReleases.map { it.id },
                    releases = groupReleases,
                    source = SearchSource.Hybrid
                )
            )
        }

        return groupedList + nonReleases
    }

    private fun extractAnimeIdentity(rawTitle: String, fallbackAnime: String?): Pair<String, Int> {
        val partMatch = partRegex.find(rawTitle)
        val part = partMatch?.groupValues?.get(1)?.toIntOrNull()

        if (!fallbackAnime.isNullOrBlank() && fallbackAnime != rawTitle) {
            val seasonMatch = seasonRegex.find(rawTitle) ?: ordinalSeasonRegex.find(rawTitle)
            val season = seasonMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
            val titleWithPart = if (part != null && !fallbackAnime.contains("Part", true) && !fallbackAnime.contains("Cour", true)) {
                "${fallbackAnime.trim()} (Part $part)"
            } else {
                fallbackAnime.trim()
            }
            return titleWithPart to season
        }

        // Strip uploader tag like [SubsPlease]
        var cleaned = rawTitle.replace(uploaderPrefixRegex, "")
        // Find season (e.g. S02 or 2nd Season)
        val seasonMatch = seasonRegex.find(cleaned) ?: ordinalSeasonRegex.find(cleaned)
        val season = seasonMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1

        // Strip everything after episode number or bracketed tags
        val episodeMatch = episodeRegex.find(cleaned)
        if (episodeMatch != null) {
            cleaned = cleaned.substring(0, episodeMatch.range.first)
        }
        cleaned = cleaned.replace(tagsRegex, "")
            .replace(seasonRegex, "")
            .replace(ordinalSeasonRegex, "")
            .replace(partRegex, "")
            .trim()
        if (cleaned.endsWith("-")) {
            cleaned = cleaned.dropLast(1).trim()
        }

        val baseTitle = if (cleaned.isNotBlank()) cleaned else rawTitle
        val finalTitle = if (part != null && !baseTitle.contains("Part", true) && !baseTitle.contains("Cour", true)) {
            "$baseTitle (Part $part)"
        } else {
            baseTitle
        }

        return finalTitle to season
    }

    fun extractEpisodeNumber(rawTitle: String): Int? {
        val match = episodeRegex.find(rawTitle) ?: return null
        return match.groupValues[1].toDoubleOrNull()?.toInt()
    }

    fun estimateTotalSize(releases: List<SearchResultItem.ReleaseResult>): String {
        var totalMb = 0.0
        for (rel in releases) {
            val str = rel.sizeFormatted.trim().uppercase()
            val num = str.substringBefore(" ").toDoubleOrNull() ?: continue
            if (str.contains("GI") || str.contains("GB")) {
                totalMb += num * 1024.0
            } else if (str.contains("MI") || str.contains("MB")) {
                totalMb += num
            }
        }
        return if (totalMb >= 1024.0) {
            "%.1f GB".format(totalMb / 1024.0)
        } else {
            "%.0f MB".format(totalMb)
        }
    }
}
