package com.aniflow.di

import com.aniflow.domain.repository.DownloadRepository
import com.aniflow.download.core.DefaultDownloadEngineRegistry
import com.aniflow.download.core.DownloadEngineRegistry
import com.aniflow.download.core.DownloadOrchestrator
import com.aniflow.download.http.HttpDownloadEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {

    @Provides
    @Singleton
    fun provideHttpDownloadEngine(httpClient: OkHttpClient): HttpDownloadEngine =
        HttpDownloadEngine(httpClient)

    @Provides
    @Singleton
    fun provideDownloadEngineRegistry(httpEngine: HttpDownloadEngine): DownloadEngineRegistry {
        val registry = DefaultDownloadEngineRegistry()
        registry.register(httpEngine)
        return registry
    }

    @Provides
    @Singleton
    fun provideDownloadOrchestrator(
        downloadRepository: DownloadRepository,
        httpEngine: HttpDownloadEngine,
        scope: CoroutineScope
    ): DownloadOrchestrator = DownloadOrchestrator(
        downloadRepository = downloadRepository,
        engineSelector = { httpEngine },
        scope = scope
    )
}
