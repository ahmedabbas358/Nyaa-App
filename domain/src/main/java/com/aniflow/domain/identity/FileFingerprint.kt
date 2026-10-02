package com.aniflow.domain.identity

import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.InfoHash

/**
 * Represents the fingerprint and cryptographic identity of a local or remote file.
 * Prevents false claims of verification: a file is only cryptographically verified
 * if a FullHash or TorrentHash matches.
 */
sealed interface FileFingerprint {

    val isFullVerification: Boolean

    /**
     * Weak fingerprint based only on file length in bytes.
     */
    data class SizeOnly(val size: ByteSize) : FileFingerprint {
        override val isFullVerification: Boolean = false
    }

    /**
     * Fast partial hash sampled from beginning, middle, or end chunks (e.g. for quick pre-checks).
     */
    data class PartialHash(
        val algorithm: String,
        val hash: String,
        val sampledBytes: Long
    ) : FileFingerprint {
        init {
            require(hash.isNotBlank()) { "PartialHash string cannot be blank" }
            require(sampledBytes > 0) { "sampledBytes must be greater than zero" }
        }
        override val isFullVerification: Boolean = false
    }

    /**
     * Complete cryptographic hash over the entire file content (SHA-256, MD5, CRC32, etc.).
     */
    data class FullHash(
        val algorithm: String,
        val hash: String
    ) : FileFingerprint {
        init {
            require(algorithm.isNotBlank()) { "Algorithm cannot be blank" }
            require(hash.isNotBlank()) { "FullHash string cannot be blank" }
        }
        override val isFullVerification: Boolean = true
    }

    /**
     * Hash supplied directly by provider metadata (e.g. ED2K, CRC in release title).
     */
    data class ProviderHash(
        val providerName: String,
        val hash: String
    ) : FileFingerprint {
        init {
            require(hash.isNotBlank()) { "Provider hash cannot be blank" }
        }
        override val isFullVerification: Boolean = false
    }

    /**
     * Torrent bitfield / piece hash validation against the torrent metadata InfoHash.
     */
    data class TorrentHash(
        val infoHash: InfoHash,
        val pieceIndex: Int? = null,
        val isEntireFileVerified: Boolean = true
    ) : FileFingerprint {
        override val isFullVerification: Boolean = isEntireFileVerified
    }
}
