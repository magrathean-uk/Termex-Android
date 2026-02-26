package com.termex.app.di

import com.termex.app.data.repository.CertificateRepositoryImpl
import com.termex.app.data.repository.KeyRepositoryImpl
import com.termex.app.data.repository.KnownHostRepositoryImpl
import com.termex.app.data.repository.ServerRepositoryImpl
import com.termex.app.data.repository.SnippetRepositoryImpl
import com.termex.app.data.repository.WorkplaceRepositoryImpl
import com.termex.app.domain.CertificateRepository
import com.termex.app.domain.KeyRepository
import com.termex.app.domain.KnownHostRepository
import com.termex.app.domain.ServerRepository
import com.termex.app.domain.SnippetRepository
import com.termex.app.domain.WorkplaceRepository
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

    @Binds
    @Singleton
    abstract fun bindKnownHostRepository(
        knownHostRepositoryImpl: KnownHostRepositoryImpl
    ): KnownHostRepository

    @Binds
    @Singleton
    abstract fun bindWorkplaceRepository(
        workplaceRepositoryImpl: WorkplaceRepositoryImpl
    ): WorkplaceRepository

    @Binds
    @Singleton
    abstract fun bindCertificateRepository(
        certificateRepositoryImpl: CertificateRepositoryImpl
    ): CertificateRepository
}
