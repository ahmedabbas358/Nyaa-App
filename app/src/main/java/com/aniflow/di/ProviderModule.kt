package com.aniflow.di

import com.aniflow.provider.core.ReleaseProvider
import com.aniflow.provider.core.coordinator.ProviderSearchCoordinator
import com.aniflow.provider.core.registry.DefaultProviderRegistry
import com.aniflow.provider.core.registry.ProviderRegistry
import com.aniflow.provider.nyaa.NyaaProvider
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProviderModule {

    @Provides
    @Singleton
    fun provideNyaaProviderConfig(): NyaaProviderConfig = NyaaProviderConfig.DEFAULT

    @Provides
    @Singleton
    fun provideNyaaHttpClient(
        config: NyaaProviderConfig,
        okHttpClient: OkHttpClient
    ): NyaaHttpClient = NyaaHttpClient(config, okHttpClient)

    @Provides
    @Singleton
    fun provideNyaaUrlBuilder(config: NyaaProviderConfig): NyaaUrlBuilder =
        NyaaUrlBuilder(config)

    @Provides
    @Singleton
    fun provideNyaaHtmlSearchParser(config: NyaaProviderConfig): NyaaHtmlSearchParser =
        NyaaHtmlSearchParser(config.baseUrl.rawValue)

    @Provides
    @Singleton
    fun provideNyaaHtmlDetailsParser(config: NyaaProviderConfig): NyaaHtmlDetailsParser =
        NyaaHtmlDetailsParser(config.baseUrl.rawValue)

    @Provides
    @Singleton
    fun provideNyaaRssParser(): NyaaRssParser = NyaaRssParser()

    @Provides
    @Singleton
    fun provideNyaaMapper(): NyaaMapper = NyaaMapper()

    @Provides
    @Singleton
    fun provideNyaaProvider(
        config: NyaaProviderConfig,
        httpClient: NyaaHttpClient,
        urlBuilder: NyaaUrlBuilder,
        searchParser: NyaaHtmlSearchParser,
        detailsParser: NyaaHtmlDetailsParser,
        rssParser: NyaaRssParser,
        mapper: NyaaMapper
    ): NyaaProvider = NyaaProvider(
        config = config,
        httpClient = httpClient,
        urlBuilder = urlBuilder,
        searchParser = searchParser,
        detailsParser = detailsParser,
        rssParser = rssParser,
        mapper = mapper,
        rateLimiter = NyaaRateLimiter(config.requestIntervalMs, config.maxConcurrentRequests),
        retryPolicy = NyaaRetryPolicy(config.maxRetries),
        circuitBreaker = CircuitBreaker(),
        healthChecker = NyaaHealthChecker()
    )

    @Provides
    @Singleton
    fun provideProviderRegistry(nyaaProvider: NyaaProvider): ProviderRegistry {
        val registry = DefaultProviderRegistry()
        registry.register(nyaaProvider)
        return registry
    }

    @Provides
    @Singleton
    fun provideProviderSearchCoordinator(registry: ProviderRegistry): ProviderSearchCoordinator =
        ProviderSearchCoordinator(registry)
}
