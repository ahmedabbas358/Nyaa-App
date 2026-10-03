package com.aniflow.domain.automation.deduplication

import com.aniflow.domain.intelligence.model.NormalizedRelease

enum class ReleaseSeenState {
    New,
    Seen,
    Ignored,
    Downloaded,
    Rejected,
    Expired
}

enum class ReleaseChangeType {
    NewRelease,
    ReleaseUpdated,      // Only seeders/leechers/download count changed (Section 19)
    MetadataChanged,     // Magnet or technical description changed
    Unchanged
}

data class ReleaseComparisonResult(
    val release: NormalizedRelease,
    val state: ReleaseSeenState,
    val changeType: ReleaseChangeType
)

data class NewReleaseDetectionResult(
    val newReleases: List<NormalizedRelease>,
    val updatedReleases: List<NormalizedRelease>,
    val seenCount: Int,
    val totalProcessed: Int
)

/**
 * Known release identity cache record (Section 17, 18, 19).
 */
data class KnownReleaseRecord(
    val identityKey: String, // provider:providerReleaseId or infoHash
    val seeders: Int,
    val leechers: Int,
    val magnetUri: String?,
    val state: ReleaseSeenState = ReleaseSeenState.Seen
)

/**
 * NewReleaseDetector (Section 15, 16, 17, 18, 19, 20, 87, 128, 146, 159).
 * Detects newly published releases from provider search results.
 * Strictly separates New Releases from stat updates (seeders/leechers changes)
 * to avoid duplicate triggers and spam.
 */
class NewReleaseDetector {

    fun detect(
        incomingReleases: List<NormalizedRelease>,
        knownReleases: Map<String, KnownReleaseRecord>
    ): NewReleaseDetectionResult {
        val newReleases = mutableListOf<NormalizedRelease>()
        val updatedReleases = mutableListOf<NormalizedRelease>()
        var seenCount = 0

        // Section 87, 146: Deduplicate incoming releases by unique identity first
        val uniqueIncoming = incomingReleases.distinctBy { release ->
            buildIdentityKey(release)
        }

        for (release in uniqueIncoming) {
            val key = buildIdentityKey(release)
            val known = knownReleases[key]

            if (known == null) {
                // Completely new release!
                newReleases.add(release)
            } else {
                seenCount++
                val hasStatChange = known.seeders != release.stats.seeders || known.leechers != release.stats.leechers
                val hasMagnetChange = known.magnetUri != release.links.magnetUri?.value

                if (hasMagnetChange) {
                    updatedReleases.add(release)
                } else if (hasStatChange) {
                    // Stat update only: does NOT trigger new release automation (Section 19)
                }
            }
        }

        return NewReleaseDetectionResult(
            newReleases = newReleases,
            updatedReleases = updatedReleases,
            seenCount = seenCount,
            totalProcessed = uniqueIncoming.size
        )
    }

    fun buildIdentityKey(release: NormalizedRelease): String {
        val infoHash = release.links.infoHash?.value
        return if (!infoHash.isNullOrBlank()) {
            "infohash:$infoHash"
        } else {
            val provider = release.releaseSource?.providerName ?: release.rawMetadata["provider"] ?: "Nyaa"
            val id = release.providerReleaseId ?: release.releaseId.value
            "provider:$provider:$id"
        }
    }
}
