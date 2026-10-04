package com.aniflow.di

import android.content.Context
import com.aniflow.domain.repository.DownloadRepository
import com.aniflow.domain.repository.LibraryRepository
import com.aniflow.download.core.engine.DefaultDownloadEngineRegistry
import com.aniflow.download.core.engine.DownloadEngine
import com.aniflow.download.core.engine.DownloadEngineRegistry
import com.aniflow.download.http.HttpDownloadEngine
import com.aniflow.download.service.DownloadExecutionCoordinator
import com.aniflow.download.torrent.TorrentDownloadEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
    fun provideTorrentDownloadEngine(): TorrentDownloadEngine =
        TorrentDownloadEngine()

    @Provides
    @Singleton
    fun provideDownloadEngine(httpEngine: HttpDownloadEngine): DownloadEngine = httpEngine

    @Provides
    @Singleton
    fun provideDownloadEngineRegistry(
        httpEngine: HttpDownloadEngine,
        torrentEngine: TorrentDownloadEngine
    ): DownloadEngineRegistry {
        val registry = DefaultDownloadEngineRegistry()
        registry.register(httpEngine)
        registry.register(torrentEngine)
        return registry
    }

    @Provides
    @Singleton
    fun provideDownloadExecutionCoordinator(
        @ApplicationContext context: Context,
        downloadRepository: DownloadRepository,
        engineRegistry: DownloadEngineRegistry,
        libraryRepository: LibraryRepository
    ): DownloadExecutionCoordinator {
        val coordinator = DownloadExecutionCoordinator(
            context = context,
            downloadRepository = downloadRepository,
            engineRegistry = engineRegistry,
            libraryRepository = libraryRepository
        )
        coordinator.start()
        return coordinator
    }
}

