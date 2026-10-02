package com.aniflow.domain.valueobject

/**
 * Concrete type of duplicate match detected between two releases or downloads (Section 75).
 */
enum class DuplicateType {
    SameProviderRelease,
    SameInfoHash,
    SameSource,
    SameNormalizedIdentity,
    SameFileFingerprint,
    PossibleDuplicate
}

/**
 * Confidence level of duplicate detection (Section 76).
 * Safe rule: Never automatically delete or bypass files on Possible confidence alone.
 */
enum class DuplicateConfidence {
    Exact,
    Strong,
    Possible
}

/**
 * Typed result of duplicate analysis (Section 75 & 140).
 * Replaces naive boolean `isDuplicate` flags with rich, actionable explanation.
 */
data class DuplicateMatch(
    val duplicateType: DuplicateType,
    val confidence: DuplicateConfidence,
    val matchedAgainstId: String,
    val explanation: String
) {
    val isExact: Boolean get() = confidence == DuplicateConfidence.Exact
    val requiresManualConfirmation: Boolean get() = confidence == DuplicateConfidence.Possible
}
