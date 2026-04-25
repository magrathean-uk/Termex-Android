package com.termex.app.testing

import android.content.Context
import com.termex.app.domain.CertificateRepository
import com.termex.app.domain.KeyRepository
import com.termex.app.domain.ServerRepository
import com.termex.app.domain.SnippetRepository
import com.termex.app.domain.WorkplaceRepository
import com.termex.app.data.prefs.UserPreferencesRepository
import com.termex.app.core.ssh.ConnectionManager
import com.termex.app.core.ssh.PortForwardManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AutomationSeedBridgeEntryPoint {
    fun serverRepository(): ServerRepository
    fun keyRepository(): KeyRepository
    fun certificateRepository(): CertificateRepository
    fun snippetRepository(): SnippetRepository
    fun workplaceRepository(): WorkplaceRepository
    fun userPreferencesRepository(): UserPreferencesRepository
    fun portForwardManager(): PortForwardManager
    fun connectionManager(): ConnectionManager
    fun socketProbeHelper(): AutomationSocketProbeHelper
}

object AutomationSeedBridge {
    fun from(context: Context): AutomationSeedBridgeEntryPoint {
        return EntryPointAccessors.fromApplication(
            context.applicationContext,
            AutomationSeedBridgeEntryPoint::class.java
        )
    }
}
