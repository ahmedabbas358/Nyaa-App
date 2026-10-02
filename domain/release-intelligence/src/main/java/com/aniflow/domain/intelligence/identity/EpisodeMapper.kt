package com.aniflow.domain.intelligence.identity

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.intelligence.model.EpisodeCoverage
import com.aniflow.domain.intelligence.model.NormalizedRelease
import com.aniflow.domain.model.aggregate.release.ReleaseEpisode
import com.aniflow.domain.model.aggregate.release.ReleaseEpisodeRelationType

/**
 * Maps normalized releases to individual Episode relations (Sections 43, 99, 100).
 * Accurately models Primary, Contained, Inferred, and Possible relationships.
 */
class EpisodeMapper {

    fun mapEpisodes(
        release: NormalizedRelease,
        animeId: AnimeId
    ): List<ReleaseEpisode> {
        val season = release.season?.seasonNumber ?: 1
        val results = mutableListOf<ReleaseEpisode>()

        when (val coverage = release.episodes) {
            is EpisodeCoverage.Single -> {
                val episodeId = EpisodeId("${animeId.value}_s${season}_e${coverage.episode}")
                results.add(ReleaseEpisode(release.releaseId, episodeId, ReleaseEpisodeRelationType.Primary))
            }

            is EpisodeCoverage.Range -> {
                for (ep in coverage.from..coverage.to) {
                    val episodeId = EpisodeId("${animeId.value}_s${season}_e$ep")
                    results.add(ReleaseEpisode(release.releaseId, episodeId, ReleaseEpisodeRelationType.Contained))
                }
            }

            is EpisodeCoverage.Set -> {
                for (ep in coverage.episodes) {
                    val episodeId = EpisodeId("${animeId.value}_s${season}_e$ep")
                    results.add(ReleaseEpisode(release.releaseId, episodeId, ReleaseEpisodeRelationType.Contained))
                }
            }

            is EpisodeCoverage.Season -> {
                val episodeId = EpisodeId("${animeId.value}_s${coverage.seasonNumber}_batch")
                results.add(ReleaseEpisode(release.releaseId, episodeId, ReleaseEpisodeRelationType.Inferred))
            }

            is EpisodeCoverage.Series -> {
                val episodeId = EpisodeId("${animeId.value}_series_batch")
                results.add(ReleaseEpisode(release.releaseId, episodeId, ReleaseEpisodeRelationType.Inferred))
            }

            is EpisodeCoverage.Unknown -> {
                val episodeId = EpisodeId("${animeId.value}_s${season}_e0")
                results.add(ReleaseEpisode(release.releaseId, episodeId, ReleaseEpisodeRelationType.Possible))
            }
        }

        return results
    }
}
