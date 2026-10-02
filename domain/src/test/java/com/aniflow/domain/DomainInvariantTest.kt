package com.aniflow.domain

import com.aniflow.domain.error.InvalidStateTransitionException
import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.state.DownloadState
import com.aniflow.domain.state.DownloadStateMachine
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.EpisodeNumber
import com.aniflow.domain.valueobject.EpisodeRange
import com.aniflow.domain.valueobject.ParseInfo
import com.aniflow.domain.valueobject.SeasonNumber
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests enforcing domain invariants (Section 97, 98, 142).
 * Verifies that invalid domain states are strictly unrepresentable.
 */
class DomainInvariantTest {

    @Test
    fun `ByteSize rejects negative values`() {
        assertThrows(IllegalArgumentException::class.java) {
            ByteSize(-1L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ByteSize(-1024L)
        }
    }

    @Test
    fun `ByteSize correctly executes arithmetic and formatting`() {
        val size1 = ByteSize.ofBytes(1024L)
        val size2 = ByteSize.ofBytes(2048L)
        val sum = size1 + size2
        assertEquals(3072L, sum.bytes)

        val diff = size2 - size1
        assertEquals(1024L, diff.bytes)

        val underflow = size1 - size2
        assertEquals(0L, underflow.bytes) // Coerced to 0, never negative

        val zero = ByteSize.ZERO
        assertEquals("0 B", zero.toDisplayString())
    }

    @Test
    fun `EpisodeRange rejects start greater than end`() {
        assertThrows(IllegalArgumentException::class.java) {
            EpisodeRange.Range(
                start = EpisodeNumber.of(12),
                end = EpisodeNumber.of(1)
            )
        }
    }

    @Test
    fun `EpisodeRange correctly represents single, batch, and containment`() {
        val single = EpisodeRange.ofSingle(5)
        assertTrue(single.isSingle)
        assertFalse(single.isBatch)
        assertEquals(1, single.count)
        assertTrue(single.contains(EpisodeNumber.of(5)))
        assertFalse(single.contains(EpisodeNumber.of(6)))

        val batch = EpisodeRange.ofRange(1, 12)
        assertFalse(batch.isSingle)
        assertTrue(batch.isBatch)
        assertEquals(12, batch.count)
        assertTrue(batch.contains(EpisodeNumber.of(1)))
        assertTrue(batch.contains(EpisodeNumber.of(6)))
        assertTrue(batch.contains(EpisodeNumber.of(12)))
        assertFalse(batch.contains(EpisodeNumber.of(13)))
        assertEquals(12, batch.toList().size)
    }

    @Test
    fun `EpisodeNumber rejects negative major or minor numbers`() {
        assertThrows(IllegalArgumentException::class.java) {
            EpisodeNumber(major = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            EpisodeNumber(major = 1, minor = -2)
        }
    }

    @Test
    fun `SeasonNumber rejects negative main numbers`() {
        assertThrows(IllegalArgumentException::class.java) {
            SeasonNumber.Main(-1)
        }
    }

    @Test
    fun `ParseInfo confidence strictly between 0 and 1`() {
        assertThrows(IllegalArgumentException::class.java) {
            ParseInfo(parserVersion = "1.0", confidence = -0.1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ParseInfo(parserVersion = "1.0", confidence = 1.01)
        }
        val valid = ParseInfo(parserVersion = "1.0", confidence = 0.85)
        assertEquals(0.85, valid.confidence, 0.001)
    }

    @Test
    fun `TypedIds reject blank strings`() {
        assertThrows(IllegalArgumentException::class.java) {
            AnimeId("   ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReleaseId("")
        }
        assertThrows(IllegalArgumentException::class.java) {
            EpisodeId("")
        }
    }

    @Test
    fun `DownloadStateMachine rejects invalid transitions`() {
        // Completed cannot transition to Downloading
        assertThrows(InvalidStateTransitionException::class.java) {
            DownloadStateMachine.transition(DownloadState.Completed, DownloadState.Downloading)
        }
        // Cancelled cannot transition to Downloading
        assertThrows(InvalidStateTransitionException::class.java) {
            DownloadStateMachine.transition(DownloadState.Cancelled, DownloadState.Downloading)
        }
        // Removed cannot transition to Downloading
        assertThrows(InvalidStateTransitionException::class.java) {
            DownloadStateMachine.transition(DownloadState.Removed, DownloadState.Downloading)
        }
    }
}
