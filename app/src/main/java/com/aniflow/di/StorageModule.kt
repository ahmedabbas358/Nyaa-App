package com.aniflow.di

import android.content.Context
import com.aniflow.core.storage.StorageManagerImpl
import com.aniflow.download.core.organization.FileOrganizationEngine
import com.aniflow.domain.service.StorageManager
import com.aniflow.platform.storage.AndroidPlatformStorageManager
import com.aniflow.platform.storage.PlatformStorageManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideStorageManager(@ApplicationContext context: Context): StorageManager =
        StorageManagerImpl(context)

    @Provides
    @Singleton
    fun providePlatformStorageManager(@ApplicationContext context: Context): PlatformStorageManager =
        AndroidPlatformStorageManager(context)

    @Provides
    @Singleton
    fun provideFileOrganizationEngine(): FileOrganizationEngine =
        FileOrganizationEngine()
}
