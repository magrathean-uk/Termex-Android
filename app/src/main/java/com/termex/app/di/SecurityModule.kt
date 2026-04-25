package com.termex.app.di

import com.termex.app.core.security.BiometricAuthGateway
import com.termex.app.core.security.BiometricAuthManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindBiometricAuthGateway(
        biometricAuthManager: BiometricAuthManager
    ): BiometricAuthGateway
}
