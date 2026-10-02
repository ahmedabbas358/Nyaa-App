package com.aniflow.domain.model.aggregate.release

import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId

/**
 * Relation strength between a Release and an Episode (Section 19).
 */
enum class ReleaseEpisodeRelationType {
    /** The release corresponds directly to this single episode. */
    Primary,
    /** The episode is fully contained inside a batch or multi-episode release. */
    Contained,
    /** Low parser confidence or ambiguous title heuristics suggest a match. */
    Possible,
    /** Inferred via season completeness or metadata association without explicit episode number. */
    Inferred
}

/**
 * Many-to-Many Junction Entity connecting Releases and Episodes (Section 17, 18, 131).
 * Resolves the fundamental architectural challenge where a single release may cover:
 * - A single episode (Primary)
 * - A batch of episodes 01-12 (Contained)
 * - A full season or complete series (Inferred / Contained)
 */
data class ReleaseEpisode(
    val releaseId: ReleaseId,
    val episodeId: EpisodeId,
    val relationType: ReleaseEpisodeRelationType = ReleaseEpisodeRelationType.Primary
)
