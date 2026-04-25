package com.termex.app.core.ssh

import android.content.Context
import com.termex.app.data.crypto.SecurePasswordStore
import com.termex.app.data.prefs.UserPreferencesRepository
import com.termex.app.domain.AuthMode
import com.termex.app.domain.Server
import com.termex.app.domain.ServerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
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
        val identityFiles = resolveIdentityFiles(server, keyPath)

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
            privateKey = if (identityFiles == null) privateKeyBytes else null,
            identityPath = identityFiles?.privateKeyFile?.absolutePath,
            certificatePath = identityFiles?.certificateFile?.absolutePath,
            // Use password as passphrase for encrypted keys when key auth is selected
            privateKeyPassphrase = if (identityFiles != null || privateKeyBytes != null) resolvedPassword else null,
            keepAliveIntervalSeconds = keepAliveInterval,
            authPreference = authPreference,
            identitiesOnly = server.identitiesOnly,
            jumpHost = jumpHostConfig,
            forwardAgent = server.forwardAgent
        )
    }

    private suspend fun resolveIdentityFiles(server: Server, keyPath: String?): IdentityFileBuilder.IdentityFiles? {
        if (keyPath.isNullOrBlank()) return null

        return withContext(Dispatchers.IO) {
            val privateKeyFile = File(keyPath)
            if (!privateKeyFile.exists()) return@withContext null

            val certificatePath = server.certificatePath?.takeIf { it.isNotBlank() }
            if (certificatePath == null) {
                return@withContext null
            }

            val certificateFile = File(certificatePath)
            if (!certificateFile.exists()) {
                return@withContext null
            }

            val identitiesDir = File(context.cacheDir, "ssh_identities")
            IdentityFileBuilder.buildIdentityFiles(
                parentDir = identitiesDir,
                serverId = server.id,
                privateKeyPath = privateKeyFile.absolutePath,
                certificatePath = certificateFile.absolutePath
            )
        }
    }
}
