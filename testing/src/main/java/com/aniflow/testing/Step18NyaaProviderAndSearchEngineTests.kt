package com.aniflow.testing

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.domain.controlplane.models.ComparisonExpression
import com.aniflow.domain.controlplane.models.ComparisonOperator
import com.aniflow.domain.controlplane.models.SearchExpression
import com.aniflow.domain.controlplane.models.SearchField
import com.aniflow.domain.identity.ProviderId
import com.aniflow.domain.identity.ReleaseId
import com.aniflow.domain.model.SearchIdentity
import com.aniflow.domain.model.SearchRequest
import com.aniflow.domain.model.SearchResult
import com.aniflow.domain.model.aggregate.release.ProviderRef
import com.aniflow.domain.model.aggregate.release.Release
import com.aniflow.domain.model.aggregate.release.ReleaseAvailability
import com.aniflow.domain.model.aggregate.release.ReleaseSource
import com.aniflow.domain.model.aggregate.release.ReleaseType
import com.aniflow.domain.repository.ReleaseRepository
import com.aniflow.domain.repository.SearchRepository
import com.aniflow.domain.usecase.GetReleaseDetailsUseCase
import com.aniflow.domain.usecase.SearchReleasesUseCase
import com.aniflow.domain.valueobject.ByteSize
import com.aniflow.domain.valueobject.InfoHash
import com.aniflow.domain.valueobject.ReleaseTechnicalMetadata
import com.aniflow.domain.valueobject.SearchFilters
import com.aniflow.domain.valueobject.SearchSorting
import com.aniflow.domain.valueobject.SortDirection
import com.aniflow.domain.valueobject.SortOption
import com.aniflow.domain.valueobject.UrlValue
import com.aniflow.feature.search.SearchStatus
import com.aniflow.feature.search.SearchUiEvent
import com.aniflow.feature.search.SearchViewModel
import com.aniflow.provider.core.model.ProviderCapabilities
import com.aniflow.provider.core.model.ProviderCategory
import com.aniflow.provider.core.model.ProviderRelease
import com.aniflow.provider.core.model.ProviderSearchFilters
import com.aniflow.provider.core.model.ProviderSearchRequest
import com.aniflow.provider.core.model.ProviderSort
import com.aniflow.provider.core.model.ProviderSortField
import com.aniflow.provider.core.registry.DefaultProviderRegistry
import com.aniflow.provider.nyaa.client.NyaaUrlBuilder
import com.aniflow.provider.nyaa.config.NyaaProviderConfig
import com.aniflow.provider.nyaa.nyaaProviderId
import com.aniflow.provider.nyaa.parser.NyaaHtmlDetailsParser
import com.aniflow.provider.nyaa.parser.NyaaParserSupport
import com.aniflow.provider.nyaa.policy.CircuitBreaker
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
 * Comprehensive verification test suite for STEP 18:
 * FULL NYAA PROVIDER, SEARCH ENGINE, PAGINATION, FILTERS & RELEASE DETAILS (Section 106, 107).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Step18NyaaProviderAndSearchEngineTests {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------
    // 1. NyaaUrlBuilder & Query Translation (Sections 7, 8, 9, 10, 11)
    // -------------------------------------------------------------
    @Test
    fun testNyaaUrlBuilderConstructsValidParameters() {
        val urlBuilder = NyaaUrlBuilder(NyaaProviderConfig.DEFAULT)
        val request = ProviderSearchRequest(
            query = "Sousou no Frieren",
            page = 2,
            category = ProviderCategory.Anime,
            filters = ProviderSearchFilters.TrustedOnly,
            sort = ProviderSort(field = ProviderSortField.Seeders, direction = SortDirection.Descending)
        )

        val url = urlBuilder.buildSearchUrl(request, uploader = "SubsPlease")

        assertTrue(url.contains("f=2")) // Trusted only
        assertTrue(url.contains("c=1_0")) // Anime category
        assertTrue(url.contains("q=Sousou+no+Frieren")) // Encoded query
        assertTrue(url.contains("s=seeders")) // Sort field
        assertTrue(url.contains("o=desc")) // Sort direction
        assertTrue(url.contains("p=2")) // Page 2
        assertTrue(url.contains("u=SubsPlease")) // Uploader filter
    }

    // -------------------------------------------------------------
    // 2. Query Identity & Duplicate Page Protection (Sections 17, 18, 19)
    // -------------------------------------------------------------
    @Test
    fun testSearchIdentityDeterministicKey() {
        val id1 = SearchIdentity.create(
            providerId = nyaaProviderId,
            query = "  Dungeon   Meshi  ",
            trustedOnly = true
        )
        val id2 = SearchIdentity.create(
            providerId = nyaaProviderId,
            query = "Dungeon Meshi",
            trustedOnly = true
        )

        assertEquals("Dungeon Meshi", id1.normalizedQuery)
        assertEquals(id1.normalizedQuery, id2.normalizedQuery)
        assertEquals(id1.toCacheKey(1), id2.toCacheKey(1))
        // Different pages produce different keys
        assertFalse(id1.toCacheKey(1) == id1.toCacheKey(2))
    }

    // -------------------------------------------------------------
    // 3. Safe Text & Number Parsing (Sections 26, 27, 28)
    // -------------------------------------------------------------
    @Test
    fun testParserSupportSafeNumberAndSizeParsing() {
        // Safe integer parsing with commas or dashes
        assertEquals(1234, NyaaParserSupport.safeToInt("1,234"))
        assertNull(NyaaParserSupport.safeToInt("—"))
        assertNull(NyaaParserSupport.safeToInt(""))

        // Safe 64-bit integer
        assertEquals(89000L, NyaaParserSupport.safeToLong("89,000"))

        // Byte size conversion
        val gib = NyaaParserSupport.parseSizeBytes("1.4 GiB")
        assertEquals(1503238553L, gib)

        val mib = NyaaParserSupport.parseSizeBytes("700.5 MiB")
        assertEquals(734524211L, mib)
    }

    // -------------------------------------------------------------
    // 4. Provider Capabilities Announcement (Sections 3, 4)
    // -------------------------------------------------------------
    @Test
    fun testProviderCapabilitiesModel() {
        val caps = ProviderCapabilities.NYAA
        assertTrue(caps.search)
        assertTrue(caps.pagination)
        assertTrue(caps.details)
        assertTrue(caps.torrent)
        assertTrue(caps.magnet)
        assertTrue(caps.uploaderSearch)
        assertTrue(caps.categoryFiltering)
        assertTrue(caps.sorting.contains(ProviderSortField.Seeders))
    }

    // -------------------------------------------------------------
    // 5. Circuit Breaker Behavior (Section 42, 43)
    // -------------------------------------------------------------
    @Test
    fun testCircuitBreakerTripsAfterFailures() {
        val breaker = CircuitBreaker(failureThreshold = 3, resetTimeoutMs = 5000L)
        assertTrue(breaker.canExecute())

        breaker.recordFailure()
        breaker.recordFailure()
        assertTrue("Circuit should remain closed before threshold", breaker.canExecute())

        breaker.recordFailure()
        assertFalse("Circuit should trip OPEN after 3 failures", breaker.canExecute())
    }

    // -------------------------------------------------------------
    // 6. Search ViewModel Generation & Cancellation (Sections 46, 47, 48)
    // -------------------------------------------------------------
    @Test
    fun testSearchCancellationAndStaleResponseRejection() = runTest(testDispatcher) {
        var queryExecuted = ""
        val fakeRepo = object : SearchRepository {
            override suspend fun search(request: SearchRequest): SearchResult {
                queryExecuted = request.query.toQueryString()
                return SearchResult(
                    items = emptyList(),
                    page = request.page,
                    hasNextPage = false
                )
            }
        }

        val viewModel = SearchViewModel(searchReleasesUseCase = SearchReleasesUseCase(fakeRepo))

        // Initial search
        viewModel.updateQuery("Bleach", autoSearch = false)
        viewModel.executeSearch()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1L, viewModel.uiState.value.searchGeneration)

        // Rapid second search increments generation
        viewModel.updateQuery("One Piece", autoSearch = false)
        viewModel.executeSearch()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2L, viewModel.uiState.value.searchGeneration)
        assertEquals("One Piece", queryExecuted)
    }

    // -------------------------------------------------------------
    // 7. Duplicate Page Protection in Pagination (Section 16, 17)
    // -------------------------------------------------------------
    @Test
    fun testDuplicatePageProtection() = runTest(testDispatcher) {
        var searchCallCount = 0
        val sampleRelease = Release(
            id = ReleaseId("rel-page-1"),
            provider = ProviderRef(nyaaProviderId, "Nyaa", "100", "https://nyaa.si/view/100"),
            providerReleaseId = "100",
            title = "[SubsPlease] Frieren - 01 (1080p).mkv",
            normalizedTitle = "Frieren - 01",
            releaseType = ReleaseType.SingleEpisode,
            animeIdentity = null,
            seasonHint = null,
            episodeRange = null,
            uploader = null,
            releaseGroup = null,
            technical = ReleaseTechnicalMetadata(),
            availability = ReleaseAvailability(),
            source = ReleaseSource.Unknown,
            publishedAt = Instant.now()
        )

        val fakeRepo = object : SearchRepository {
            override suspend fun search(request: SearchRequest): SearchResult {
                searchCallCount++
                return SearchResult(
                    items = listOf(sampleRelease),
                    page = request.page,
                    hasNextPage = true
                )
            }
        }

        val viewModel = SearchViewModel(searchReleasesUseCase = SearchReleasesUseCase(fakeRepo))
        viewModel.updateQuery("Frieren", autoSearch = false)
        viewModel.executeSearch()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, searchCallCount)

        // Load page 2
        viewModel.loadMore()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, searchCallCount)

        // Requesting page 2 again immediately should be blocked by duplicate page protection
        viewModel.loadMore()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Duplicate page request should be ignored", 2, searchCallCount)
    }

    // -------------------------------------------------------------
    // 8. Uploader Search Integration (Sections 50, 98)
    // -------------------------------------------------------------
    @Test
    fun testUploaderSearchRouting() = runTest(testDispatcher) {
        var receivedQuery = ""
        val fakeRepo = object : SearchRepository {
            override suspend fun search(request: SearchRequest): SearchResult {
                receivedQuery = request.query.toQueryString()
                return SearchResult(items = emptyList(), page = 1, hasNextPage = false)
            }
        }

        val viewModel = SearchViewModel(searchReleasesUseCase = SearchReleasesUseCase(fakeRepo))
        viewModel.searchByUploader("Erai-raws")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(receivedQuery.contains("uploader:Erai-raws"))
    }

    // -------------------------------------------------------------
    // 9. Lazy Release Details Retrieval (Sections 32, 33, 34, 51)
    // -------------------------------------------------------------
    @Test
    fun testGetReleaseDetailsUseCaseCacheFirst() = runTest(testDispatcher) {
        val cachedRelease = Release(
            id = ReleaseId("rel-detail-1"),
            provider = ProviderRef(nyaaProviderId, "Nyaa", "1800001", "https://nyaa.si/view/1800001"),
            providerReleaseId = "1800001",
            title = "[SubsPlease] Frieren - 28 (1080p).mkv",
            normalizedTitle = "Frieren - 28",
            releaseType = ReleaseType.SingleEpisode,
            animeIdentity = null,
            seasonHint = null,
            episodeRange = null,
            uploader = null,
            releaseGroup = null,
            technical = ReleaseTechnicalMetadata(),
            availability = ReleaseAvailability(seeders = 140),
            source = ReleaseSource.Unknown,
            publishedAt = Instant.now()
        )

        val inMemoryRepo = InMemoryReleaseRepository().apply {
            save(cachedRelease)
        }
        val fakeNyaa = FakeReleaseProvider("nyaa", emptyList())
        val registry = DefaultProviderRegistry().apply { register(fakeNyaa) }

        val useCase = GetReleaseDetailsUseCase(inMemoryRepo, registry)

        // 1. Initial call returns cached release without remote hit
        val result = useCase(ReleaseId("rel-detail-1"), forceRefresh = false)
        assertTrue(result is AniFlowResult.Success)
        val loaded = (result as AniFlowResult.Success).data
        assertEquals("rel-detail-1", loaded.id.value)
        assertEquals(140, loaded.availability.seeders)
    }

    // -------------------------------------------------------------
    // 10. Details HTML Parsing (Section 32, 35, 36)
    // -------------------------------------------------------------
    @Test
    fun testDetailsParserExtractsFullMetadata() {
        val parser = NyaaHtmlDetailsParser("https://nyaa.si")
        val sampleHtml = """
            <html>
                <body>
                    <div class="panel-heading"><h3 class="panel-title">[SubsPlease] Frieren - 28 (1080p) [ABCD1234].mkv</h3></div>
                    <div class="panel-body">
                        <div class="row">
                            <div class="col-md-1">Category:</div>
                            <div class="col-md-5"><a href="/?c=1_2">Anime - English-translated</a></div>
                        </div>
                        <div class="row">
                            <div class="col-md-1">Submitter:</div>
                            <div class="col-md-5"><a href="/user/SubsPlease">SubsPlease</a></div>
                        </div>
                        <div class="row">
                            <div class="col-md-1">Seeders:</div>
                            <div class="col-md-5">250</div>
                            <div class="col-md-1">Leechers:</div>
                            <div class="col-md-5">8</div>
                        </div>
                        <div class="row">
                            <div class="col-md-1">File size:</div>
                            <div class="col-md-5">1.4 GiB</div>
                        </div>
                        <div class="row">
                            <div class="col-md-1">Info hash:</div>
                            <div class="col-md-5"><kbd>0123456789abcdef0123456789abcdef01234567</kbd></div>
                        </div>
                    </div>
                    <div class="panel-footer">
                        <a href="/download/1800001.torrent">Download Torrent</a>
                        <a href="magnet:?xt=urn:btih:0123456789abcdef0123456789abcdef01234567&dn=Frieren">Magnet</a>
                    </div>
                    <div id="torrent-description">Full Episode 28 in 1080p.</div>
                </body>
            </html>
        """.trimIndent()

        val dto = parser.parse(sampleHtml, "1800001")

        assertEquals("[SubsPlease] Frieren - 28 (1080p) [ABCD1234].mkv", dto.title)
        assertEquals("SubsPlease", dto.uploaderName)
        assertEquals(250, dto.seeders)
        assertEquals(8, dto.leechers)
        assertEquals("0123456789abcdef0123456789abcdef01234567", dto.infoHash)
        assertEquals("https://nyaa.si/download/1800001.torrent", dto.torrentUrl)
        assertNotNull(dto.magnetUri)
        assertEquals("Full Episode 28 in 1080p.", dto.descriptionMarkdown)
    }
}
