package com.aniflow.domain.coverage

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.SeasonId

/**
 * Step 20 — Episode Availability State (Section 19, 20).
 * Strictly distinguishes NotSearched, NoReleaseFound, and NoEligibleRelease.
 */
enum class EpisodeAvailabilityState(val label: String, val symbol: String) {
    NotSearched("Not Searched", "?"),
    Searching("Searching", "…"),
    NoReleaseFound("Not Found", "—"),
    NoEligibleRelease("No Eligible Release", "—"),
    Available("Available", "✓"),
    Downloading("Downloading", "↓"),
    Downloaded("Downloaded", "✓✓"),
    Upgradeable("Upgrade Available", "↑"),
    ReviewRequired("Review Required", "!")
}

/**
 * Step 20 — Coverage Confidence (Section 21, 30).
 */
enum class CoverageConfidence {
    High,
    Medium,
    Low,
    Inferred,
    UserMapped
}

/**
 * Step 20 — Episode Coverage Result (Section 21).
 */
data class EpisodeCoverage(
    val episodeId: EpisodeId,
    val state: EpisodeAvailabilityState,
    val releaseCount: Int,
    val selectedReleaseId: ReleaseId? = null,
    val localFileCount: Int = 0,
    val upgradeAvailable: Boolean = false,
    val confidence: CoverageConfidence = CoverageConfidence.High
) {
    val isCovered: Boolean
        get() = state == EpisodeAvailabilityState.Available ||
                state == EpisodeAvailabilityState.Downloading ||
                state == EpisodeAvailabilityState.Downloaded ||
                state == EpisodeAvailabilityState.Upgradeable

    val isMissing: Boolean
        get() = state == EpisodeAvailabilityState.NoReleaseFound ||
                state == EpisodeAvailabilityState.NoEligibleRelease
}

/**
 * Step 20 — Season Coverage Result (Section 22, 24).
 * If expectedCount is unknown, percentage is null (no fake 0% or fake precision).
 */
data class SeasonCoverage(
    val seasonId: SeasonId,
    val expectedCount: Int?,
    val knownCount: Int,
    val availableCount: Int,
    val downloadingCount: Int = 0,
    val missingCount: Int? = if (expectedCount != null) (expectedCount - availableCount).coerceAtLeast(0) else null,
    val reviewCount: Int = 0,
    val percentage: Float? = if (expectedCount != null && expectedCount > 0) {
        ((availableCount.toFloat() / expectedCount.toFloat()) * 100f).coerceIn(0f, 100f)
    } else null
) {
    val isComplete: Boolean get() = expectedCount != null && availableCount >= expectedCount
    val displaySummary: String
        get() = if (expectedCount != null) {
            "$availableCount / $expectedCount episodes (${percentage?.toInt() ?: 0}%)"
        } else {
            "$availableCount episodes found"
        }
}

/**
 * Step 20 — Anime Coverage Result (Section 23, 24, 82, 83).
 */
data class AnimeCoverage(
    val animeId: AnimeId,
    val seasons: List<SeasonCoverage>,
    val totalExpected: Int?,
    val totalAvailable: Int,
    val totalDownloading: Int = 0,
    val totalReview: Int = 0
) {
    val percentage: Float? = if (totalExpected != null && totalExpected > 0) {
        ((totalAvailable.toFloat() / totalExpected.toFloat()) * 100f).coerceIn(0f, 100f)
    } else null

    val displaySummary: String
        get() = if (totalExpected != null) {
            "$totalAvailable / $totalExpected episodes available"
        } else {
            "$totalAvailable episodes discovered across ${seasons.size} seasons"
        }
}
