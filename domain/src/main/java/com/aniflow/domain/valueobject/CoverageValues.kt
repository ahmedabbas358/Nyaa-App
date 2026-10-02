package com.aniflow.domain.valueobject

/**
 * Detailed episode coverage metrics for a Season or Anime series (Section 72 & 73).
 * Derived dynamically from source data rather than stale persistent counters.
 */
data class EpisodeCoverage(
    val expected: Set<EpisodeNumber> = emptySet(),
    val available: Set<EpisodeNumber> = emptySet(),
    val downloaded: Set<EpisodeNumber> = emptySet(),
    val queued: Set<EpisodeNumber> = emptySet(),
    val downloading: Set<EpisodeNumber> = emptySet(),
    val missing: Set<EpisodeNumber> = emptySet(),
    val failed: Set<EpisodeNumber> = emptySet()
) {
    val totalExpectedCount: Int get() = expected.size
    val downloadedCount: Int get() = downloaded.size
    val missingCount: Int get() = missing.size

    val isFullyDownloaded: Boolean
        get() = expected.isNotEmpty() && downloaded.containsAll(expected)

    val isFullyAvailable: Boolean
        get() = expected.isNotEmpty() && available.containsAll(expected)

    val completionPercentage: Float
        get() = if (expected.isEmpty()) 0.0f else (downloaded.size.toFloat() / expected.size.toFloat()) * 100f
}

/**
 * Result of comparing a batch release against individual releases and library coverage (Section 112).
 */
data class CoverageComparison(
    val coveredEpisodes: Set<EpisodeNumber>,
    val missingEpisodes: Set<EpisodeNumber>,
    val redundantEpisodes: Set<EpisodeNumber>,
    val totalBatchSize: ByteSize,
    val totalIndividualSize: ByteSize,
    val recommendation: ComparisonRecommendation,
    val explanation: String
) {
    enum class ComparisonRecommendation {
        PreferBatch,
        PreferIndividuals,
        EqualSuitability,
        RequiresManualChoice
    }
}
