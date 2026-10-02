package com.aniflow.testing

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.core.common.time.FakeClock
import com.aniflow.core.common.time.SystemClock
import com.aniflow.domain.controlplane.models.ComparisonExpression
import com.aniflow.domain.controlplane.models.ComparisonOperator
import com.aniflow.domain.controlplane.models.SearchExpression
import com.aniflow.domain.controlplane.models.SearchField
import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.model.SearchRequest
import com.aniflow.domain.model.SearchResult
import com.aniflow.domain.model.aggregate.release.ProviderRef
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.model.aggregate.release.ReleaseAvailability
import com.aniflow.domain.model.aggregate.release.ReleaseSource
import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.repository.SearchRepository
import com.aniflow.domain.usecase.SearchReleasesUseCase
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.ReleaseTechnicalMetadata
import com.aniflow.feature.search.SearchStatus
import com.aniflow.feature.search.SearchUiEvent
import com.aniflow.feature.search.SearchViewModel
import com.aniflow.provider.core.coordinator.ProviderSearchCoordinator
import com.aniflow.provider.core.model.ProviderCategory
import com.aniflow.provider.core.model.ProviderRelease
import com.aniflow.provider.core.registry.DefaultProviderRegistry
import com.aniflow.provider.nyaa.mapper.NyaaMapper
import com.aniflow.provider.nyaa.model.NyaaParsedRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Verification test suite for STEP 17 (Sections 64, 66, 72).
 * Verifies:
 * - SearchUseCaseTest
 * - NyaaMapperTest
 * - ProviderRegistryTest
 * - SearchRepositoryTest
 * - SearchViewModelTest
 * - Clock Abstraction Test
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Step17FoundationAndSearchTests {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---------------------------------------------------------
    // 1. Clock Abstraction Test (Section 17)
    // ---------------------------------------------------------
    @Test
    fun testClockAbstraction() {
        val fakeClock = FakeClock(Instant.ofEpochSecond(1000))
        assertEquals(1000L, fakeClock.now().epochSecond)

        fakeClock.advanceBySeconds(60)
        assertEquals(1060L, fakeClock.now().epochSecond)

        val systemClock = SystemClock()
        assertTrue(systemClock.now().toEpochMilli() > 0)
    }

    // ---------------------------------------------------------
    // 2. ProviderRegistry Test (Section 40, 42)
    // ---------------------------------------------------------
    @Test
    fun testProviderRegistryOperations() {
        val registry = DefaultProviderRegistry()
        val fakeNyaa = FakeReleaseProvider("nyaa", emptyList())

        registry.register(fakeNyaa)
        assertNotNull(registry.get(ProviderId("nyaa")))
        assertEquals(1, registry.all().size)
        assertTrue(registry.isEnabled(ProviderId("nyaa")))

        registry.setEnabled(ProviderId("nyaa"), false)
        assertFalse(registry.isEnabled(ProviderId("nyaa")))
        assertEquals(0, registry.enabled().size)
        assertEquals(1, registry.all().size)
    }

    // ---------------------------------------------------------
    // 3. NyaaMapper Test (Section 54, 55, 64)
    // ---------------------------------------------------------
    @Test
    fun testNyaaMapperTransformsCorrectly() {
        val mapper = NyaaMapper()
        val parsed = NyaaParsedRelease(
            id = "1900001",
            title = "[SubsPlease] Solo Leveling - 12 (1080p) [9876FEDC].mkv",
            torrentUrl = "https://nyaa.si/download/1900001.torrent",
            magnetUri = "magnet:?xt=urn:btih:fedcba9876543210fedcba9876543210fedcba98&dn=Solo+Leveling",
            sizeDisplay = "1.3 GiB",
            sizeBytes = 1_395_864_371L,
            dateTimestamp = 1711756800L,
            seeders = 550,
            leechers = 18,
            completedDownloads = 8900L,
            isTrusted = true,
            isRemake = false,
            categoryCode = "1_2"
        )

        val providerRelease = mapper.toProviderRelease(parsed, baseUrl = "https://nyaa.si")

        assertEquals("1900001", providerRelease.providerReleaseId)
        assertEquals("[SubsPlease] Solo Leveling - 12 (1080p) [9876FEDC].mkv", providerRelease.title)
        assertEquals(1_395_864_371L, providerRelease.sizeBytes)
        assertEquals(550, providerRelease.seeders)
        assertEquals(18, providerRelease.leechers)
        assertEquals(8900, providerRelease.downloads)
        assertTrue(providerRelease.isTrusted)
        assertEquals("https://nyaa.si/view/1900001", providerRelease.detailsUrl)
    }

    // ---------------------------------------------------------
    // 4. SearchUseCase & Repository Test (Section 50, 51, 64)
    // ---------------------------------------------------------
    @Test
    fun testSearchReleasesUseCase() = runTest(testDispatcher) {
        val cannedRelease = Release(
            id = ReleaseId("rel-test-1"),
            provider = ProviderRef(ProviderId("nyaa"), "Nyaa", "1900001", "https://nyaa.si/view/1900001"),
            providerReleaseId = "1900001",
            title = "[SubsPlease] Solo Leveling - 12 (1080p).mkv",
            normalizedTitle = "Solo Leveling - 12",
            releaseType = ReleaseType.SingleEpisode,
            animeIdentity = null,
            seasonHint = null,
            episodeRange = null,
            uploader = null,
            releaseGroup = null,
            technical = ReleaseTechnicalMetadata(),
            availability = ReleaseAvailability(
                size = ByteSize.fromBytes(1_400_000_000L),
                seeders = 500,
                leechers = 10,
                completedDownloads = 5000L
            ),
            source = ReleaseSource.Unknown,
            publishedAt = Instant.now()
        )

        val fakeSearchRepo = object : SearchRepository {
            override suspend fun search(request: SearchRequest): SearchResult {
                return SearchResult(
                    items = listOf(cannedRelease),
                    page = request.page,
                    hasNextPage = false,
                    isFromCache = false
                )
            }
        }

        val useCase = SearchReleasesUseCase(fakeSearchRepo)
        val request = SearchRequest(
            query = SearchExpression(
                root = ComparisonExpression(SearchField.Anime, ComparisonOperator.Contains, "Solo Leveling")
            )
        )

        val result = useCase(request)
        assertEquals(1, result.items.size)
        assertEquals("[SubsPlease] Solo Leveling - 12 (1080p).mkv", result.items.first().title)
        assertFalse(result.isFromCache)
    }

    // ---------------------------------------------------------
    // 5. SearchViewModel Test (Section 48, 49)
    // ---------------------------------------------------------
    @Test
    fun testSearchViewModelStateTransitions() = runTest(testDispatcher) {
        val sampleRelease = Release(
            id = ReleaseId("rel-vm-1"),
            provider = ProviderRef(ProviderId("nyaa"), "Nyaa", "2000001", "https://nyaa.si/view/2000001"),
            providerReleaseId = "2000001",
            title = "[Erai-raws] Frieren - 28 [1080p].mkv",
            normalizedTitle = "Frieren - 28",
            releaseType = ReleaseType.SingleEpisode,
            animeIdentity = null,
            seasonHint = null,
            episodeRange = null,
            uploader = null,
            releaseGroup = null,
            technical = ReleaseTechnicalMetadata(),
            availability = ReleaseAvailability(
                size = ByteSize.fromBytes(1_200_000_000L),
                seeders = 200,
                leechers = 5,
                completedDownloads = 2000L
            ),
            source = ReleaseSource.Unknown,
            publishedAt = Instant.now()
        )

        val fakeRepo = object : SearchRepository {
            override suspend fun search(request: SearchRequest): SearchResult {
                return SearchResult(
                    items = listOf(sampleRelease),
                    page = 1,
                    hasNextPage = false,
                    isFromCache = false
                )
            }
        }

        val useCase = SearchReleasesUseCase(fakeRepo)
        val viewModel = SearchViewModel(searchReleasesUseCase = useCase)

        // Event: Update Query
        viewModel.onEvent(SearchUiEvent.QueryChanged("Frieren"))
        assertEquals("Frieren", viewModel.uiState.value.query)

        // Event: Submit Search
        viewModel.onEvent(SearchUiEvent.SubmitSearch)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(SearchStatus.Success, state.status)
        assertEquals(1, state.results.size)
        assertEquals("[Erai-raws] Frieren - 28 [1080p].mkv", state.results.first().title)

        // Event: Toggle selection
        viewModel.onEvent(SearchUiEvent.ToggleSelection("rel-vm-1"))
        assertTrue(viewModel.uiState.value.selectedReleaseIds.contains("rel-vm-1"))

        // Event: Clear Selection
        viewModel.onEvent(SearchUiEvent.ClearSelection)
        assertTrue(viewModel.uiState.value.selectedReleaseIds.isEmpty())
    }
}
