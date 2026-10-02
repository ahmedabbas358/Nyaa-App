package com.aniflow.di

import com.aniflow.core.network.HttpClientFactory
import com.aniflow.core.network.RequestLimiter
import com.aniflow.core.network.RetryPolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        HttpClientFactory.createOkHttpClient(enableLogging = false)

    @Provides
    @Singleton
    fun provideRequestLimiter(): RequestLimiter =
        RequestLimiter(minIntervalMs = 500L)

    @Provides
    @Singleton
    fun provideRetryPolicy(): RetryPolicy =
        RetryPolicy(maxRetries = 2)
}
