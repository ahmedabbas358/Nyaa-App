package com.aniflow.testing

import com.aniflow.download.core.organization.CollisionPolicy
import com.aniflow.download.core.organization.FileCollisionEngine
import com.aniflow.download.core.organization.FileMediaAttributes
import com.aniflow.download.core.organization.MediaNamingContext
import com.aniflow.download.core.organization.NamingTemplateEngine
import com.aniflow.feature.library.parser.MediaFilenameParser
import com.aniflow.feature.library.probe.DefaultMediaProbe
import com.aniflow.feature.library.reconciliation.DatabaseFileRecord
import com.aniflow.feature.library.reconciliation.LibraryReconciliationEngine
import com.aniflow.feature.library.reconciliation.ReconciliationState
import com.aniflow.feature.library.resolver.LibraryIdentityResolver
import com.aniflow.feature.library.resolver.MatchSource
import com.aniflow.feature.library.scanner.LibraryScanner
import com.aniflow.feature.library.scanner.ScannedFile
import com.aniflow.feature.library.search.LibraryFilterCriteria
import com.aniflow.feature.library.search.LibrarySearchEngine
import com.aniflow.feature.library.search.SearchableLibraryItem
import com.aniflow.platform.storage.StorageHealthLevel
import com.aniflow.platform.storage.StorageHealthMonitor
import com.aniflow.platform.storage.StorageReservationManager
import com.aniflow.platform.storage.model.StorageFile
import com.aniflow.platform.storage.model.StorageLocation
import com.aniflow.platform.storage.model.StorageLocationId
import com.aniflow.platform.storage.model.StorageLocationType
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class StorageAndLibraryTests {

    @Test
    fun testNamingTemplateEngineFormatsCleanFilename() {
        val engine = NamingTemplateEngine()
        val context = MediaNamingContext(
            anime = "One Piece",
            season = 1,
            episode = 3.0,
            episodeTitle = "Enter Zoro",
            resolution = "1080p",
            codec = "HEVC",
            group = "Erai-raws",
            extension = "mkv"
        )

        val formatted = engine.formatFileName(context)
        assertEquals("S01E03 - Enter Zoro [1080p].mkv", formatted)

        val customFormatted = engine.formatFileName(
            context,
            template = "[{group}] {anime} - S{season}E{episode} [{resolution}][{codec}]"
        )
        assertEquals("[Erai-raws] One Piece - S01E03 [1080p][HEVC].mkv", customFormatted)
    }

    @Test
    fun testNamingTemplateEngineSanitizesIllegalCharacters() {
        val engine = NamingTemplateEngine()
        val context = MediaNamingContext(
            anime = "Fate/stay night: Heaven's Feel *Special?*",
            season = 1,
            episode = 1.0,
            episodeTitle = "What <is> \"Fate\" | Part 1",
            extension = "mkv"
        )

        val formatted = engine.formatFileName(context)
        // Check that none of the forbidden characters remain: / : * ? " < > |
        assertFalse(formatted.contains('/'))
        assertFalse(formatted.contains(':'))
        assertFalse(formatted.contains('*'))
        assertFalse(formatted.contains('?'))
        assertFalse(formatted.contains('"'))
        assertFalse(formatted.contains('<'))
        assertFalse(formatted.contains('>'))
        assertFalse(formatted.contains('|'))
    }

    @Test
    fun testDirectoryTemplateEngineNormalizesDots() {
        val engine = NamingTemplateEngine()
        val context = MediaNamingContext(
            anime = "One.Piece.Special",
            season = 2
        )

        val dir = engine.formatDirectoryPath(context)
        assertEquals("One Piece Special/Season 02", dir)
    }

    @Test
    fun testCollisionEngineKeepBothPolicy() {
        val engine = FileCollisionEngine()
        val existing = StorageFile(
            name = "Episode 01.mkv",
            relativePath = "Anime/One Piece/Episode 01.mkv",
            persistentUri = "",
            sizeBytes = 1000L
        )

        val incomingAttrs = FileMediaAttributes("Episode 01.mkv", 1200L, System.currentTimeMillis())
        val resolution = engine.resolveCollision(
            incomingName = "Episode 01.mkv",
            existingFile = existing,
            existingAttributes = null,
            incomingAttributes = incomingAttrs,
            preferredPolicy = CollisionPolicy.KeepBoth
        )

        assertEquals("Episode 01 (1).mkv", resolution.resolvedFileName)
        assertTrue(resolution.proceedWithTransfer)
    }

    @Test
    fun testCollisionEngineAttributeComparison() {
        val engine = FileCollisionEngine()
        val existing = FileMediaAttributes(
            fileName = "Episode 01.mkv",
            sizeBytes = 500_000_000L,
            modifiedEpochMillis = 0L,
            resolution = "720p",
            codec = "H.264"
        )
        val incoming = FileMediaAttributes(
            fileName = "Episode 01.mkv",
            sizeBytes = 1_400_000_000L,
            modifiedEpochMillis = 0L,
            resolution = "1080p",
            codec = "HEVC"
        )

        val comparison = engine.compareAttributes(existing, incoming)
        assertTrue(comparison.hasHigherResolution)
        assertTrue(comparison.hasBetterCodec)
        assertEquals(CollisionPolicy.Overwrite, comparison.recommendedAction)
    }

    @Test
    fun testStorageHealthMonitorThresholds() {
        val monitor = StorageHealthMonitor()
        val location = StorageLocation(
            id = StorageLocationId("sd-1"),
            type = StorageLocationType.UserSelectedTree,
            displayName = "SD Card",
            persistentReference = "content://..."
        )

        // 100GB total, 50GB free = 50% -> Healthy
        val health1 = monitor.evaluateStorage(location, 100_000_000_000L, 50_000_000_000L)
        assertEquals(StorageHealthLevel.Healthy, health1.level)

        // 100GB total, 15GB free = 15% -> Warning (<20%)
        val health2 = monitor.evaluateStorage(location, 100_000_000_000L, 15_000_000_000L)
        assertEquals(StorageHealthLevel.Warning, health2.level)

        // 100GB total, 8GB free = 8% -> Low (<10%)
        val health3 = monitor.evaluateStorage(location, 100_000_000_000L, 8_000_000_000L)
        assertEquals(StorageHealthLevel.Low, health3.level)

        // 100GB total, 3GB free = 3% -> Critical (<5%)
        val health4 = monitor.evaluateStorage(location, 100_000_000_000L, 3_000_000_000L)
        assertEquals(StorageHealthLevel.Critical, health4.level)
        assertFalse(health4.isWritable)
    }

    @Test
    fun testStorageReservationManager() = runBlocking {
        val reservationManager = StorageReservationManager()
        val locId = StorageLocationId("internal")

        // 10GB available on disk
        val available = 10_000_000_000L

        // Reserve 4GB for Task A -> Success
        val r1 = reservationManager.reserve("task-A", locId, 4_000_000_000L, available)
        assertTrue(r1)

        // Reserve 5GB for Task B -> Success (4 + 5 = 9 <= 10)
        val r2 = reservationManager.reserve("task-B", locId, 5_000_000_000L, available)
        assertTrue(r2)

        // Reserve 2GB for Task C -> Fails because only 1GB unreserved space remaining!
        val r3 = reservationManager.reserve("task-C", locId, 2_000_000_000L, available)
        assertFalse(r3)

        // Release Task A -> frees 4GB
        reservationManager.release("task-A")

        // Now Task C can be accommodated!
        val r4 = reservationManager.reserve("task-C", locId, 2_000_000_000L, available)
        assertTrue(r4)
    }

    @Test
    fun testMediaFilenameParserExtraction() {
        val parser = MediaFilenameParser()

        val parsed1 = parser.parse("[SubsPlease] One Piece - 1050 (1080p) [HEVC].mkv")
        assertEquals("One Piece", parsed1.animeTitle)
        assertEquals(1050.0, parsed1.episodeNumber)
        assertEquals("1080P", parsed1.resolution)
        assertEquals("HEVC", parsed1.codec)
        assertFalse(parsed1.isAmbiguous)

        val parsed2 = parser.parse("Bleach S02E15 [720p].mp4")
        assertEquals("Bleach", parsed2.animeTitle)
        assertEquals(2, parsed2.seasonNumber)
        assertEquals(15.0, parsed2.episodeNumber)
        assertFalse(parsed2.isAmbiguous)

        // Ambiguous standalone file with no parent folder
        val ambiguous = parser.parse("Episode 03.mkv", parentFolderName = null)
        assertTrue(ambiguous.isAmbiguous)
    }

    @Test
    fun testLibraryIdentityResolverUnidentifiedHandling() {
        val resolver = LibraryIdentityResolver()
        val ambiguousFile = ScannedFile(
            name = "Episode 03.mkv",
            relativePath = "Downloads/Episode 03.mkv",
            sizeBytes = 500_000_000L,
            modifiedEpochMillis = 0L,
            cheapFingerprint = "500_0",
            parentFolder = "Downloads"
        )

        val resolved = resolver.resolve(ambiguousFile)
        assertEquals("Unidentified", resolved.animeTitle)
        assertTrue(resolved.isUnidentified)
        assertEquals(MatchSource.UnidentifiedFallback, resolved.matchSource)
    }

    @Test
    fun testLibraryReconciliationDetectsAllStates() {
        val engine = LibraryReconciliationEngine()

        val dbRecords = listOf(
            DatabaseFileRecord("1", "item1", "Anime/OP/01.mkv", "01.mkv", 100L, "100_10"),
            DatabaseFileRecord("2", "item1", "Anime/OP/02.mkv", "02.mkv", 100L, "100_10"),
            DatabaseFileRecord("3", "item1", "Anime/OP/03.mkv", "03.mkv", 100L, "100_10")
        )

        val scannedFiles = listOf(
            // 01 is exact synced match
            ScannedFile("01.mkv", "Anime/OP/01.mkv", 100L, 10L, "100_10"),
            // 02 is modified externally (fingerprint changed)
            ScannedFile("02.mkv", "Anime/OP/02.mkv", 120L, 20L, "120_20"),
            // 03 was moved to "Anime/One Piece/03.mkv" (same fingerprint)
            ScannedFile("03.mkv", "Anime/One Piece/03.mkv", 100L, 10L, "100_10"),
            // 04 is newly added / unindexed
            ScannedFile("04.mkv", "Anime/OP/04.mkv", 150L, 30L, "150_30")
        )

        val report = engine.reconcile(dbRecords, scannedFiles)
        assertEquals(1, report.syncedCount)
        assertEquals(1, report.modifiedCount)
        assertEquals(1, report.movedCandidatesCount)
        assertEquals(1, report.unindexedCount)
    }

    @Test
    fun testLibraryIncrementalScannerBatching() = runBlocking {
        val probe = DefaultMediaProbe()
        val scanner = LibraryScanner(probe)

        val files = (1..25).map { i ->
            StorageFile(
                name = "Episode $i.mkv",
                relativePath = "Anime/Show/Episode $i.mkv",
                persistentUri = "content://...",
                sizeBytes = 1000L * i,
                lastModified = Instant.ofEpochMilli(1000L * i)
            )
        }

        // Known fingerprint for file 1: should be skipped incrementally
        val known = setOf("1000_1000")

        // Batch size of 10
        val batches = scanner.scanIncrementally(files, known, batchSize = 10).toList()

        assertTrue(batches.isNotEmpty())
        val finalProgress = batches.last()
        assertTrue(finalProgress.isComplete)
        assertEquals(25, finalProgress.totalDiscovered)
        assertEquals(25, finalProgress.processedCount)
    }

    @Test
    fun testLibrarySearchAndDuplicateEpisodes() {
        val searchEngine = LibrarySearchEngine()

        val items = listOf(
            SearchableLibraryItem(
                id = "1",
                animeTitle = "One Piece",
                seasonNumber = 1,
                episodeNumber = 1.0,
                episodeTitle = "Romance Dawn",
                fileName = "One Piece - 01 [720p].mkv",
                path = "Anime/One Piece/01.mkv",
                storageLocationId = "loc-internal",
                sizeBytes = 500_000_000L,
                dateAddedEpochMillis = 100L,
                lastModifiedEpochMillis = 100L
            ),
            SearchableLibraryItem(
                id = "2",
                animeTitle = "One Piece",
                seasonNumber = 1,
                episodeNumber = 1.0,
                episodeTitle = "Romance Dawn",
                fileName = "One Piece - 01 [1080p].mkv",
                path = "Anime/One Piece/01_1080p.mkv",
                storageLocationId = "loc-sd",
                sizeBytes = 1_400_000_000L,
                dateAddedEpochMillis = 200L,
                lastModifiedEpochMillis = 200L
            ),
            SearchableLibraryItem(
                id = "3",
                animeTitle = "Naruto",
                seasonNumber = 1,
                episodeNumber = 1.0,
                episodeTitle = "Enter Naruto",
                fileName = "Naruto - 01.mkv",
                path = "Anime/Naruto/01.mkv",
                storageLocationId = "loc-internal",
                sizeBytes = 400_000_000L,
                dateAddedEpochMillis = 300L,
                lastModifiedEpochMillis = 300L
            )
        )

        // Query search
        val filtered = searchEngine.filterAndSort(items, LibraryFilterCriteria(query = "Piece"))
        assertEquals(2, filtered.size)

        // Duplicates detection
        val duplicates = searchEngine.findDuplicateEpisodes(items)
        assertEquals(1, duplicates.size)
        val dupGroup = duplicates.first()
        assertEquals("One Piece", dupGroup.animeTitle)
        assertEquals(1.0, dupGroup.episodeNumber, 0.0)
        assertEquals(2, dupGroup.files.size)
    }
}
