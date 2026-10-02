package com.aniflow.domain.model.aggregate.download

import com.aniflow.domain.identity.CollectionId
import com.aniflow.domain.identity.DownloadPlanId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.DuplicateMatch
import com.aniflow.domain.valueobject.SelectionReason
import com.aniflow.domain.valueobject.StorageTarget

/**
 * Individual item selected for download planning (Section 34).
 */
data class DownloadSelection(
    val episodeId: EpisodeId?,
    val releaseId: ReleaseId,
    val reason: SelectionReason = SelectionReason.BestCompatible,
    val manualOverride: Boolean = false
)

enum class SkipSeverity {
    Warning,
    Error
}

data class SkippedSelection(
    val selection: DownloadSelection,
    val code: String,
    val message: String,
    val severity: SkipSeverity = SkipSeverity.Warning
)

/**
 * Validated result of evaluating a download plan prior to execution (Section 107, 108, 109).
 */
data class DownloadPlanValidation(
    val validItems: List<DownloadSelection> = emptyList(),
    val skippedItems: List<SkippedSelection> = emptyList(),
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val duplicates: List<DuplicateMatch> = emptyList(),
    val storageEstimate: ByteSize = ByteSize.ZERO
) {
    val canProceed: Boolean get() = errors.isEmpty() && validItems.isNotEmpty()
    val hasWarnings: Boolean get() = warnings.isNotEmpty() || duplicates.isNotEmpty()
}

/**
 * Transient planning aggregate for batch downloads (Section 33).
 */
data class DownloadPlan(
    val id: DownloadPlanId,
    val sourceCollectionId: CollectionId? = null,
    val selectedItems: List<DownloadSelection> = emptyList(),
    val estimatedSize: ByteSize = ByteSize.ZERO,
    val destination: StorageTarget = StorageTarget.DEFAULT,
    val concurrency: Int = 3,
    val policy: DownloadPolicy = DownloadPolicy()
) {
    init {
        require(concurrency in 1..10) { "Concurrency must be between 1 and 10" }
    }
}
