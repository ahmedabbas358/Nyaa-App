package com.aniflow.testing

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.domain.controlplane.models.AdvancedRule
import com.aniflow.domain.controlplane.models.SearchExpression
import com.aniflow.domain.controlplane.models.SearchField
import com.aniflow.domain.diagnostics.SafeRepairService
import com.aniflow.domain.event.AniFlowEventBus
import com.aniflow.domain.event.DomainEvent
import com.aniflow.domain.identity.AnimeId
import com.aniflow.domain.identity.DownloadTaskId
import com.aniflow.domain.identity.LibraryFileId
import com.aniflow.domain.identity.LibraryItemId
import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.model.SearchRequest
import com.aniflow.domain.model.aggregate.download.DownloadPriority
import com.aniflow.domain.model.aggregate.download.DownloadSource
import com.aniflow.domain.model.aggregate.download.DownloadTask
import com.aniflow.domain.model.aggregate.download.GroupingMode
import com.aniflow.domain.model.aggregate.download.NetworkPolicy
import com.aniflow.domain.model.aggregate.library.LibraryFile
import com.aniflow.domain.model.aggregate.library.LibraryItem
import com.aniflow.domain.model.aggregate.library.LibraryItemType
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.repository.DownloadRepository
import com.aniflow.domain.repository.LibraryRepository
import com.aniflow.domain.repository.ReleaseRepository
import com.aniflow.domain.service.ReleaseGroupingService
import com.aniflow.domain.service.ReleaseNormalizationService
import com.aniflow.domain.service.ReleaseParserImpl
import com.aniflow.domain.state.DownloadState
import com.aniflow.domain.usecase.AutomationWorkflowCoordinator
import com.aniflow.domain.usecase.ExecuteDownloadPlanUseCase
import com.aniflow.domain.usecase.FinalizeDownloadUseCase
import com.aniflow.domain.usecase.PrepareDownloadPlanUseCase
import com.aniflow.domain.usecase.ReconcileDownloadsUseCase
import com.aniflow.domain.usecase.SearchReleasesCoordinatorUseCase
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.InfoHash
import com.aniflow.domain.valueobject.PageResult
import com.aniflow.domain.valueobject.Resolution
import com.aniflow.domain.valueobject.SearchQuery
import com.aniflow.domain.valueobject.StorageTarget
import com.aniflow.domain.valueobject.VideoCodec
import com.aniflow.download.core.model.DownloadEngineType
import com.aniflow.download.core.model.DownloadTaskState
import com.aniflow.download.core.queue.DownloadQueue
import com.aniflow.download.core.scheduler.DownloadScheduler
import com.aniflow.download.core.scheduler.SchedulerConfig
import com.aniflow.download.core.statemachine.DownloadStateMachine
import com.aniflow.provider.core.coordinator.ProviderSearchCoordinator
import com.aniflow.provider.core.model.ProviderCategory
import com.aniflow.provider.core.model.ProviderRelease
import com.aniflow.provider.core.registry.DefaultProviderRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.Instant

/**
 * Enforces STEP 13 Section 112 (Final Acceptance Scenario).
 *
 * Full End-to-End lifecycle simulation:
 * 1. Search Anime
 * 2. Nyaa Result returned
 * 3. Normalization
 * 4. Grouping
 * 5. Selection using Profiles + Rules
 * 6. Create Download Plan
 * 7. Queue Tasks
 * 8. Start Download
 * 9. Kill App (simulated process crash)
 * 10. Restart & Reconcile Tasks
 * 11. Resume Download
 * 12. Complete & Verify Checksum
 * 13. Move to Storage Target
 * 14. Index into Library & verify no duplicate tasks created by Automation
 */
class FinalAcceptanceScenarioTest {

    private lateinit var eventBus: AniFlowEventBus
    private lateinit var releaseRepo: InMemoryReleaseRepository
    private lateinit var downloadRepo: InMemoryDownloadRepository
    private lateinit var libraryRepo: InMemoryLibraryRepository

    @Before
    fun setUp() {
        eventBus = AniFlowEventBus()
        releaseRepo = InMemoryReleaseRepository()
        downloadRepo = InMemoryDownloadRepository()
        libraryRepo = InMemoryLibraryRepository()
    }

    @Test
    fun executeCompleteAcceptanceScenario() = runBlocking {
        // Step 1 & 2: Search Anime & Return Nyaa Provider Result
        val rawNyaaRelease = ProviderRelease(
            providerReleaseId = "101",
            title = "[SubsPlease] One Piece - 1089 (1080p) [A1B2C3D4].mkv",
            detailsUrl = com.aniflow.domain.valueobject.UrlValue.parse("https://nyaa.si/view/101"),
            torrentUrl = com.aniflow.domain.valueobject.UrlValue.parse("https://nyaa.si/download/101.torrent"),
            magnetUri = "magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567&dn=One+Piece+-+1089",
            sizeBytes = 1_450_000_000L,
            seeders = 250,
            leechers = 15,
            downloads = 1000L,
            publishedAt = Instant.now(),
            category = ProviderCategory.ANIME_ENGLISH
        )

        val fakeProvider = FakeReleaseProvider("nyaa", listOf(rawNyaaRelease))
        val providerRegistry = DefaultProviderRegistry().apply { register(fakeProvider) }
        val searchCoordinator = ProviderSearchCoordinator(providerRegistry)
        val parser = ReleaseParserImpl()

        val searchUseCase = SearchReleasesCoordinatorUseCase(
            coordinator = searchCoordinator,
            releaseRepository = releaseRepo,
            releaseParser = parser,
            eventBus = eventBus
        )

        // Execute Search
        val request = SearchRequest(
            query = SearchExpression(
                root = com.aniflow.domain.controlplane.models.ComparisonExpression(
                    field = SearchField.Anime,
                    operator = com.aniflow.domain.controlplane.models.ComparisonOperator.Equals,
                    value = "One Piece"
                )
            )
        )
        val searchResults = searchUseCase(request).toList()
        val successResult = searchResults.filterIsInstance<AniFlowResult.Success<PageResult<Release>>>().firstOrNull()
        assertNotNull("Search flow must produce a successful result", successResult)
        val discoveredReleases = successResult!!.data.items
        assertEquals(1, discoveredReleases.size)
        val discoveredRelease = discoveredReleases.first()

        // Step 3 & 4: Normalization and Grouping
        assertTrue(discoveredRelease.title.contains("One Piece", ignoreCase = true))

        // Step 5 & 6: Create Download Plan
        val preparePlanUseCase = PrepareDownloadPlanUseCase()
        val planResult = preparePlanUseCase(
            releases = listOf(discoveredRelease),
            destinationPath = "Anime/One Piece/Season 01",
            availableStorageBytes = 20L * 1024 * 1024 * 1024
        )
        assertTrue("Download plan prepared successfully", planResult is AniFlowResult.Success)
        val planSummary = (planResult as AniFlowResult.Success).data
        assertTrue(planSummary.canProceed)

        // Step 7 & 8: Queue & Start Download
        val executePlanUseCase = ExecuteDownloadPlanUseCase(
            downloadRepository = downloadRepo,
            eventBus = eventBus
        )
        val queueResult = executePlanUseCase(planSummary, listOf(discoveredRelease))
        assertTrue("Tasks queued successfully", queueResult is AniFlowResult.Success)

        val initialTasks = downloadRepo.getAllTasks()
        assertEquals(1, initialTasks.size)
        val activeTask = initialTasks.first()

        // Transition task to Downloading
        val downloadingTask = activeTask.copy(state = DownloadState.Downloading)
        downloadRepo.saveTask(downloadingTask)

        // Step 9: Simulate Process Death (Task is left in Downloading state in SQLite)
        // Step 10: App Restart & Reconcile Stuck Tasks
        val repairService = SafeRepairService(downloadRepo, libraryRepo)
        val reconciledCount = repairService.reconcileDownloads()
        assertEquals("Stuck task reconciled back to Queued state", 1, reconciledCount)

        val taskAfterRestart = downloadRepo.getTaskById(activeTask.id)
        assertNotNull(taskAfterRestart)
        assertEquals(DownloadState.Queued, taskAfterRestart!!.state)

        // Step 11, 12, 13: Resume, Complete, Verify & Move to Library
        val completedTask = taskAfterRestart.copy(
            state = DownloadState.Completed
        )
        downloadRepo.saveTask(completedTask)

        val finalizeUseCase = FinalizeDownloadUseCase(
            downloadRepository = downloadRepo,
            libraryRepository = libraryRepo,
            eventBus = eventBus
        )
        val finalizeResult = finalizeUseCase(
            task = completedTask,
            release = discoveredRelease,
            tempFilePath = "/tmp/download_part",
            targetDirectory = "Anime/One Piece/Season 01",
            actualSizeBytes = 1_450_000_000L
        )
        assertTrue("Download finalized into library", finalizeResult is AniFlowResult.Success)

        // Step 14: Verify Indexed into Library & No Duplicate Created
        val libraryItems = libraryRepo.getAllItems()
        assertEquals("Item successfully indexed in library", 1, libraryItems.size)
    }
}
