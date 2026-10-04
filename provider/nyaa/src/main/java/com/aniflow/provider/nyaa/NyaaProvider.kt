package com.aniflow.provider.nyaa

import com.aniflow.core.common.result.AniFlowResult
import com.aniflow.core.common.result.ErrorType
import com.aniflow.domain.identity.ProviderId
import com.aniflow.provider.core.ReleaseProvider
import com.aniflow.provider.core.error.ProviderError
import com.aniflow.provider.core.health.ProviderHealth
import com.aniflow.provider.core.model.ProviderCapabilities
import com.aniflow.provider.core.model.ProviderDescriptor
import com.aniflow.provider.core.model.ProviderRelease
import com.aniflow.provider.core.model.ProviderSearchPage
import com.aniflow.provider.core.model.ProviderSearchRequest
import com.aniflow.provider.core.model.SearchTransport
import com.aniflow.provider.nyaa.client.NyaaHttpClient
import com.aniflow.provider.nyaa.client.NyaaUrlBuilder
import com.aniflow.provider.nyaa.config.NyaaProviderConfig
import com.aniflow.provider.nyaa.health.NyaaHealthChecker
import com.aniflow.provider.nyaa.mapper.NyaaMapper
import com.aniflow.provider.nyaa.parser.NyaaHtmlDetailsParser
import com.aniflow.provider.nyaa.parser.NyaaHtmlSearchParser
import com.aniflow.provider.nyaa.parser.NyaaRssParser
import com.aniflow.provider.nyaa.policy.CircuitBreaker
import com.aniflow.provider.nyaa.policy.NyaaRateLimiter
import com.aniflow.provider.nyaa.policy.NyaaRetryPolicy

/**
 * Concrete ReleaseProvider implementation for Nyaa.si (Sections 16, 17, 18).
 * Manages HTTP transport, rate limits, circuit breaker, HTML parsing, RSS fallback, and health.
 */
class NyaaProvider(
    private val config: NyaaProviderConfig = NyaaProviderConfig.DEFAULT,
    private val httpClient: NyaaHttpClient = NyaaHttpClient(config),
    private val urlBuilder: NyaaUrlBuilder = NyaaUrlBuilder(config),
    private val searchParser: NyaaHtmlSearchParser = NyaaHtmlSearchParser(config.baseUrl.rawValue),
    private val detailsParser: NyaaHtmlDetailsParser = NyaaHtmlDetailsParser(config.baseUrl.rawValue),
    private val rssParser: NyaaRssParser = NyaaRssParser(),
    private val mapper: NyaaMapper = NyaaMapper(),
    private val rateLimiter: NyaaRateLimiter = NyaaRateLimiter(config.requestIntervalMs, config.maxConcurrentRequests),
    private val retryPolicy: NyaaRetryPolicy = NyaaRetryPolicy(config.maxRetries),
    private val circuitBreaker: CircuitBreaker = CircuitBreaker(),
    private val healthChecker: NyaaHealthChecker = NyaaHealthChecker()
) : ReleaseProvider {

    override val descriptor = ProviderDescriptor(
        id = ProviderId("nyaa"),
        name = "Nyaa.si",
        baseUrl = config.baseUrl,
        capabilities = ProviderCapabilities.NYAA,
        version = "1.0.0",
        description = "Public anime & Asian media torrent tracker"
    )

    override suspend fun search(request: ProviderSearchRequest): AniFlowResult<ProviderSearchPage> {
        return executeSearchInternal(request, uploader = null)
    }

    override fun toString(): String = "NyaaProvider(baseUrl=${config.baseUrl.rawValue})"

    override suspend fun searchByUploader(
        uploader: String,
        request: ProviderSearchRequest
    ): AniFlowResult<ProviderSearchPage> {
        return executeSearchInternal(request, uploader = uploader)
    }

    override suspend fun getRelease(providerReleaseId: String): AniFlowResult<ProviderRelease> {
        if (!circuitBreaker.canExecute()) {
            return AniFlowResult.Error(
                error = ErrorType.ProviderUnavailable("nyaa", 503),
                message = "Nyaa provider is temporarily halted due to consecutive failures"
            )
        }

        val url = urlBuilder.buildDetailsUrl(providerReleaseId)
        val startTime = System.currentTimeMillis()

        return try {
            val response = rateLimiter.execute {
                retryPolicy.executeWithRetry {
                    httpClient.get(url)
                }
            }
            val latency = System.currentTimeMillis() - startTime
            circuitBreaker.recordSuccess()
            healthChecker.recordSuccess(latency, response.statusCode)

            val detailsDto = detailsParser.parse(response.body, providerReleaseId)
            val release = mapper.toProviderRelease(detailsDto)
            AniFlowResult.Success(release)
        } catch (e: ProviderError) {
            handleError(e, startTime)
        } catch (e: Exception) {
            handleError(ProviderError.Unknown("Unexpected error fetching details: ${e.message}", e), startTime)
        }
    }

    override suspend fun healthCheck(): ProviderHealth {
        return healthChecker.getHealth()
    }

    private suspend fun executeSearchInternal(
        request: ProviderSearchRequest,
        uploader: String?
    ): AniFlowResult<ProviderSearchPage> {
        if (!circuitBreaker.canExecute()) {
            return AniFlowResult.Error(
                error = ErrorType.ProviderUnavailable("nyaa", 503),
                message = "Nyaa provider temporarily unavailable due to multiple network errors"
            )
        }

        val htmlUrl = urlBuilder.buildSearchUrl(request, uploader)
        val startTime = System.currentTimeMillis()

        return try {
            // 1. Primary path: Fetch HTML search page
            val response = rateLimiter.execute {
                retryPolicy.executeWithRetry {
                    httpClient.get(htmlUrl)
                }
            }
            val latency = System.currentTimeMillis() - startTime
            circuitBreaker.recordSuccess()
            healthChecker.recordSuccess(latency, response.statusCode)

            val pageDto = searchParser.parse(response.body, request.page)
            val domainReleases = pageDto.releases.map { mapper.toProviderRelease(it) }

            if (domainReleases.isEmpty() && request.page == 1) {
                // If HTML returns empty on first page, query RSS feed as fallback
                return tryRssFallback(request, uploader, null)
            }

            AniFlowResult.Success(
                ProviderSearchPage(
                    items = domainReleases,
                    page = pageDto.currentPage,
                    hasNextPage = pageDto.hasNextPage,
                    source = SearchTransport.Html,
                    totalItemsEstimate = pageDto.totalResultsEstimate
                )
            )
        } catch (e: Exception) {
            // Automatic RSS fallback on any network, parsing, or blocking error
            tryRssFallback(request, uploader, e)
        }
    }

    private suspend fun tryRssFallback(
        request: ProviderSearchRequest,
        uploader: String?,
        originalError: Throwable?
    ): AniFlowResult<ProviderSearchPage> {
        val rssUrl = urlBuilder.buildRssUrl(request, uploader)
        val startTime = System.currentTimeMillis()
        return try {
            val response = rateLimiter.execute {
                retryPolicy.executeWithRetry {
                    httpClient.get(rssUrl)
                }
            }
            val pageDto = rssParser.parse(response.body)
            val releases = pageDto.releases.map { mapper.toProviderRelease(it) }
            circuitBreaker.recordSuccess()
            healthChecker.recordSuccess(System.currentTimeMillis() - startTime, response.statusCode)

            AniFlowResult.Success(
                ProviderSearchPage(
                    items = releases,
                    page = 1,
                    hasNextPage = false,
                    source = SearchTransport.Rss,
                    totalItemsEstimate = releases.size.toLong()
                )
            )
        } catch (fallbackError: Exception) {
            handleError(
                ProviderError.Unknown("Search failed: ${originalError?.message ?: fallbackError.message}", fallbackError),
                startTime
            )
        }
    }

    private suspend fun <T> handleError(error: ProviderError, startTime: Long): AniFlowResult<T> {
        val latency = System.currentTimeMillis() - startTime
        val statusCode = if (error is ProviderError.InvalidResponse) error.statusCode else null

        circuitBreaker.recordFailure()
        if (error is ProviderError.RateLimited) {
            healthChecker.recordRateLimit(error.retryAfterSeconds)
        } else {
            healthChecker.recordFailure(statusCode, error)
        }

        val domainErrorType = when (error) {
            is ProviderError.Timeout -> ErrorType.TimeoutError
            is ProviderError.NetworkFailure,
            is ProviderError.Blocked -> ErrorType.NetworkError
            is ProviderError.Unavailable -> ErrorType.ProviderUnavailable("nyaa", statusCode)
            is ProviderError.ParserFailure,
            is ProviderError.ParserStructureChanged -> ErrorType.ParserError("nyaa", error.message ?: "Parse error")
            else -> ErrorType.NetworkError
        }

        return AniFlowResult.Error(
            error = domainErrorType,
            message = error.message ?: "Provider operation failed",
            cause = error
        )
    }
}
