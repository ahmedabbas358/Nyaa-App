package com.aniflow.domain

import com.aniflow.domain.identity.FileFingerprint
import com.aniflow.domain.identity.LibraryFileId
import com.aniflow.domain.identity.LibraryItemId
import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.identity.ReleaseIdentity
import com.aniflow.domain.model.aggregate.library.LibraryFile
import com.aniflow.domain.model.aggregate.release.ProviderRef
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.model.aggregate.release.ReleaseSource
import com.aniflow.domain.service.DuplicateDetectionService
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.DuplicateConfidence
import com.aniflow.domain.valueobject.DuplicateType
import com.aniflow.domain.valueobject.EpisodeRange
import com.aniflow.domain.valueobject.InfoHash
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateDetectionTest {

    private val provider = ProviderRef(ProviderId("nyaa"), "Nyaa")

    @Test
    fun `Detects exact duplicate by InfoHash`() {
        val hash = InfoHash("0123456789abcdef0123456789abcdef01234567")
        val r1 = Release(
            id = ReleaseId("r1"),
            provider = provider,
            providerReleaseId = "100",
            title = "Anime 01",
            normalizedTitle = "anime 01",
            source = ReleaseSource.Torrent(infoHash = hash),
            identity = ReleaseIdentity(provider.providerId, "100", hash, null, "anime 01", EpisodeRange.ofSingle(1), null)
        )
        val r2 = Release(
            id = ReleaseId("r2"),
            provider = provider,
            providerReleaseId = "200", // different provider ID but identical hash
            title = "Anime 01 Repack",
            normalizedTitle = "anime 01 repack",
            source = ReleaseSource.Torrent(infoHash = hash),
            identity = ReleaseIdentity(provider.providerId, "200", hash, null, "anime 01 repack", EpisodeRange.ofSingle(1), null)
        )

        val match = DuplicateDetectionService.checkDuplicate(r2, listOf(r1))
        assertNotNull(match)
        assertEquals(DuplicateType.SameInfoHash, match?.duplicateType)
        assertEquals(DuplicateConfidence.Exact, match?.confidence)
        assertEquals("r1", match?.matchedAgainstId)
    }

    @Test
    fun `Detects file fingerprint full hash vs weak size-only match`() {
        val existing = LibraryFile(
            id = LibraryFileId("f1"),
            libraryItemId = LibraryItemId("item1"),
            path = "Anime/OnePiece/01.mkv",
            fileName = "01.mkv",
            size = ByteSize.ofBytes(104857600L),
            fingerprint = FileFingerprint.FullHash("SHA-256", "abc123hash")
        )

        // Matching full hash -> Exact
        val exactCand = FileFingerprint.FullHash("SHA-256", "abc123hash")
        val matchExact = DuplicateDetectionService.checkFileDuplicate(exactCand, listOf(existing))
        assertNotNull(matchExact)
        assertEquals(DuplicateConfidence.Exact, matchExact?.confidence)

        // Matching size only -> Possible (requires confirmation)
        val sizeCand = FileFingerprint.SizeOnly(ByteSize.ofBytes(104857600L))
        val matchPossible = DuplicateDetectionService.checkFileDuplicate(sizeCand, listOf(existing))
        assertNotNull(matchPossible)
        assertEquals(DuplicateConfidence.Possible, matchPossible?.confidence)
        assertTrue(matchPossible?.requiresManualConfirmation == true)
    }
}
