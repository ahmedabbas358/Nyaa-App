package com.aniflow.domain.selection.model

import com.aniflow.domain.intelligence.model.ReleaseCandidate
import com.aniflow.domain.model.aggregate.media.Episode
import com.aniflow.domain.model.aggregate.organization.Rule

/**
 * Context input for single-episode release candidate evaluation (Section 6).
 */
data class SelectionContext(
    val episode: Episode,
    val candidates: List<ReleaseCandidate>,
    val profile: DownloadProfile?,
    val preferences: UserSelectionPreferences,
    val rules: List<Rule> = emptyList(),
    val existingState: ExistingMediaState = ExistingMediaState.Missing,
    val sessionPreferences: SelectionSessionPreferences? = null,
    val previousEpisodeUploader: String? = null,
    val previousEpisodeGroup: String? = null
)

/**
 * Candidates associated with an episode for batch and multi-episode evaluation.
 */
data class EpisodeCandidateSet(
    val episode: Episode,
    val candidates: List<ReleaseCandidate>,
    val existingState: ExistingMediaState = ExistingMediaState.Missing
)

/**
 * Multi-episode evaluation context across a season or batch (Section 158).
 */
data class MultiEpisodeSelectionContext(
    val episodes: List<EpisodeCandidateSet>,
    val policy: SelectionPolicyType = SelectionPolicyType.BestCompatible,
    val profile: DownloadProfile?,
    val preferences: UserSelectionPreferences,
    val rules: List<Rule> = emptyList(),
    val consistencyPolicy: ConsistencyPolicy = ConsistencyPolicy.PerEpisode,
    val sessionPreferences: SelectionSessionPreferences? = null
)
