package com.aniflow.domain.library

import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.EpisodeId
import com.aniflow.domain.identity.FileFingerprint
import com.aniflow.domain.identity.LibraryFileId
import com.aniflow.domain.identity.LibraryItemId
import com.aniflow.domain.identity.SeasonId
import com.aniflow.domain.identity.StorageId
import com.aniflow.domain.library.duplicates.DuplicateMediaDetector
import com.aniflow.domain.library.matching.DownloadTaskIdentityContext
import com.aniflow.domain.library.matching.LibraryIdentityResolver
import com.aniflow.domain.library.matching.MatchConfidence
import com.aniflow.domain.library.matching.MatchEvidenceSource
import com.aniflow.domain.library.matching.MatchState
import com.aniflow.domain.library.matching.UserLibraryMapping
import com.aniflow.domain.library.model.DuplicateMediaType
import com.aniflow.domain.library.model.DuplicateResolutionRecommendation
import com.aniflow.domain.library.model.LibraryFile
import com.aniflow.domain.library.model.LibraryFileState
import com.aniflow.domain.library.model.LibraryItem
import com.aniflow.domain.library.model.LibraryItemType
import com.aniflow.domain.library.parser.LibraryFilenameParser
import com.aniflow.domain.library.probe.DefaultMediaProbe
import com.aniflow.domain.library.reconciliation.LibraryReconciliationEngine
import com.aniflow.domain.library.reconciliation.ReconciliationItemResult
import com.aniflow.domain.library.scanner.DefaultLibraryScanner
import com.aniflow.domain.library.scanner.DiscoveredPhysicalFile
import com.aniflow.domain.library.scanner.ScanMode
import com.aniflow.domain.organization.engine.OrganizationEngine
import com.aniflow.domain.organization.model.CollisionPolicy
import com.aniflow.domain.organization.model.NamingContext
import com.aniflow.domain.organization.model.NamingTemplate
import com.aniflow.domain.organization.model.OrganizationOperation
import com.aniflow.domain.organization.naming.NamingTemplateEngine
import com.aniflow.domain.storage.analytics.StorageAnalyticsEngine
import com.aniflow.domain.storage.model.StorageAvailability
import com.aniflow.domain.storage.model.StorageLocation
import com.aniflow.domain.storage.model.StorageLocationRoot
import com.aniflow.domain.storage.model.StorageRoot
import com.aniflow.domain.storage.model.StorageType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Comprehensive Test Suite for Step 23 (Definition of Done 1 to 33).
 */
class LibraryOrganizationTestSuite {

    // --- 1. Filename & Folder Parser Tests ---

    @Test
    fun `test library filename parser extracts anime, season, and episode from hierarchical path`() {
        val parser = LibraryFilenameParser()
        val result = parser.parse(
            fileName = "S02E03 - One Piece [1080p HEVC].mkv",
            parentFolder = "Season 02",
            grandparentFolder = "One Piece"
        )

        assertEquals("One Piece", result.animeTitleCandidate)
        assertEquals(2, result.seasonNumber)
        assertEquals(3.0, result.episodeNumber!!, 0.001)
        assertEquals("1080P", result.resolution)
        assertEquals("HEVC", result.codec)
        assertFalse(result.isAmbiguous)
    }

    @Test
    fun `test library filename parser flags ambiguous episode without anime title`() {
        val parser = LibraryFilenameParser()
        val result = parser.parse(
            fileName = "Episode 03.mkv",
            parentFolder = "Downloads",
            grandparentFolder = null
        )

        // Must be flagged ambiguous so it enters Unidentified (Section 37)
        assertTrue(result.isAmbiguous)
        assertNull(result.animeTitleCandidate)
    }

    // --- 2. Identity Resolver & Priority Chain Tests ---

    @Test
    fun `test identity resolution prioritizes download task mapping over filename guessing`() {
        val resolver = LibraryIdentityResolver()
        val file = DiscoveredPhysicalFile(
            storageId = StorageId("storage-1"),
            relativePath = "Downloads/weird_filename_xyz.mkv",
            name = "weird_filename_xyz.mkv",
            sizeBytes = 1000L,
            modifiedEpochMillis = 1000L,
            cheapFingerprint = "1000_1000"
        )

        val taskMapping = DownloadTaskIdentityContext(
            animeId = AnimeId("anime_frieren"),
            animeTitle = "Sousou no Frieren",
            seasonId = SeasonId("frieren_s1"),
            seasonNumber = 1,
            episodeId = EpisodeId("frieren_s1_ep4"),
            episodeNumber = 4.0
        )

        val result = resolver.resolve(file = file, taskMapping = taskMapping)

        assertEquals(MatchState.Matched, result.state)
        assertEquals(MatchConfidence.Definite, result.confidence)
        assertEquals("Sousou no Frieren", result.anime?.title)
        assertEquals(MatchEvidenceSource.DownloadTaskMapping, result.evidence.first().source)
    }

    @Test
    fun `test identity resolution places ambiguous files into Unidentified`() {
        val resolver = LibraryIdentityResolver()
        val file = DiscoveredPhysicalFile(
            storageId = StorageId("storage-1"),
            relativePath = "Downloads/03.mkv",
            name = "03.mkv",
            sizeBytes = 1000L,
            modifiedEpochMillis = 1000L,
            cheapFingerprint = "1000_1000"
        )

        val result = resolver.resolve(file = file)

        // Section 37: Ambiguous -> State.Ambiguous (Unidentified)
        assertEquals(MatchState.Ambiguous, result.state)
        assertNull(result.anime)
    }

    @Test
    fun `test manual user mapping overrides filename parser`() {
        val resolver = LibraryIdentityResolver()
        val file = DiscoveredPhysicalFile(
            storageId = StorageId("storage-1"),
            relativePath = "CustomFolder/random_video.mkv",
            name = "random_video.mkv",
            sizeBytes = 1000L,
            modifiedEpochMillis = 1000L,
            cheapFingerprint = "1000_1000",
            parentFolder = "CustomFolder"
        )

        val userMapping = UserLibraryMapping(
            patternOrFolder = "CustomFolder",
            animeId = AnimeId("anime_custom"),
            animeTitle = "Custom Anime Title",
            seasonNumber = 1,
            episodeNumber = 1.0
        )

        val result = resolver.resolve(file = file, userMappings = listOf(userMapping))

        assertEquals(MatchState.UserMapped, result.state)
        assertEquals(MatchConfidence.Definite, result.confidence)
        assertEquals("Custom Anime Title", result.anime?.title)
    }

    // --- 3. MediaProbe Tests ---

    @Test
    fun `test media probe identifies formats and extracts resolution`() = runBlocking {
        val probe = DefaultMediaProbe()
        assertTrue(probe.isMediaFile("video.mkv"))
        assertTrue(probe.isMediaFile("video.mp4"))
        assertTrue(probe.isSubtitleFile("sub.ass"))
        assertTrue(probe.isSidecarFile("cover.jpg"))
        assertFalse(probe.isMediaFile("data.bin"))

        val location = StorageLocation(StorageId("s1"), "Anime/[SubsPlease] Show - 01 (1080p) [HEVC].mkv")
        val metadata = probe.inspect(location, fileSizeBytes = 1024L)

        assertEquals("1080p", metadata.resolutionLabel)
        assertEquals(1080, metadata.height)
        assertEquals("HEVC", metadata.videoCodec)
        assertEquals("MKV", metadata.container)
    }

    // --- 4. Duplicate Media Detector Tests ---

    @Test
    fun `test duplicate detector classifies exact duplicate`() {
        val detector = DuplicateMediaDetector()
        val fileA = LibraryFile(
            id = LibraryFileId("fa"),
            libraryItemId = LibraryItemId("item-1"),
            location = StorageLocation(StorageId("s1"), "A.mkv"),
            displayName = "Episode 01.mkv",
            sizeBytes = 2000L,
            modifiedAt = Instant.now(),
            mediaMetadata = null,
            fingerprint = FileFingerprint.FullHash("SHA256", "HASH123"),
            state = LibraryFileState.Present
        )
        val fileB = fileA.copy(
            id = LibraryFileId("fb"),
            location = StorageLocation(StorageId("s1"), "B.mkv")
        )

        val result = detector.compareFiles(fileA, fileB)
        assertNotNull(result)
        assertEquals(DuplicateMediaType.ExactDuplicate, result?.type)
        assertEquals(DuplicateResolutionRecommendation.KeepFileA, result?.recommendation)
    }

    @Test
    fun `test duplicate detector classifies technical duplicate as alternative version`() {
        val detector = DuplicateMediaDetector()
        val probe = DefaultMediaProbe()
        val meta1080Hevc = runBlocking {
            probe.inspect(StorageLocation(StorageId("s1"), "Ep1_1080p_HEVC.mkv"))
        }
        val meta1080H264 = runBlocking {
            probe.inspect(StorageLocation(StorageId("s1"), "Ep1_1080p_H264.mkv"))
        }

        val fileA = LibraryFile(
            id = LibraryFileId("fa"),
            libraryItemId = LibraryItemId("item-1"),
            location = StorageLocation(StorageId("s1"), "Ep1_1080p_HEVC.mkv"),
            displayName = "Ep1_1080p_HEVC.mkv",
            sizeBytes = 2000L,
            modifiedAt = Instant.now(),
            mediaMetadata = meta1080Hevc,
            fingerprint = null,
            state = LibraryFileState.Present
        )
        val fileB = LibraryFile(
            id = LibraryFileId("fb"),
            libraryItemId = LibraryItemId("item-1"),
            location = StorageLocation(StorageId("s1"), "Ep1_1080p_H264.mkv"),
            displayName = "Ep1_1080p_H264.mkv",
            sizeBytes = 1800L,
            modifiedAt = Instant.now(),
            mediaMetadata = meta1080H264,
            fingerprint = null,
            state = LibraryFileState.Present
        )

        val result = detector.compareFiles(fileA, fileB)
        assertNotNull(result)
        assertEquals(DuplicateMediaType.TechnicalDuplicate, result?.type)
        assertEquals(DuplicateResolutionRecommendation.KeepBoth, result?.recommendation)
    }

    // --- 5. Naming Template Engine Tests ---

    @Test
    fun `test naming template renders tokens and handles missing values gracefully without null`() {
        val engine = NamingTemplateEngine()
        val template = NamingTemplate("{anime}/Season {seasonNumber}/{episode} - {anime} [{resolution} {codec}].{ext}")
        val context = NamingContext(
            anime = "Bleach",
            seasonNumber = 2,
            episode = "03",
            resolution = "1080p",
            codec = "HEVC",
            originalFilename = "bleach_raw.mkv",
            extension = "mkv"
        )

        val rendered = engine.render(template, context)
        assertEquals("Bleach/Season 02/03 - Bleach [1080p HEVC].mkv", rendered)

        // Missing values test (Section 59)
        val contextMissing = NamingContext(
            anime = "One Piece",
            seasonNumber = 1,
            episode = "05",
            resolution = null, // missing!
            codec = null,      // missing!
            originalFilename = "op_05.mkv",
            extension = "mkv"
        )
        val renderedMissing = engine.render(template, contextMissing)
        assertFalse(renderedMissing.contains("null"))
        assertEquals("One Piece/Season 01/05 - One Piece.mkv", renderedMissing)
    }

    @Test
    fun `test naming template sanitizes path traversal attempts`() {
        val engine = NamingTemplateEngine()
        val dirty = "../../root/escape/../Anime/01.mkv"
        val sanitized = engine.sanitizePath(dirty)

        assertFalse(sanitized.contains(".."))
        assertFalse(sanitized.startsWith("/"))
        assertEquals("root/escape/Anime/01.mkv", sanitized)
    }

    // --- 6. Organization Engine & Safe Upgrade Tests ---

    @Test
    fun `test organization engine plans moves and disambiguates collisions`() {
        val engine = OrganizationEngine()
        val targetRoot = StorageRoot(
            id = StorageId("target_drive"),
            name = "Target Drive",
            type = StorageType.ExternalVolume,
            state = StorageAvailability.Available,
            location = StorageLocationRoot("/target")
        )

        val file = LibraryFile(
            id = LibraryFileId("file-1"),
            libraryItemId = LibraryItemId("item-1"),
            location = StorageLocation(StorageId("source_drive"), "temp.mkv"),
            displayName = "temp.mkv",
            sizeBytes = 1024L,
            modifiedAt = Instant.now(),
            mediaMetadata = null,
            fingerprint = null,
            state = LibraryFileState.Present
        )

        val context = NamingContext(
            anime = "Frieren",
            seasonNumber = 1,
            episode = "01",
            originalFilename = "temp.mkv",
            extension = "mkv"
        )

        // Target path already has a collision
        val existingPaths = setOf("frieren/season 01/01 - frieren.mkv")

        val plan = engine.createPlan(
            files = listOf(file to context),
            targetRoot = targetRoot,
            template = NamingTemplate("{anime}/Season {seasonNumber}/{episode} - {anime}.{ext}"),
            collisionPolicy = CollisionPolicy.KeepBoth,
            existingTargetPaths = existingPaths
        )

        assertEquals(1, plan.items.size)
        val item = plan.items.first()
        assertEquals(OrganizationOperation.CrossVolumeCopyVerifyDelete, item.operation)
        // Disambiguated with (1)
        assertEquals("Frieren/Season 01/01 - Frieren (1).mkv", item.targetPath.normalizedPath)
    }

    @Test
    fun `test safe upgrade keeps old file if new verification fails`() = runBlocking {
        val engine = OrganizationEngine()
        val oldLoc = StorageLocation(StorageId("s1"), "Anime/OnePiece_720p.mkv")
        val newLoc = StorageLocation(StorageId("s1"), "Anime/OnePiece_1080p.mkv")

        var oldDeleted = false

        // Verification fails (e.g. corrupted download)
        val upgradeResult = engine.executeSafeUpgrade(
            oldFileLocation = oldLoc,
            newFileLocation = newLoc,
            expectedSizeBytes = 2000L,
            verifyNewFile = { _, _ -> false }, // Fails verification!
            deleteOldFile = { oldDeleted = true; true }
        )

        assertFalse(upgradeResult)
        assertFalse("Old version must NOT be deleted if new file is invalid!", oldDeleted)
    }

    // --- 7. Reconciliation Engine Tests ---

    @Test
    fun `test reconciliation detects moved file via fingerprint`() {
        val reconciliation = LibraryReconciliationEngine()
        val root = StorageRoot(
            id = StorageId("root_1"),
            name = "Root 1",
            type = StorageType.AppPrivate,
            state = StorageAvailability.Available,
            location = StorageLocationRoot("/storage/anime")
        )

        val dbFile = LibraryFile(
            id = LibraryFileId("file-1"),
            libraryItemId = LibraryItemId("item-1"),
            location = StorageLocation(StorageId("root_1"), "OldFolder/Episode_01.mkv"),
            displayName = "Episode_01.mkv",
            sizeBytes = 1500L,
            modifiedAt = Instant.now(),
            mediaMetadata = null,
            fingerprint = FileFingerprint.FullHash("SHA256", "HASH_EPISODE_1"),
            state = LibraryFileState.Present
        )

        // Physical file disappeared from OldFolder, but is now present in NewFolder with same fingerprint
        val physicalDiscovered = listOf(
            DiscoveredPhysicalFile(
                storageId = StorageId("root_1"),
                relativePath = "NewFolder/Episode_01.mkv",
                name = "Episode_01.mkv",
                sizeBytes = 1500L,
                modifiedEpochMillis = 1000L,
                cheapFingerprint = "HASH_EPISODE_1",
                parentFolder = "NewFolder"
            )
        )

        val summary = reconciliation.reconcile(
            storageRoots = listOf(root),
            indexedFiles = listOf(dbFile),
            physicalFiles = physicalDiscovered
        )

        assertEquals(1, summary.movedCount)
        val moved = summary.results.first { it is ReconciliationItemResult.MovedFile } as ReconciliationItemResult.MovedFile
        assertEquals("NewFolder/Episode_01.mkv", moved.newLocation.normalizedPath)
    }

    @Test
    fun `test reconciliation marks files as Offline when storage is unavailable and never deletes`() {
        val reconciliation = LibraryReconciliationEngine()
        val offlineRoot = StorageRoot(
            id = StorageId("usb_drive"),
            name = "External USB",
            type = StorageType.ExternalVolume,
            state = StorageAvailability.Offline, // Offline!
            location = StorageLocationRoot("/usb")
        )

        val dbFile = LibraryFile(
            id = LibraryFileId("file-usb"),
            libraryItemId = LibraryItemId("item-1"),
            location = StorageLocation(StorageId("usb_drive"), "Episode_01.mkv"),
            displayName = "Episode_01.mkv",
            sizeBytes = 1500L,
            modifiedAt = Instant.now(),
            mediaMetadata = null,
            fingerprint = null,
            state = LibraryFileState.Present
        )

        val summary = reconciliation.reconcile(
            storageRoots = listOf(offlineRoot),
            indexedFiles = listOf(dbFile),
            physicalFiles = emptyList()
        )

        assertEquals(1, summary.offlineCount)
        assertEquals(0, summary.missingCount) // Must NOT be marked missing or deleted!
    }

    // --- 8. Storage Analytics & Scanner Tests ---

    @Test
    fun `test storage analytics calculates breakdowns and duplicate wasted space`() {
        val analyticsEngine = StorageAnalyticsEngine()

        val item1 = LibraryItem(
            id = LibraryItemId("item-1"),
            type = LibraryItemType.Anime,
            title = "Sousou no Frieren",
            animeId = AnimeId("frieren"),
            seasonId = SeasonId("s1"),
            episodeId = null,
            state = com.aniflow.domain.library.model.LibraryItemState.Indexed
        )

        val file1 = LibraryFile(
            id = LibraryFileId("f1"),
            libraryItemId = LibraryItemId("item-1"),
            location = StorageLocation(StorageId("s1"), "f1.mkv"),
            displayName = "f1.mkv",
            sizeBytes = 2L * 1024 * 1024 * 1024, // 2 GB
            modifiedAt = Instant.now(),
            mediaMetadata = null,
            fingerprint = null,
            state = LibraryFileState.Present
        )

        val file2 = LibraryFile(
            id = LibraryFileId("f2"),
            libraryItemId = LibraryItemId("item-1"),
            location = StorageLocation(StorageId("s1"), "f2.mkv"),
            displayName = "f2.mkv",
            sizeBytes = 2L * 1024 * 1024 * 1024, // 2 GB duplicate
            modifiedAt = Instant.now(),
            mediaMetadata = null,
            fingerprint = null,
            state = LibraryFileState.Present
        )

        val duplicateComparison = com.aniflow.domain.library.model.DuplicateComparison(
            fileA = file1,
            fileB = file2,
            type = DuplicateMediaType.ExactDuplicate,
            recommendation = DuplicateResolutionRecommendation.KeepFileA
        )

        val analytics = analyticsEngine.computeAnalytics(
            items = listOf(item1),
            files = listOf(file1, file2),
            duplicates = listOf(duplicateComparison)
        )

        assertEquals(4L * 1024 * 1024 * 1024, analytics.totalLibrarySizeBytes)
        assertEquals(4L * 1024 * 1024 * 1024, analytics.animeBreakdown["Sousou no Frieren"])
        assertEquals(2L * 1024 * 1024 * 1024, analytics.duplicateWastedBytes)
    }

    @Test
    fun `test library scanner incremental scan skips known fingerprints`() = runBlocking {
        val scanner = DefaultLibraryScanner(DefaultMediaProbe())
        val root = StorageRoot(
            id = StorageId("root_1"),
            name = "Root",
            type = StorageType.AppPrivate,
            state = StorageAvailability.Available,
            location = StorageLocationRoot("/storage")
        )

        val fileList = listOf(
            DiscoveredPhysicalFile(
                storageId = StorageId("root_1"),
                relativePath = "Anime/01.mkv",
                name = "01.mkv",
                sizeBytes = 1000L,
                modifiedEpochMillis = 500L,
                cheapFingerprint = "1000_500" // Known!
            ),
            DiscoveredPhysicalFile(
                storageId = StorageId("root_1"),
                relativePath = "Anime/02.mkv",
                name = "02.mkv",
                sizeBytes = 2000L,
                modifiedEpochMillis = 800L,
                cheapFingerprint = "2000_800" // New!
            )
        )

        val processedBatches = mutableListOf<DiscoveredPhysicalFile>()

        val result = scanner.scan(
            root = root,
            mode = ScanMode.IncrementalScan,
            knownFingerprints = setOf("1000_500"),
            fileLister = { _, _ -> fileList },
            onBatchProcessed = { batch -> processedBatches.addAll(batch) }
        )

        // Only the new file 02.mkv should be added in incremental scan
        assertEquals(1, result.added)
        assertEquals(1, processedBatches.size)
        assertEquals("02.mkv", processedBatches.first().name)
    }
}
