package com.termex.app.core.ssh

import com.termex.app.data.crypto.SecurePasswordStore
import com.termex.app.data.prefs.UserPreferencesRepository
import com.termex.app.domain.AuthMode
import com.termex.app.domain.Server
import com.termex.app.domain.ServerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SshConfigBuilder @Inject constructor(
    private val serverRepository: ServerRepository,
    private val passwordStore: SecurePasswordStore,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    suspend fun buildConfig(
        server: Server,
        passwordOverride: String? = null,
        depth: Int = 0
    ): SSHConnectionConfig? {
        if (depth > 1) return null

        val keepAliveInterval = userPreferencesRepository.keepAliveIntervalFlow.first().seconds

        val keyPath = server.keyId?.takeIf { it.isNotBlank() }
        val privateKeyBytes = keyPath?.let { path ->
            withContext(Dispatchers.IO) {
                val keyFile = File(path)
                if (keyFile.exists()) keyFile.readBytes() else null
            }
        }

        val resolvedPassword = passwordOverride?.takeIf { it.isNotBlank() }
            ?: run {
                val resolved = passwordStore.resolvePassword(server.id, server.passwordKeychainID)
                if (resolved.keyId != null && resolved.keyId != server.passwordKeychainID) {
                    serverRepository.updateServer(server.copy(passwordKeychainID = resolved.keyId))
                }
                resolved.password
            }

        val authPreference = when (server.authMode) {
            AuthMode.KEY -> AuthPreference.KEY
            AuthMode.PASSWORD -> AuthPreference.PASSWORD
            AuthMode.AUTO -> AuthPreference.AUTO
        }

        val jumpHostConfig = server.jumpHostId?.let { jumpId ->
            val jumpServer = serverRepository.getServer(jumpId)
            jumpServer?.let { buildConfig(it, depth = depth + 1) }
        }

        return SSHConnectionConfig(
            hostname = server.hostname,
            port = server.port,
            username = server.username,
            password = resolvedPassword,
            privateKey = privateKeyBytes,
            // Use password as passphrase for encrypted keys when key auth is selected
            privateKeyPassphrase = if (privateKeyBytes != null) resolvedPassword else null,
            keepAliveIntervalSeconds = keepAliveInterval,
            authPreference = authPreference,
            jumpHost = jumpHostConfig,
            forwardAgent = server.forwardAgent
        )
    }
}
