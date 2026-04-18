package com.termex.app.di

import com.termex.app.core.sync.GoogleDriveAuthorizationManager
import com.termex.app.core.sync.GoogleDriveAuthorizationManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindGoogleDriveAuthorizationManager(
        impl: GoogleDriveAuthorizationManagerImpl
    ): GoogleDriveAuthorizationManager
}
