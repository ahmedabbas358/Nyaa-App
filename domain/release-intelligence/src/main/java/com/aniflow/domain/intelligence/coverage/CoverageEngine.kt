package com.aniflow.domain.intelligence.coverage

import com.aniflow.domain.intelligence.model.EpisodeCoverage
import com.aniflow.domain.intelligence.model.EpisodeCoverageStatus
import com.aniflow.domain.intelligence.model.NormalizedRelease

/**
 * Granular status for episodes that are not currently available or covered (Section 51).
 */
enum class MissingReason {
    Unknown,
    NotSearched,
    NoResult,
    NoEligibleRelease,
    Missing
}

/**
 * Detailed coverage metrics across a season or anime series (Section 50, 51, 52).
 */
data class SeasonEpisodeCoverage(
    val animeTitle: String,
    val seasonNumber: Int,
    val expectedEpisodes: Set<Int>,
    val availableEpisodes: Set<Int>,
    val downloadedEpisodes: Set<Int> = emptySet(),
    val queuedEpisodes: Set<Int> = emptySet(),
    val downloadingEpisodes: Set<Int> = emptySet(),
    val missingEpisodes: Set<Int>,
    val missingReasons: Map<Int, MissingReason> = emptyMap(),
    val coveragePercentage: Float
) {
    val isComplete: Boolean get() = expectedEpisodes.isNotEmpty() && (availableEpisodes + downloadedEpisodes).containsAll(expectedEpisodes)
    val isMissingAny: Boolean get() = missingEpisodes.isNotEmpty()
}

/**
 * Calculates episode coverage, identifies missing episodes, and distinguishes reasons (Section 50, 51, 52).
 */
class EpisodeCoverageService {

    /**
     * Calculates coverage given an explicit expected episode range and available releases.
     * Does not assume 1..12 if expected range is not explicitly known (Section 52).
     */
    fun computeCoverage(
        animeTitle: String,
        seasonNumber: Int,
        expectedRange: IntRange?,
        availableReleases: List<NormalizedRelease>,
        downloadedEpisodes: Set<Int> = emptySet(),
        queuedEpisodes: Set<Int> = emptySet(),
        downloadingEpisodes: Set<Int> = emptySet(),
        defaultMissingReason: MissingReason = MissingReason.Missing
    ): SeasonEpisodeCoverage {
        val expectedSet = expectedRange?.toSet() ?: emptySet()
        val availableSet = mutableSetOf<Int>()

        for (release in availableReleases) {
            when (val coverage = release.episodes) {
                is EpisodeCoverage.Single -> {
                    if (expectedSet.isEmpty() || expectedSet.contains(coverage.episode)) {
                        availableSet.add(coverage.episode)
                    }
                }
                is EpisodeCoverage.Range -> {
                    for (ep in coverage.from..coverage.to) {
                        if (expectedSet.isEmpty() || expectedSet.contains(ep)) {
                            availableSet.add(ep)
                        }
                    }
                }
                is EpisodeCoverage.Set -> {
                    for (ep in coverage.episodes) {
                        if (expectedSet.isEmpty() || expectedSet.contains(ep)) {
                            availableSet.add(ep)
                        }
                    }
                }
                else -> {
                    // Season/series batch without enumerated episodes
                }
            }
        }

        // If expected range was not provided, available set defines the observed scope (Section 52)
        val effectiveExpected = if (expectedSet.isNotEmpty()) expectedSet else availableSet

        val knownCovered = availableSet + downloadedEpisodes + queuedEpisodes + downloadingEpisodes
        val missingSet = effectiveExpected - knownCovered

        val missingReasons = missingSet.associateWith { defaultMissingReason }

        val totalExpected = if (effectiveExpected.isNotEmpty()) effectiveExpected.size else 1
        val coveredCount = (availableSet + downloadedEpisodes).intersect(effectiveExpected).size
        val percentage = (coveredCount.toFloat() / totalExpected.toFloat()) * 100f

        return SeasonEpisodeCoverage(
            animeTitle = animeTitle,
            seasonNumber = seasonNumber,
            expectedEpisodes = effectiveExpected,
            availableEpisodes = availableSet,
            downloadedEpisodes = downloadedEpisodes,
            queuedEpisodes = queuedEpisodes,
            downloadingEpisodes = downloadingEpisodes,
            missingEpisodes = missingSet,
            missingReasons = missingReasons,
            coveragePercentage = percentage
        )
    }

    /**
     * Computes the single status for an episode given all known states.
     */
    fun resolveEpisodeStatus(
        episodeNumber: Int,
        availableEpisodes: Set<Int>,
        downloadedEpisodes: Set<Int>,
        queuedEpisodes: Set<Int>,
        downloadingEpisodes: Set<Int>
    ): EpisodeCoverageStatus {
        return when {
            downloadedEpisodes.contains(episodeNumber) -> EpisodeCoverageStatus.Downloaded
            downloadingEpisodes.contains(episodeNumber) -> EpisodeCoverageStatus.Downloading
            queuedEpisodes.contains(episodeNumber) -> EpisodeCoverageStatus.Queued
            availableEpisodes.contains(episodeNumber) -> EpisodeCoverageStatus.Available
            else -> EpisodeCoverageStatus.Missing
        }
    }
}

/**
 * Backward compatibility alias for CoverageEngine.
 */
typealias CoverageEngine = EpisodeCoverageService
