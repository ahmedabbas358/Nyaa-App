package com.aniflow.domain.service

import com.aniflow.domain.identity.CollectionId
import com.aniflow.domain.identity.DownloadPlanId
import com.aniflow.domain.model.aggregate.download.DownloadPlan
import com.aniflow.domain.model.aggregate.download.DownloadPlanValidation
import com.aniflow.domain.model.aggregate.download.DownloadPolicy
import com.aniflow.domain.model.aggregate.download.DownloadSelection
import com.aniflow.domain.model.aggregate.download.SkipSeverity
import com.aniflow.domain.model.aggregate.download.SkippedSelection
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.DuplicateMatch
import com.aniflow.domain.valueobject.StorageTarget

/**
 * Domain service validating and constructing executable DownloadPlans (Section 33, 107, 108).
 */
object DownloadPlanningService {

    fun validatePlan(
        selections: List<DownloadSelection>,
        releasesById: Map<String, Release>,
        existingDuplicates: Map<String, DuplicateMatch> = emptyMap(),
        storageTarget: StorageTarget = StorageTarget.DEFAULT
    ): DownloadPlanValidation {
        val valid = mutableListOf<DownloadSelection>()
        val skipped = mutableListOf<SkippedSelection>()
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()
        val duplicates = mutableListOf<DuplicateMatch>()
        var estimatedSize = ByteSize.ZERO

        if (selections.isEmpty()) {
            errors.add("No items selected for download")
            return DownloadPlanValidation(errors = errors)
        }

        for (sel in selections) {
            val release = releasesById[sel.releaseId.value]
            if (release == null) {
                skipped.add(
                    SkippedSelection(
                        selection = sel,
                        code = "RELEASE_NOT_FOUND",
                        message = "Release metadata not found for ID: ${sel.releaseId.value}",
                        severity = SkipSeverity.Error
                    )
                )
                continue
            }

            // Check duplicate
            val dup = existingDuplicates[sel.releaseId.value]
            if (dup != null && !sel.manualOverride) {
                duplicates.add(dup)
                skipped.add(
                    SkippedSelection(
                        selection = sel,
                        code = "DUPLICATE_ITEM",
                        message = dup.explanation,
                        severity = if (dup.isExact) SkipSeverity.Error else SkipSeverity.Warning
                    )
                )
                continue
            }

            // Accumulate valid
            valid.add(sel)
            val size = release.availability.size ?: ByteSize.ZERO
            estimatedSize += size
        }

        return DownloadPlanValidation(
            validItems = valid,
            skippedItems = skipped,
            warnings = warnings,
            errors = errors,
            duplicates = duplicates,
            storageEstimate = estimatedSize
        )
    }

    fun createPlan(
        planId: DownloadPlanId,
        validation: DownloadPlanValidation,
        destination: StorageTarget = StorageTarget.DEFAULT,
        concurrency: Int = 3,
        policy: DownloadPolicy = DownloadPolicy(),
        sourceCollectionId: CollectionId? = null
    ): DownloadPlan {
        require(validation.canProceed) { "Cannot build plan from invalid validation: ${validation.errors}" }
        return DownloadPlan(
            id = planId,
            sourceCollectionId = sourceCollectionId,
            selectedItems = validation.validItems,
            estimatedSize = validation.storageEstimate,
            destination = destination,
            concurrency = concurrency,
            policy = policy
        )
    }
}
