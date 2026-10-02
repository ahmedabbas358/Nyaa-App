package com.aniflow.domain.service

import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.valueobject.EpisodeCoverage
import com.aniflow.domain.valueobject.EpisodeNumber

/**
 * Calculates current and missing episode coverage (Section 72 & 73).
 * Derived purely from source collections, avoiding stale persistent counters.
 */
object EpisodeCoverageService {

    fun calculateCoverage(
        expectedEpisodes: Set<EpisodeNumber>,
        availableReleases: List<Release>,
        downloadedEpisodes: Set<EpisodeNumber> = emptySet(),
        queuedEpisodes: Set<EpisodeNumber> = emptySet(),
        downloadingEpisodes: Set<EpisodeNumber> = emptySet(),
        failedEpisodes: Set<EpisodeNumber> = emptySet()
    ): EpisodeCoverage {
        val available = mutableSetOf<EpisodeNumber>()
        for (release in availableReleases) {
            val range = release.episodeRange
            if (range != null) {
                available.addAll(range.toList())
            }
        }

        val allKnown = if (expectedEpisodes.isNotEmpty()) expectedEpisodes else available
        val missing = allKnown - downloadedEpisodes - queuedEpisodes - downloadingEpisodes

        return EpisodeCoverage(
            expected = allKnown,
            available = available,
            downloaded = downloadedEpisodes,
            queued = queuedEpisodes,
            downloading = downloadingEpisodes,
            missing = missing,
            failed = failedEpisodes
        )
    }
}
