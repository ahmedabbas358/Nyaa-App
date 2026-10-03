package com.aniflow.di

import com.aniflow.download.core.engine.DefaultDownloadEngineRegistry
import com.aniflow.download.core.engine.DownloadEngine
import com.aniflow.download.core.engine.DownloadEngineRegistry
import com.aniflow.download.http.HttpDownloadEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
    fun provideDownloadEngine(httpEngine: HttpDownloadEngine): DownloadEngine = httpEngine

    @Provides
    @Singleton
    fun provideDownloadEngineRegistry(httpEngine: HttpDownloadEngine): DownloadEngineRegistry {
        val registry = DefaultDownloadEngineRegistry()
        registry.register(httpEngine)
        return registry
    }
}
