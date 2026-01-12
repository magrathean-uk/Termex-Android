package com.termex.app.di

import com.termex.app.data.repository.KeyRepositoryImpl
import com.termex.app.data.repository.ServerRepositoryImpl
import com.termex.app.data.repository.SnippetRepositoryImpl
import com.termex.app.domain.KeyRepository
import com.termex.app.domain.ServerRepository
import com.termex.app.domain.SnippetRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindServerRepository(
        serverRepositoryImpl: ServerRepositoryImpl
    ): ServerRepository

    @Binds
    @Singleton
    abstract fun bindSnippetRepository(
        snippetRepositoryImpl: SnippetRepositoryImpl
    ): SnippetRepository

    @Binds
    @Singleton
    abstract fun bindKeyRepository(
        keyRepositoryImpl: KeyRepositoryImpl
    ): KeyRepository
}
