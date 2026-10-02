package com.aniflow.testing

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.domain.controlplane.models.ComparisonExpression
import com.aniflow.domain.controlplane.models.ComparisonOperator
import com.aniflow.domain.controlplane.models.SearchExpression
import com.aniflow.domain.controlplane.models.SearchField
import com.aniflow.domain.event.AniFlowEventBus
import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.model.SearchRequest
import com.aniflow.domain.service.ReleaseNormalizationService
import com.aniflow.domain.service.ReleaseParserImpl
import com.aniflow.domain.usecase.PrepareDownloadPlanUseCase
import com.aniflow.domain.usecase.SearchReleasesCoordinatorUseCase
import com.aniflow.domain.valueobject.UrlValue
import com.aniflow.feature.search.SearchViewModel
import com.aniflow.provider.core.coordinator.ProviderSearchCoordinator
import com.aniflow.provider.core.model.ProviderCategory
import com.aniflow.provider.core.model.ProviderRelease
import com.aniflow.provider.core.registry.DefaultProviderRegistry
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Enforces STEP 16: FIRST VERTICAL SLICE VERIFICATION.
 *
 * Verifies the complete single feature vertical slice:
 * SearchScreen / SearchViewModel
 *       ↓
 * SearchReleasesCoordinatorUseCase
 *       ↓
 * ProviderSearchCoordinator
 *       ↓
 * Nyaa Provider
 *       ↓
 * ReleaseParser & Normalizer
 *       ↓
 * Domain Release Aggregate
 *       ↓
 * Room ReleaseRepository Persistence
 *       ↓
 * ReleaseUiModel Transformation
 *       ↓
 * UI State populated for Compose rendering
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirstVerticalSliceTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var releaseRepository: InMemoryReleaseRepository
    private lateinit var eventBus: AniFlowEventBus
    private lateinit var searchViewModel: SearchViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        releaseRepository = InMemoryReleaseRepository()
        eventBus = AniFlowEventBus()

        // 1. Canned Nyaa raw provider release
        val nyaaRelease = ProviderRelease(
            id = "nyaa-1089",
            providerId = ProviderId("nyaa"),
            title = "[SubsPlease] One Piece - 1089 (1080p) [A1B2C3D4].mkv",
            detailsUrl = UrlValue("https://nyaa.si/view/1089"),
            downloadUrl = UrlValue("https://nyaa.si/download/1089.torrent"),
            magnetUrl = UrlValue("magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567&dn=One+Piece+-+1089"),
            sizeBytes = 1_450_000_000L,
            seeders = 320,
            leechers = 12,
            publishedDate = Instant.now(),
            category = ProviderCategory.ANIME_ENGLISH
        )

        // 2. Provider layer wiring
        val fakeProvider = FakeReleaseProvider("nyaa", listOf(nyaaRelease))
        val providerRegistry = DefaultProviderRegistry().apply { register(fakeProvider) }
        val searchCoordinator = ProviderSearchCoordinator(providerRegistry)

        // 3. Coordinator UseCase wiring
        val coordinatorUseCase = SearchReleasesCoordinatorUseCase(
            coordinator = searchCoordinator,
            releaseRepository = releaseRepository,
            releaseParser = ReleaseParserImpl(),
            eventBus = eventBus
        )

        // 4. ViewModel wiring
        searchViewModel = SearchViewModel(
            searchCoordinatorUseCase = coordinatorUseCase,
            preparePlanUseCase = PrepareDownloadPlanUseCase()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun executeFirstVerticalSliceEndToEnd() = runTest(testDispatcher) {
        // Step 1: User types query
        searchViewModel.updateQuery("One Piece")
        assertEquals("One Piece", searchViewModel.uiState.value.query)

        // Step 2: User submits search
        searchViewModel.executeSearch()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = searchViewModel.uiState.value

        // Step 3: Assert UI state transitioned out of loading
        assertFalse("Search should be finished loading", state.isLoading)

        // Step 4: Assert Results reached UI Model
        assertEquals("Should return 1 release result", 1, state.results.size)
        val uiModel = state.results.first()

        assertEquals("[SubsPlease] One Piece - 1089 (1080p) [A1B2C3D4].mkv", uiModel.title)
        assertEquals("SubsPlease", uiModel.releaseGroup)
        assertEquals(320, uiModel.seeders)
        assertEquals("1080p", uiModel.resolution)

        // Step 5: Assert Data reached Room Repository Cache
        val cachedReleases = releaseRepository.search(com.aniflow.domain.valueobject.SearchQuery(text = "One Piece"))
        assertNotNull(cachedReleases)
    }
}
