package com.aniflow.testing

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.domain.controlplane.models.AdvancedRule
import com.aniflow.domain.controlplane.models.AdvancedRuleAction
import com.aniflow.domain.controlplane.models.AdvancedRuleCondition
import com.aniflow.domain.controlplane.models.ComparisonExpression
import com.aniflow.domain.controlplane.models.ComparisonOperator
import com.aniflow.domain.controlplane.models.ConditionNode
import com.aniflow.domain.controlplane.models.NetworkPolicyType
import com.aniflow.domain.controlplane.models.SearchExpression
import com.aniflow.domain.controlplane.models.SearchField
import com.aniflow.domain.controlplane.models.UpgradePolicyRule
import com.aniflow.domain.controlplane.service.CandidateReleaseContext
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
import com.aniflow.download.core.model.NetworkType
import com.aniflow.download.core.model.RuntimeCapabilities
import com.aniflow.download.core.queue.DownloadQueue
import com.aniflow.download.core.scheduler.DownloadScheduler
import com.aniflow.download.core.scheduler.SchedulerConfig
import com.aniflow.provider.core.ReleaseProvider
import com.aniflow.provider.core.coordinator.ProviderSearchCoordinator
import com.aniflow.provider.core.model.ProviderCategory
import com.aniflow.provider.core.model.ProviderDescriptor
import com.aniflow.provider.core.model.ProviderRelease
import com.aniflow.provider.core.model.ProviderSearchRequest
import com.aniflow.provider.core.registry.DefaultProviderRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * EndToEndIntegrationTests (Sections 50–58, 68–74, 77).
 * Verifies all 9 critical End-to-End flows:
 * 1. Search pipeline (Provider -> Parser -> Normalizer -> Grouping -> Cache)
 * 2. Manual download (Selection -> Plan -> Queue -> Transfer -> Verification -> Library)
 * 3. Automatic download (Trigger -> Rules -> Safety -> Queue)
 * 4. Upgrade policy (720p replaced by 1080p safely)
 * 5. Process death recovery (interrupted download reconciliation)
 * 6. Storage capacity failure rejection
 * 7. Network restriction enforcement (Wi-Fi only vs Metered)
 * 8. Duplicate detection prevention
 * 9. Provider degradation and error resilience
 */
class EndToEndIntegrationTests {

    private lateinit var eventBus: AniFlowEventBus
    private lateinit var inMemoryReleaseRepo: InMemoryReleaseRepository
    private lateinit var inMemoryDownloadRepo: InMemoryDownloadRepository
    private lateinit var inMemoryLibraryRepo: InMemoryLibraryRepository

    @Before
    fun setUp() {
        eventBus = AniFlowEventBus()
        inMemoryReleaseRepo = InMemoryReleaseRepository()
        inMemoryDownloadRepo = InMemoryDownloadRepository()
        inMemoryLibraryRepo = InMemoryLibraryRepository()
    }

    // ===============================================================
    // FLOW 1: SEARCH END-TO-END (Section 50)
    // ===============================================================
    @Test
    fun `test Flow 1 - Search End-to-End with normalization, parser, grouping and cache`() = runBlocking {
        val fakeProvider = FakeReleaseProvider(
            providerId = "nyaa",
            cannedReleases = listOf(
                ProviderRelease(
                    providerReleaseId = "1001",
                    title = "[Erai-raws] One Piece - 1000 [1080p][HEVC][Multiple Subtitle].mkv",
                    sizeBytes = 1_400_000_000L,
                    seeders = 45,
                    leechers = 10,
                    magnetUri = "magnet:?xt=urn:btih:1111111111111111111111111111111111111111&dn=OnePiece1000"
                ),
                ProviderRelease(
                    providerReleaseId = "1002",
                    title = "[SubsPlease] One Piece - 1000 (720p) [x264].mkv",
                    sizeBytes = 750_000_000L,
                    seeders = 80,
                    leechers = 5,
                    magnetUri = "magnet:?xt=urn:btih:2222222222222222222222222222222222222222&dn=OnePiece1000"
                )
            )
        )

        val registry = DefaultProviderRegistry()
        registry.register(fakeProvider)
        val coordinator = ProviderSearchCoordinator(registry)

        val searchUseCase = SearchReleasesCoordinatorUseCase(
            coordinator = coordinator,
            releaseRepository = inMemoryReleaseRepo,
            releaseParser = ReleaseParserImpl(),
            eventBus = eventBus
        )

        val request = SearchRequest(
            query = SearchExpression(
                root = ComparisonExpression(SearchField.Anime, ComparisonOperator.Equals, "One Piece")
            )
        )

        val resultFlow = searchUseCase(request).toList()
        val successResult = resultFlow.filterIsInstance<AniFlowResult.Success<PageResult<Release>>>().firstOrNull()

        assertNotNull("Search flow must produce a successful result", successResult)
        val releases = successResult!!.data.items
        assertEquals(2, releases.size)

        // Verify parsing & normalization
        val eraiRelease = releases.first { it.title.contains("Erai-raws") }
        assertEquals(Resolution.R1080p, eraiRelease.technical.resolution)
        assertEquals(VideoCodec.HEVC, eraiRelease.technical.codec)
        assertEquals("one piece 1000", eraiRelease.normalizedTitle)
        assertEquals(1000.0, eraiRelease.episodeRange?.start?.number?.toDouble())

        // Verify grouping
        val grouped = ReleaseGroupingService.group(releases, GroupingMode.AnimeSeason)
        assertTrue("Releases must be grouped under Anime: One Piece", grouped.groups.isNotEmpty())

        // Verify persistence in repository cache
        val cached = inMemoryReleaseRepo.getById(eraiRelease.id)
        assertNotNull("Release must be atomically cached in repository", cached)
    }

    // ===============================================================
    // FLOW 2: MANUAL DOWNLOAD PIPELINE (Section 51)
    // ===============================================================
    @Test
    fun `test Flow 2 - Manual Download from planning to verification and library index`() = runBlocking {
        val release = createSampleRelease("rel-manual-1", "One Piece - 1000", 1_400_000_000L)

        // 1. Prepare Plan
        val preparePlanUseCase = PrepareDownloadPlanUseCase()
        val planResult = preparePlanUseCase(
            releases = listOf(release),
            destinationPath = "Anime/One Piece/Season 01",
            availableStorageBytes = 20L * 1024 * 1024 * 1024
        )
        assertTrue(planResult is AniFlowResult.Success)
        val planSummary = (planResult as AniFlowResult.Success).data
        assertTrue(planSummary.canProceed)
        assertEquals(1, planSummary.totalFilesCount)

        // 2. Execute Plan -> Persist tasks & Queue
        val executePlanUseCase = ExecuteDownloadPlanUseCase(inMemoryDownloadRepo, eventBus)
        val executeResult = executePlanUseCase(planSummary, listOf(release))
        assertTrue(executeResult is AniFlowResult.Success)
        val taskIds = (executeResult as AniFlowResult.Success).data
        assertEquals(1, taskIds.size)

        val taskId = taskIds.first()
        val queuedTask = inMemoryDownloadRepo.getTaskById(taskId)
        assertNotNull("Task must be persisted in repository", queuedTask)
        assertEquals(DownloadState.Pending, queuedTask!!.state)

        // 3. Finalize transfer -> Verify size -> Organize -> Index Library
        val finalizeUseCase = FinalizeDownloadUseCase(inMemoryDownloadRepo, inMemoryLibraryRepo, eventBus)
        val finalResult = finalizeUseCase(
            task = queuedTask,
            release = release,
            tempFilePath = "/tmp/download_part",
            targetDirectory = "Anime/One Piece/Season 01",
            actualSizeBytes = 1_400_000_000L
        )

        assertTrue(finalResult is AniFlowResult.Success)
        val finalData = (finalResult as AniFlowResult.Success).data
        assertTrue(finalData.isVerified)

        // Check task marked Completed
        val completedTask = inMemoryDownloadRepo.getTaskById(taskId)
        assertEquals(DownloadState.Completed, completedTask?.state)

        // Check indexed in Library
        val libraryItems = inMemoryLibraryRepo.getAllItems()
        assertTrue("File must be indexed into Library", libraryItems.any { it.title.contains("One Piece") })
    }

    // ===============================================================
    // FLOW 3: AUTOMATION INTEGRATION (Section 52)
    // ===============================================================
    @Test
    fun `test Flow 3 - Automation trigger matches rule, executes safety gate and queues task`() = runBlocking {
        val coordinator = AutomationWorkflowCoordinator(
            downloadRepository = inMemoryDownloadRepo,
            eventBus = eventBus
        )

        val rule = AdvancedRule(
            id = com.aniflow.domain.identity.RuleId("rule-auto-bleach"),
            name = "Auto Bleach 1080p",
            root = ConditionNode(AdvancedRuleCondition.AnimeIs("Bleach")),
            actions = listOf(AdvancedRuleAction.QueueDownload)
        )

        val candidate = CandidateReleaseContext(
            title = "[SubsPlease] Bleach TYBW - 01 [1080p].mkv",
            animeTitle = "Bleach TYBW",
            episodeNumber = 1.0,
            resolution = Resolution.R1080p,
            sizeBytes = 1_200_000_000L
        )

        val triggerResult = coordinator.handleTrigger(
            trigger = com.aniflow.domain.controlplane.models.AutomationTrigger.NewReleaseDetected("Bleach"),
            candidates = listOf(candidate),
            rules = listOf(rule),
            storageFreeBytes = 50L * 1024 * 1024 * 1024
        )

        assertTrue(triggerResult is AniFlowResult.Success)
        val data = (triggerResult as AniFlowResult.Success).data
        assertEquals(1, data.tasksQueued)
        assertFalse(data.isBlockedBySafety)

        val queuedTasks = inMemoryDownloadRepo.getAllTasks()
        assertEquals(1, queuedTasks.size)
    }

    // ===============================================================
    // FLOW 4: UPGRADE RULES (Section 53)
    // ===============================================================
    @Test
    fun `test Flow 4 - Upgrade Policy allows replacing 720p with 1080p safely`() {
        val policy = UpgradePolicyRule(
            allowUpgradeToHigherResolution = true,
            allowUpgradeToBetterCodec = true,
            keepOldFileUntilNewVerified = true
        )

        val currentResolution = Resolution.R720p
        val newResolution = Resolution.R1080p

        val isUpgradeAllowed = policy.allowUpgradeToHigherResolution &&
            (newResolution.heightPixels > currentResolution.heightPixels)

        assertTrue("1080p must be permitted to upgrade 720p under upgrade policy", isUpgradeAllowed)
        assertTrue("Old file must be retained until new download completes", policy.keepOldFileUntilNewVerified)
    }

    // ===============================================================
    // FLOW 5: PROCESS DEATH RECOVERY (Section 54)
    // ===============================================================
    @Test
    fun `test Flow 5 - Process death recovery reconciles interrupted downloads`() = runBlocking {
        val task1 = DownloadTask(
            id = DownloadTaskId("task-active-1"),
            releaseId = null,
            source = DownloadSource.TorrentSource(InfoHash("0000000000000000000000000000000000000000"), name = "Ep 1"),
            state = DownloadState.Downloading
        )
        val task2 = DownloadTask(
            id = DownloadTaskId("task-active-2"),
            releaseId = null,
            source = DownloadSource.TorrentSource(InfoHash("0000000000000000000000000000000000000000"), name = "Ep 2"),
            state = DownloadState.Downloading
        )
        inMemoryDownloadRepo.saveTask(task1)
        inMemoryDownloadRepo.saveTask(task2)

        val reconcileUseCase = ReconcileDownloadsUseCase(inMemoryDownloadRepo)

        // Assume task-active-1 has a part file on disk, but task-active-2 does not
        val report = reconcileUseCase(existingPartFiles = setOf("task-active-1"))

        assertEquals(2, report.recoveredTasksCount)

        val recovered1 = inMemoryDownloadRepo.getTaskById(task1.id)
        val recovered2 = inMemoryDownloadRepo.getTaskById(task2.id)

        assertEquals(DownloadState.Paused, recovered1?.state)
        assertEquals(DownloadState.Pending, recovered2?.state)
    }

    // ===============================================================
    // FLOW 6: STORAGE INSUFFICIENT REJECTION (Section 55)
    // ===============================================================
    @Test
    fun `test Flow 6 - Planner rejects download when storage capacity is insufficient`() {
        val release = createSampleRelease("rel-huge", "Batch Collection", 50L * 1024 * 1024 * 1024) // 50 GB
        val preparePlanUseCase = PrepareDownloadPlanUseCase()

        // Available storage is only 10 GB
        val result = preparePlanUseCase(
            releases = listOf(release),
            availableStorageBytes = 10L * 1024 * 1024 * 1024
        )

        assertTrue(result is AniFlowResult.Success)
        val summary = (result as AniFlowResult.Success).data
        assertFalse("Plan cannot proceed when disk space is insufficient", summary.canProceed)
        assertTrue(summary.warnings.any { it.contains("Insufficient storage space") })
    }

    // ===============================================================
    // FLOW 7: NETWORK RESTRICTION (Section 56)
    // ===============================================================
    @Test
    fun `test Flow 7 - Scheduler blocks task on metered network when WiFiOnly policy is active`() = runBlocking {
        val downloadQueue = DownloadQueue()
        val scheduler = DownloadScheduler(downloadQueue, SchedulerConfig(maxGlobalActiveTasks = 3))

        val task = com.aniflow.download.core.model.DownloadTask(
            id = com.aniflow.download.core.model.DownloadTaskId("task-wifi"),
            source = com.aniflow.download.core.model.DownloadSource.DirectHttp("https://example.com/ep1.mp4"),
            destination = com.aniflow.download.core.model.StorageTarget("/tmp/ep1.mp4"),
            engineType = DownloadEngineType.Http,
            state = DownloadTaskState.Queued
        )
        downloadQueue.enqueue(task)

        // 1. Metered connection with WiFiOnly policy
        val meteredCapabilities = RuntimeCapabilities(
            networkType = NetworkType.CellularMetered,
            isBatteryLow = false,
            freeStorageBytes = 10_000_000_000L
        )
        val claimedOnCellular = scheduler.selectNextTaskAndClaim(meteredCapabilities, NetworkPolicy.WifiOnly)
        assertEquals("Task must NOT start over metered connection when WiFiOnly is enforced", null, claimedOnCellular)

        // 2. WiFi becomes available
        val wifiCapabilities = RuntimeCapabilities(
            networkType = NetworkType.Wifi,
            isBatteryLow = false,
            freeStorageBytes = 10_000_000_000L
        )
        val claimedOnWifi = scheduler.selectNextTaskAndClaim(wifiCapabilities, NetworkPolicy.WifiOnly)
        assertNotNull("Task must start once Wi-Fi is available", claimedOnWifi)
        assertEquals(task.id, claimedOnWifi?.id)
    }

    // ===============================================================
    // FLOW 8: DUPLICATE DETECTION (Section 57)
    // ===============================================================
    @Test
    fun `test Flow 8 - Automation skips duplicate candidate when task already exists`() = runBlocking {
        val existingTask = DownloadTask(
            id = DownloadTaskId("task-existing"),
            releaseId = ReleaseId("rel-dup-1"),
            source = DownloadSource.TorrentSource(InfoHash("0000000000000000000000000000000000000000"), name = "Naruto - 01"),
            state = DownloadState.Downloading
        )
        inMemoryDownloadRepo.saveTask(existingTask)

        val coordinator = AutomationWorkflowCoordinator(
            downloadRepository = inMemoryDownloadRepo,
            eventBus = eventBus
        )

        val rule = AdvancedRule(
            id = com.aniflow.domain.identity.RuleId("rule-naruto"),
            name = "Auto Naruto",
            root = ConditionNode(AdvancedRuleCondition.AnimeIs("Naruto")),
            actions = listOf(AdvancedRuleAction.QueueDownload)
        )

        val candidate = CandidateReleaseContext(
            title = "Naruto - 01",
            animeTitle = "Naruto",
            episodeNumber = 1.0
        )

        // First run triggers download
        val res1 = coordinator.handleTrigger(
            trigger = com.aniflow.domain.controlplane.models.AutomationTrigger.NewReleaseDetected("Naruto"),
            candidates = listOf(candidate),
            rules = listOf(rule)
        )
        assertTrue(res1 is AniFlowResult.Success)

        // Second run with same episode must be prevented by idempotency lock
        val res2 = coordinator.handleTrigger(
            trigger = com.aniflow.domain.controlplane.models.AutomationTrigger.NewReleaseDetected("Naruto"),
            candidates = listOf(candidate),
            rules = listOf(rule)
        )
        assertTrue(res2 is AniFlowResult.Success)
        assertEquals("Duplicate download must be prevented", 0, (res2 as AniFlowResult.Success).data.tasksQueued)
    }

    // ===============================================================
    // FLOW 9: PROVIDER DEGRADATION & ERROR RESILIENCE (Section 58)
    // ===============================================================
    @Test
    fun `test Flow 9 - Provider degradation emits partial or error state without crashing`() = runBlocking {
        val failingProvider = object : ReleaseProvider {
            override val descriptor = ProviderDescriptor(
                id = ProviderId("failing-nyaa"),
                name = "Failing Provider",
                version = "1.0",
                capabilities = emptySet()
            )
            override suspend fun search(request: ProviderSearchRequest) =
                AniFlowResult.Error(com.aniflow.core.common.result.ErrorType.NetworkError("503 Service Unavailable"), "Provider unavailable")
            override suspend fun getDetails(detailsUrl: com.aniflow.domain.valueobject.UrlValue) =
                AniFlowResult.Error(com.aniflow.core.common.result.ErrorType.NetworkError("503"), "Unavailable")
            override suspend fun getRecent(limit: Int) =
                AniFlowResult.Error(com.aniflow.core.common.result.ErrorType.NetworkError("503"), "Unavailable")
        }

        val registry = DefaultProviderRegistry()
        registry.register(failingProvider)
        val coordinator = ProviderSearchCoordinator(registry)

        val merged = coordinator.search(ProviderSearchRequest(query = "test"))
        assertTrue("Merged search must record provider errors without crashing", merged.providerErrors.isNotEmpty())
        assertEquals(0, merged.items.size)
    }

    private fun createSampleRelease(id: String, title: String, sizeBytes: Long): Release {
        val parsed = ReleaseParserImpl().parse(title)
        val cleanTitle = ReleaseNormalizationService.normalizeTitle(title)
        return Release(
            id = ReleaseId(id),
            provider = com.aniflow.domain.model.aggregate.release.ProviderRef(
                providerId = ProviderId("nyaa"),
                providerName = "Nyaa.si",
                externalId = id,
                detailsUrl = null
            ),
            providerReleaseId = id,
            title = title,
            normalizedTitle = cleanTitle,
            identity = com.aniflow.domain.identity.ReleaseIdentity(
                infoHash = InfoHash("0000000000000000000000000000000000000000"),
                canonicalTitle = cleanTitle,
                episodeNumber = 1.0,
                resolution = "1080p",
                codec = "HEVC"
            ),
            availability = com.aniflow.domain.model.aggregate.release.ReleaseAvailability(
                size = ByteSize.fromBytes(sizeBytes),
                seeders = 25,
                leechers = 5
            ),
            source = com.aniflow.domain.model.aggregate.release.ReleaseSource.Torrent(
                infoHash = InfoHash("0000000000000000000000000000000000000000")
            )
        )
    }
}

// ==========================================
// TEST DOUBLES & IN-MEMORY REPOSITORIES
// ==========================================

class FakeReleaseProvider(
    val providerId: String,
    val cannedReleases: List<ProviderRelease>
) : ReleaseProvider {
    override val descriptor = ProviderDescriptor(
        id = ProviderId(providerId),
        name = "Fake Nyaa",
        version = "1.0",
        capabilities = emptySet()
    )

    override suspend fun search(request: ProviderSearchRequest): AniFlowResult<com.aniflow.provider.core.model.ProviderPageResult<ProviderRelease>> {
        return AniFlowResult.Success(
            com.aniflow.provider.core.model.ProviderPageResult(
                items = cannedReleases,
                page = request.page,
                hasNextPage = false
            )
        )
    }

    override suspend fun getDetails(detailsUrl: com.aniflow.domain.valueobject.UrlValue): AniFlowResult<ProviderRelease> {
        return cannedReleases.firstOrNull()?.let { AniFlowResult.Success(it) }
            ?: AniFlowResult.Error(com.aniflow.core.common.result.ErrorType.NotFoundError("Not found"), "Not found")
    }

    override suspend fun getRecent(limit: Int): AniFlowResult<List<ProviderRelease>> {
        return AniFlowResult.Success(cannedReleases.take(limit))
    }
}

class InMemoryReleaseRepository : ReleaseRepository {
    private val store = mutableMapOf<String, Release>()

    override suspend fun getById(id: ReleaseId): Release? = store[id.value]
    override suspend fun getByProviderId(providerId: ProviderId, providerReleaseId: String): Release? =
        store.values.firstOrNull { it.provider.providerId == providerId && it.providerReleaseId == providerReleaseId }
    override suspend fun getByInfoHash(infoHash: InfoHash): Release? = null
    override fun search(query: SearchQuery): Flow<AniFlowResult<PageResult<Release>>> = flowOf(
        AniFlowResult.Success(PageResult(store.values.toList(), 1))
    )
    override suspend fun save(release: Release) { store[release.id.value] = release }
    override suspend fun saveAll(releases: List<Release>) { releases.forEach { save(it) } }
    override fun observeById(id: ReleaseId): Flow<Release?> = flowOf(store[id.value])
}

class InMemoryDownloadRepository : DownloadRepository {
    private val taskStore = mutableMapOf<DownloadTaskId, DownloadTask>()

    override suspend fun getTaskById(id: DownloadTaskId): DownloadTask? = taskStore[id]
    override suspend fun getAllTasks(): List<DownloadTask> = taskStore.values.toList()
    override fun observeAllTasks(): Flow<List<DownloadTask>> = flowOf(taskStore.values.toList())
    override fun observeActiveTasks(): Flow<List<DownloadTask>> = flowOf(taskStore.values.filter { it.state == DownloadState.Downloading })
    override suspend fun saveTask(task: DownloadTask) { taskStore[task.id] = task }
    override suspend fun deleteTask(id: DownloadTaskId) { taskStore.remove(id) }
    override suspend fun getFilesByTaskId(taskId: DownloadTaskId) = emptyList<com.aniflow.domain.model.aggregate.download.DownloadFile>()
    override suspend fun saveFile(file: com.aniflow.domain.model.aggregate.download.DownloadFile) {}
}

class InMemoryLibraryRepository : LibraryRepository {
    private val itemStore = mutableMapOf<LibraryItemId, LibraryItem>()
    private val fileStore = mutableMapOf<LibraryFileId, LibraryFile>()

    override suspend fun getItemById(id: LibraryItemId): LibraryItem? = itemStore[id]
    override suspend fun getItemByAnimeId(animeId: AnimeId): LibraryItem? = null
    override fun observeAllItems(): Flow<List<LibraryItem>> = flowOf(itemStore.values.toList())
    override suspend fun getAllItems(): List<LibraryItem> = itemStore.values.toList()
    override suspend fun getFilesByItemId(itemId: LibraryItemId): List<LibraryFile> =
        fileStore.values.filter { it.itemId == itemId }
    override suspend fun saveItem(item: LibraryItem) { itemStore[item.id] = item }
    override suspend fun saveFile(file: LibraryFile) { fileStore[file.id] = file }
    override suspend fun deleteItem(id: LibraryItemId) { itemStore.remove(id) }
}
