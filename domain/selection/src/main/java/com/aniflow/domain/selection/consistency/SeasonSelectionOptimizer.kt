package com.aniflow.domain.selection.consistency

import com.aniflow.domain.intelligence.model.ReleaseCandidate
import com.aniflow.domain.selection.model.ConsistencyPolicy
import com.aniflow.domain.selection.model.EpisodeCandidateSet
import com.aniflow.domain.selection.model.EvaluatedCandidate
import com.aniflow.domain.selection.model.UserSelectionPreferences

/**
 * Optimizes release choices across an entire season for uniform uploader/group/quality (Section 105, 106, 107, 108, 109, 110, 160, 161).
 */
class SeasonSelectionOptimizer {

    fun determineDominantUploader(
        episodeSets: List<EpisodeCandidateSet>,
        preferences: UserSelectionPreferences
    ): String? {
        // 1. If user explicitly preferred an uploader, check if they cover significant portion
        val preferredUploader = preferences.uploader.preferred.firstOrNull()
        if (preferredUploader != null) {
            val coverage = episodeSets.count { set ->
                set.candidates.any { it.release.uploader.equals(preferredUploader, ignoreCase = true) }
            }
            if (coverage >= (episodeSets.size * 0.6)) {
                return preferredUploader
            }
        }

        // 2. Count frequency of uploaders across top candidates
        val frequency = mutableMapOf<String, Int>()
        episodeSets.forEach { set ->
            set.candidates.forEach { cand ->
                cand.release.uploader?.let { uploader ->
                    frequency[uploader] = (frequency[uploader] ?: 0) + 1
                }
            }
        }

        return frequency.maxByOrNull { it.value }?.key
    }

    fun applyConsistencyBonus(
        candidates: List<EvaluatedCandidate>,
        dominantUploader: String?,
        policy: ConsistencyPolicy
    ): List<EvaluatedCandidate> {
        if (dominantUploader == null || policy == ConsistencyPolicy.PerEpisode) {
            return candidates
        }

        return candidates.map { evaluated ->
            val uploader = evaluated.candidate.release.uploader
            if (uploader.equals(dominantUploader, ignoreCase = true)) {
                // If candidate matches dominant uploader, boost its score by 15 consistency points
                val newScore = evaluated.score.copy(total = evaluated.score.total + 15)
                evaluated.copy(score = newScore)
            } else {
                evaluated
            }
        }
    }
}
