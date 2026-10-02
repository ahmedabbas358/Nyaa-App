package com.aniflow.domain.service

import com.aniflow.domain.identity.FileFingerprint
import com.aniflow.domain.model.aggregate.library.LibraryFile
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.model.aggregate.release.ReleaseSource
import com.aniflow.domain.valueobject.DuplicateConfidence
import com.aniflow.domain.valueobject.DuplicateMatch
import com.aniflow.domain.valueobject.DuplicateType

/**
 * Domain service detecting duplicate releases, torrents, and files (Section 75 & 76).
 * Replaces simple booleans with rich, explainable DuplicateMatch models.
 */
object DuplicateDetectionService {

    fun checkDuplicate(
        candidate: Release,
        existingReleases: List<Release>
    ): DuplicateMatch? {
        for (existing in existingReleases) {
            // 1. Exact Provider Release ID
            if (candidate.provider == existing.provider &&
                candidate.providerReleaseId != null &&
                candidate.providerReleaseId == existing.providerReleaseId
            ) {
                return DuplicateMatch(
                    duplicateType = DuplicateType.SameProviderRelease,
                    confidence = DuplicateConfidence.Exact,
                    matchedAgainstId = existing.id.value,
                    explanation = "Exact duplicate on provider ${candidate.provider.name} (ID: ${candidate.providerReleaseId})"
                )
            }

            // 2. Cryptographic InfoHash match
            val candidateHash = (candidate.source as? ReleaseSource.Torrent)?.infoHash
            val existingHash = (existing.source as? ReleaseSource.Torrent)?.infoHash
            if (candidateHash != null && existingHash != null && candidateHash == existingHash) {
                return DuplicateMatch(
                    duplicateType = DuplicateType.SameInfoHash,
                    confidence = DuplicateConfidence.Exact,
                    matchedAgainstId = existing.id.value,
                    explanation = "Identical BitTorrent InfoHash: ${candidateHash.hexString}"
                )
            }

            // 3. Exact Normalized Identity + Episode Range match
            if (candidate.identity.normalizedTitle.equals(existing.identity.normalizedTitle, ignoreCase = true) &&
                candidate.episodeRange != null && candidate.episodeRange == existing.episodeRange &&
                candidate.technical == existing.technical
            ) {
                return DuplicateMatch(
                    duplicateType = DuplicateType.SameNormalizedIdentity,
                    confidence = DuplicateConfidence.Strong,
                    matchedAgainstId = existing.id.value,
                    explanation = "Identical anime, episode range (${candidate.episodeRange}), and technical specifications"
                )
            }
        }
        return null
    }

    fun checkFileDuplicate(
        fingerprint: FileFingerprint,
        existingFiles: List<LibraryFile>
    ): DuplicateMatch? {
        for (file in existingFiles) {
            val existingFp = file.fingerprint ?: continue

            // Full cryptographic hash match
            if (fingerprint is FileFingerprint.FullHash && existingFp is FileFingerprint.FullHash) {
                if (fingerprint.hash.equals(existingFp.hash, ignoreCase = true)) {
                    return DuplicateMatch(
                        duplicateType = DuplicateType.SameFileFingerprint,
                        confidence = DuplicateConfidence.Exact,
                        matchedAgainstId = file.id.value,
                        explanation = "Full cryptographic hash match (${fingerprint.algorithm}: ${fingerprint.hash})"
                    )
                }
            }

            // Torrent bitfield match
            if (fingerprint is FileFingerprint.TorrentHash && existingFp is FileFingerprint.TorrentHash) {
                if (fingerprint.infoHash == existingFp.infoHash) {
                    return DuplicateMatch(
                        duplicateType = DuplicateType.SameFileFingerprint,
                        confidence = DuplicateConfidence.Exact,
                        matchedAgainstId = file.id.value,
                        explanation = "Matching torrent bitfield hash: ${fingerprint.infoHash.hexString}"
                    )
                }
            }

            // Size-only match (Weak / Possible)
            if (fingerprint is FileFingerprint.SizeOnly && existingFp is FileFingerprint.SizeOnly) {
                if (fingerprint.size == existingFp.size) {
                    return DuplicateMatch(
                        duplicateType = DuplicateType.PossibleDuplicate,
                        confidence = DuplicateConfidence.Possible,
                        matchedAgainstId = file.id.value,
                        explanation = "File size matches exactly (${fingerprint.size.toDisplayString()}), but hashes are unverified"
                    )
                }
            }
        }
        return null
    }
}
