package com.termex.app.core.ssh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.channel.ChannelShell
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.NamedResource
import org.apache.sshd.common.config.keys.FilePasswordProvider
import org.apache.sshd.common.util.net.SshdSocketAddress
import org.apache.sshd.common.util.security.SecurityUtils
import org.apache.sshd.core.CoreModuleProperties
import org.apache.sshd.common.util.security.bouncycastle.BouncyCastleSecurityProviderRegistrar
import org.apache.sshd.common.util.security.eddsa.EdDSASecurityProviderRegistrar
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyPair
import java.security.Security
import java.time.Duration
import javax.inject.Inject

sealed class SSHConnectionState {
    data object Disconnected : SSHConnectionState()
    data object Connecting : SSHConnectionState()
    data object Connected : SSHConnectionState()
    data class Error(val message: String) : SSHConnectionState()
    data class VerifyingHostKey(val result: HostKeyVerificationResult) : SSHConnectionState()
}

enum class AuthPreference {
    KEY,
    PASSWORD,
    AUTO
}

data class SSHConnectionConfig(
    val hostname: String,
    val port: Int = 22,
    val username: String,
    val password: String? = null,
    val privateKey: ByteArray? = null,
    val privateKeyPassphrase: String? = null,
    val keepAliveIntervalSeconds: Int = 30,
    val connectTimeoutMillis: Int = 15_000,
    val readTimeoutMillis: Int = 15_000,
    val authPreference: AuthPreference = AuthPreference.AUTO,
    val jumpHost: SSHConnectionConfig? = null,
    val forwardAgent: Boolean = false,
    val verifyHostKeyCertificates: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SSHConnectionConfig
        val privateKeyMatches = when {
            privateKey == null && other.privateKey == null -> true
            privateKey == null || other.privateKey == null -> false
            else -> privateKey.contentEquals(other.privateKey)
        }
        return hostname == other.hostname &&
            port == other.port &&
            username == other.username &&
            password == other.password &&
            privateKeyMatches &&
            privateKeyPassphrase == other.privateKeyPassphrase &&
            keepAliveIntervalSeconds == other.keepAliveIntervalSeconds &&
            connectTimeoutMillis == other.connectTimeoutMillis &&
            readTimeoutMillis == other.readTimeoutMillis &&
            authPreference == other.authPreference &&
            jumpHost == other.jumpHost &&
            forwardAgent == other.forwardAgent &&
            verifyHostKeyCertificates == other.verifyHostKeyCertificates
    }

    override fun hashCode(): Int {
        var result = hostname.hashCode()
        result = 31 * result + port
        result = 31 * result + username.hashCode()
        result = 31 * result + (password?.hashCode() ?: 0)
        result = 31 * result + (privateKey?.contentHashCode() ?: 0)
        result = 31 * result + (privateKeyPassphrase?.hashCode() ?: 0)
        result = 31 * result + keepAliveIntervalSeconds
        result = 31 * result + connectTimeoutMillis
        result = 31 * result + readTimeoutMillis
        result = 31 * result + authPreference.hashCode()
        result = 31 * result + (jumpHost?.hashCode() ?: 0)
        result = 31 * result + forwardAgent.hashCode()
        result = 31 * result + verifyHostKeyCertificates.hashCode()
        return result
    }
}

class SSHClient @Inject constructor(
    private val hostKeyVerifier: TermexHostKeyVerifier
) {

    init {
        ensureCryptoProvider()
    }

    private var sshClient: SshClient? = null
    private var session: ClientSession? = null
    private var shell: ChannelShell? = null
    private var jumpClient: SshClient? = null
    private var jumpSession: ClientSession? = null
    private var jumpForward: SshdSocketAddress? = null

    private val _connectionState = MutableStateFlow<SSHConnectionState>(SSHConnectionState.Disconnected)
    val connectionState: StateFlow<SSHConnectionState> = _connectionState.asStateFlow()

    var inputStream: InputStream? = null
        private set
    var outputStream: OutputStream? = null
        private set

    private data class PtySize(
        val cols: Int = 80,
        val rows: Int = 24,
        val widthPx: Int = 0,
        val heightPx: Int = 0
    )

    @Volatile
    private var pendingPtySize: PtySize = PtySize()

    fun setHostKeyVerificationCallback(callback: HostKeyVerificationCallback?) {
        hostKeyVerifier.setCallback(callback)
    }

    suspend fun trustHostKey(result: HostKeyVerificationResult) {
        hostKeyVerifier.trustHostKey(result)
    }

    suspend fun connect(config: SSHConnectionConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect() // Ensure fresh start
            _connectionState.value = SSHConnectionState.Connecting

            val (client, sess) = connectInternal(config)
            sshClient = client
            session = sess

            // Start shell with PTY
            val pty = pendingPtySize
            val sh = sess.createShellChannel()
            sh.setUsePty(true)
            sh.ptyType = "xterm-256color"
            sh.ptyColumns = pty.cols
            sh.ptyLines = pty.rows
            sh.ptyWidth = pty.widthPx
            sh.ptyHeight = pty.heightPx
            sh.isAgentForwarding = config.forwardAgent

            sh.open().verify(config.connectTimeoutMillis.toLong())
            shell = sh

            inputStream = sh.invertedOut
            outputStream = sh.invertedIn
            
            _connectionState.value = SSHConnectionState.Connected
            Result.success(Unit)
        } catch (e: Exception) {
            // Error handled via state - no stack trace in production
            cleanupConnection() // Cleanup without overwriting error state
            _connectionState.value = SSHConnectionState.Error(e.message ?: "Connection failed")
            Result.failure(e)
        }
    }

    private fun connectInternal(config: SSHConnectionConfig): Pair<SshClient, ClientSession> {
        return if (config.jumpHost != null) {
            val (jumpClientLocal, jumpSessionLocal) = connectDirect(config.jumpHost, config.jumpHost.hostname, config.jumpHost.port)
            jumpClient = jumpClientLocal
            jumpSession = jumpSessionLocal

            val localBind = SshdSocketAddress("127.0.0.1", 0)
            val remoteTarget = SshdSocketAddress(config.hostname, config.port)
            val boundAddress = jumpSessionLocal.startLocalPortForwarding(localBind, remoteTarget)
            jumpForward = boundAddress

            connectDirect(
                config.copy(hostname = boundAddress.hostName, port = boundAddress.port, jumpHost = null),
                boundAddress.hostName,
                boundAddress.port,
                config.hostname,
                config.port
            )
        } else {
            connectDirect(config, config.hostname, config.port)
        }
    }

    private fun connectDirect(
        config: SSHConnectionConfig,
        connectHost: String,
        connectPort: Int,
        verifyHost: String = connectHost,
        verifyPort: Int = connectPort
    ): Pair<SshClient, ClientSession> {
        hostKeyVerifier.setTarget(verifyHost, verifyPort)

        val client = SshClient.setUpDefaultClient()
        client.serverKeyVerifier = hostKeyVerifier

        CoreModuleProperties.IO_CONNECT_TIMEOUT.set(client, Duration.ofMillis(config.connectTimeoutMillis.toLong()))
        CoreModuleProperties.AUTH_TIMEOUT.set(client, Duration.ofMillis(config.connectTimeoutMillis.toLong()))
        CoreModuleProperties.CHANNEL_OPEN_TIMEOUT.set(client, Duration.ofMillis(config.connectTimeoutMillis.toLong()))
        if (config.readTimeoutMillis > 0) {
            CoreModuleProperties.NIO2_READ_TIMEOUT.set(client, Duration.ofMillis(config.readTimeoutMillis.toLong()))
        }
        if (config.keepAliveIntervalSeconds > 0) {
            CoreModuleProperties.HEARTBEAT_INTERVAL.set(
                client,
                Duration.ofSeconds(config.keepAliveIntervalSeconds.toLong())
            )
        }
        CoreModuleProperties.ABORT_ON_INVALID_CERTIFICATE.set(client, config.verifyHostKeyCertificates)
        if (!config.verifyHostKeyCertificates) {
            val factories = client.signatureFactories
            if (!factories.isNullOrEmpty()) {
                val filtered = factories.filterNot { it.name.contains("-cert-v01@openssh.com") }
                if (filtered.isNotEmpty()) {
                    client.signatureFactories = filtered
                }
            }
        }

        client.start()
        val session = client.connect(config.username, connectHost, connectPort)
            .verify(config.connectTimeoutMillis.toLong())
            .session

        authenticate(session, config)
        return client to session
    }

    private fun authenticate(session: ClientSession, config: SSHConnectionConfig) {
        val keyPairs = loadKeyPairs(session, config)
        val hasKey = keyPairs.isNotEmpty()
        val hasPassword = !config.password.isNullOrBlank()

        if (!hasKey && !hasPassword) {
            throw IllegalArgumentException("No authentication method provided")
        }

        keyPairs.forEach { session.addPublicKeyIdentity(it) }
        if (hasPassword) {
            session.addPasswordIdentity(config.password)
        }

        val preferredOrder = when (config.authPreference) {
            AuthPreference.KEY -> listOf("publickey", "password")
            AuthPreference.PASSWORD -> listOf("password", "publickey")
            AuthPreference.AUTO -> listOf("publickey", "password")
        }
        CoreModuleProperties.PREFERRED_AUTHS.set(session, preferredOrder.joinToString(","))

        val authFuture = session.auth()
        authFuture.verify(config.connectTimeoutMillis.toLong())
        if (!authFuture.isSuccess) {
            throw IllegalStateException("Authentication failed")
        }
    }

    private fun loadKeyPairs(session: ClientSession, config: SSHConnectionConfig): List<KeyPair> {
        val keyBytes = config.privateKey ?: return emptyList()
        val passwordProvider = config.privateKeyPassphrase?.let { FilePasswordProvider.of(it) }
        val resource = NamedResource.ofName("termex-key")
        val input = ByteArrayInputStream(keyBytes)
        return SecurityUtils.loadKeyPairIdentities(session, resource, input, passwordProvider).toList()
    }
    
    fun resizeTerminal(cols: Int, rows: Int, widthPx: Int = 0, heightPx: Int = 0) {
        if (cols <= 0 || rows <= 0) return
        pendingPtySize = PtySize(cols, rows, widthPx, heightPx)
        try {
            shell?.sendWindowChange(cols, rows, widthPx, heightPx)
        } catch (_: Exception) {
        }
    }
    
    suspend fun sendData(data: ByteArray) = withContext(Dispatchers.IO) {
        try {
            outputStream?.write(data)
            outputStream?.flush()
        } catch (e: Exception) {
            _connectionState.value = SSHConnectionState.Error("Failed to send data: ${e.message}")
        }
    }
    
    suspend fun sendData(data: String) = sendData(data.toByteArray())
    
    private fun cleanupConnection() {
        try {
            shell?.close()
            session?.close()
            sshClient?.stop()
            sshClient?.close()
            jumpForward?.let { forward ->
                try {
                    jumpSession?.stopLocalPortForwarding(forward)
                } catch (_: Exception) {
                }
            }
            jumpSession?.close()
            jumpClient?.stop()
            jumpClient?.close()
        } catch (_: Exception) {
        } finally {
            shell = null
            session = null
            sshClient = null
            jumpClient = null
            jumpSession = null
            jumpForward = null
            inputStream = null
            outputStream = null
        }
    }

    fun disconnect() {
        cleanupConnection()
        _connectionState.value = SSHConnectionState.Disconnected
    }
    
    fun isConnected(): Boolean = session?.isOpen == true

    internal fun startLocalPortForwarding(
        local: SshdSocketAddress,
        remote: SshdSocketAddress
    ): SshdSocketAddress {
        val sess = session ?: throw IllegalStateException("Not connected")
        return sess.startLocalPortForwarding(local, remote)
    }

    internal fun stopLocalPortForwarding(local: SshdSocketAddress) {
        val sess = session ?: throw IllegalStateException("Not connected")
        sess.stopLocalPortForwarding(local)
    }

    internal fun startRemotePortForwarding(
        remote: SshdSocketAddress,
        local: SshdSocketAddress
    ): SshdSocketAddress {
        val sess = session ?: throw IllegalStateException("Not connected")
        return sess.startRemotePortForwarding(remote, local)
    }

    internal fun stopRemotePortForwarding(remote: SshdSocketAddress) {
        val sess = session ?: throw IllegalStateException("Not connected")
        sess.stopRemotePortForwarding(remote)
    }

    internal fun startDynamicPortForwarding(local: SshdSocketAddress): SshdSocketAddress {
        val sess = session ?: throw IllegalStateException("Not connected")
        return sess.startDynamicPortForwarding(local)
    }

    internal fun stopDynamicPortForwarding(local: SshdSocketAddress) {
        val sess = session ?: throw IllegalStateException("Not connected")
        sess.stopDynamicPortForwarding(local)
    }

    companion object {
        @Volatile
        private var providerInitialized = false

        private fun ensureCryptoProvider() {
            if (providerInitialized) return
            synchronized(this) {
                if (providerInitialized) return
                // Android ships with a stripped-down functionality Bouncy Castle provider
                // We must remove it and add the full one to support modern algorithms like X25519
                Security.removeProvider("BC")
                Security.addProvider(BouncyCastleProvider())
                try {
                    SecurityUtils.registerSecurityProvider(BouncyCastleSecurityProviderRegistrar())
                } catch (_: Exception) {
                }
                try {
                    SecurityUtils.registerSecurityProvider(EdDSASecurityProviderRegistrar())
                } catch (_: Exception) {
                }
                providerInitialized = true
            }
        }
    }
}
