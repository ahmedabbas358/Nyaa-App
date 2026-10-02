package com.aniflow.domain.selection.simulation

import com.aniflow.domain.identity.DownloadProfileId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.selection.model.DownloadProfile
import com.aniflow.domain.selection.model.MultiEpisodeSelectionResult
import com.aniflow.domain.selection.model.SelectionWarning
import java.time.Instant

/**
 * Audit record capturing deterministic decisions for each evaluated episode (Section 115, 154, 155).
 */
data class SelectionAudit(
    val episodeId: EpisodeId,
    val releaseId: ReleaseId?,
    val profileId: DownloadProfileId?,
    val algorithmVersion: Int = 1,
    val score: Int,
    val reasons: List<String>,
    val timestamp: Instant = Instant.now()
)

/**
 * Result of a Dry-Run profile simulation without initiating downloads (Section 77, 78, 113, 114).
 */
data class SimulationResult(
    val totalEpisodes: Int,
    val selectedCount: Int,
    val preferredCount: Int,
    val fallbackCount: Int,
    val unresolvedCount: Int,
    val totalEstimatedBytes: Long,
    val warnings: List<SelectionWarning>,
    val uploaderBreakdown: Map<String, Int>,
    val auditTrail: List<SelectionAudit>
)

/**
 * Comparative analysis between two profiles evaluated on the same episodes (Section 112).
 */
data class SelectionComparison(
    val profileA: DownloadProfile,
    val profileB: DownloadProfile,
    val resultA: SimulationResult,
    val resultB: SimulationResult
) {
    val coverageDifference: Int get() = resultA.selectedCount - resultB.selectedCount
    val sizeDifferenceBytes: Long get() = resultA.totalEstimatedBytes - resultB.totalEstimatedBytes
}
