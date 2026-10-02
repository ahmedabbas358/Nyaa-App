package com.aniflow.domain.intelligence.adapter

import com.aniflow.domain.intelligence.model.EpisodeCoverage
import com.aniflow.domain.intelligence.model.NormalizedRelease
import com.aniflow.domain.intelligence.model.ReleaseCandidate

/**
 * Step 19 — Selection Compatibility Contract (Section 106, 107).
 * Maps normalized releases into candidate sets consumable by Selection Engine (Step 7/21)
 * without leaking parser internals.
 */
object SelectionCompatibilityAdapter {

    fun toReleaseCandidates(release: NormalizedRelease): List<ReleaseCandidate> {
        val conf = release.confidence.overall.toDouble()
        return when (val cov = release.episodes) {
            is EpisodeCoverage.Single -> listOf(ReleaseCandidate(cov.episode, release, conf))
            is EpisodeCoverage.Range -> (cov.from..cov.to).map { ReleaseCandidate(it, release, conf) }
            is EpisodeCoverage.Set -> cov.episodes.sorted().map { ReleaseCandidate(it, release, conf) }
            is EpisodeCoverage.Season -> listOf(ReleaseCandidate(null, release, conf))
            is EpisodeCoverage.Series -> listOf(ReleaseCandidate(null, release, conf))
            is EpisodeCoverage.Unknown -> listOf(ReleaseCandidate(null, release, conf))
        }
    }
}
