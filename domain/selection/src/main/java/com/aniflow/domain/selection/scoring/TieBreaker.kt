package com.aniflow.domain.selection.scoring

import com.aniflow.domain.intelligence.model.ReleaseCandidate
import com.aniflow.domain.selection.model.PreferenceMode
import com.aniflow.domain.selection.model.UserSelectionPreferences

/**
 * Deterministic tie-breaker for release candidates with equal scores (Section 52, 53, 116, 146).
 * Ensures 100% reproducible results across re-evaluations and refreshes.
 */
class TieBreaker {

    fun breakTie(
        candidateA: ReleaseCandidate,
        candidateB: ReleaseCandidate,
        preferences: UserSelectionPreferences
    ): Int {
        val relA = candidateA.release
        val relB = candidateB.release

        // 1. Explicit user uploader preference
        val uploaderA = preferences.uploader.getDisposition(relA.uploaderName) == PreferenceMode.Preferred
        val uploaderB = preferences.uploader.getDisposition(relB.uploaderName) == PreferenceMode.Preferred
        if (uploaderA && !uploaderB) return -1
        if (!uploaderA && uploaderB) return 1

        // 2. Exact resolution match
        val resExactA = relA.technicalMetadata.resolution == preferences.resolution.target
        val resExactB = relB.technicalMetadata.resolution == preferences.resolution.target
        if (resExactA && !resExactB) return -1
        if (!resExactA && resExactB) return 1

        // 3. Exact first preferred codec match
        val topCodec = preferences.codec.preferredCodecs.firstOrNull()
        if (topCodec != null) {
            val codecA = relA.technicalMetadata.videoCodec == topCodec
            val codecB = relB.technicalMetadata.videoCodec == topCodec
            if (codecA && !codecB) return -1
            if (!codecA && codecB) return 1
        }

        // 4. Higher availability (seeders count)
        val seedersA = relA.rawMetadata["seeders"]?.toIntOrNull() ?: 0
        val seedersB = relB.rawMetadata["seeders"]?.toIntOrNull() ?: 0
        if (seedersA != seedersB) {
            return seedersB.compareTo(seedersA) // Higher seeders first
        }

        // 5. Smaller file size
        val sizeA = relA.rawMetadata["sizeBytes"]?.toLongOrNull() ?: Long.MAX_VALUE
        val sizeB = relB.rawMetadata["sizeBytes"]?.toLongOrNull() ?: Long.MAX_VALUE
        if (sizeA != sizeB) {
            return sizeA.compareTo(sizeB) // Smaller size first
        }

        // 6. Higher parse confidence
        if (candidateA.confidence != candidateB.confidence) {
            return candidateB.confidence.compareTo(candidateA.confidence)
        }

        // 7. Stable deterministic providerReleaseId comparison
        val idA = relA.providerReleaseId ?: relA.id.value
        val idB = relB.providerReleaseId ?: relB.id.value
        return idA.compareTo(idB)
    }

    /**
     * Overload for SelectionCandidate (Step 21 Domain Model).
     */
    fun breakTieCandidates(
        candidateA: com.aniflow.domain.selection.context.SelectionCandidate,
        candidateB: com.aniflow.domain.selection.context.SelectionCandidate,
        preferences: UserSelectionPreferences
    ): Int {
        // 1. Explicit user uploader preference
        val uploaderA = preferences.uploader.getDisposition(candidateA.uploader) == PreferenceMode.Preferred
        val uploaderB = preferences.uploader.getDisposition(candidateB.uploader) == PreferenceMode.Preferred
        if (uploaderA && !uploaderB) return -1
        if (!uploaderA && uploaderB) return 1

        // 2. Exact resolution match
        val resExactA = candidateA.technical.resolution == preferences.resolution.target
        val resExactB = candidateB.technical.resolution == preferences.resolution.target
        if (resExactA && !resExactB) return -1
        if (!resExactA && resExactB) return 1

        // 3. Exact first preferred codec match
        val topCodec = preferences.codec.preferredCodecs.firstOrNull()
        if (topCodec != null) {
            val codecA = candidateA.technical.videoCodec == topCodec
            val codecB = candidateB.technical.videoCodec == topCodec
            if (codecA && !codecB) return -1
            if (!codecA && codecB) return 1
        }

        // 4. Higher availability (seeders count)
        val seedersA = candidateA.seeders ?: 0
        val seedersB = candidateB.seeders ?: 0
        if (seedersA != seedersB) {
            return seedersB.compareTo(seedersA) // Higher seeders first
        }

        // 5. Smaller file size
        val sizeA = candidateA.size?.bytes ?: Long.MAX_VALUE
        val sizeB = candidateB.size?.bytes ?: Long.MAX_VALUE
        if (sizeA != sizeB) {
            return sizeA.compareTo(sizeB) // Smaller size first
        }

        // 6. Higher parse confidence
        if (candidateA.confidence != candidateB.confidence) {
            return candidateB.confidence.score.compareTo(candidateA.confidence.score)
        }

        // 7. Stable deterministic releaseId lexical order (Section 57)
        return candidateA.releaseId.value.compareTo(candidateB.releaseId.value)
    }
}

