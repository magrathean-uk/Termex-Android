package com.termex.app.di

import com.termex.app.discovery.AndroidSharedServersDiscoveryService
import com.termex.app.discovery.SharedServersDiscoveryService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DiscoveryModule {

    @Binds
    @Singleton
    abstract fun bindSharedServersDiscoveryService(
        impl: AndroidSharedServersDiscoveryService
    ): SharedServersDiscoveryService
}

