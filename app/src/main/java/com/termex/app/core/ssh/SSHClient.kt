package com.termex.app.core.ssh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.apache.sshd.agent.local.LocalAgentFactory
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.channel.ClientChannelEvent
import org.apache.sshd.client.config.keys.ClientIdentityLoader
import org.apache.sshd.client.channel.ChannelShell
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.NamedResource
import org.apache.sshd.common.config.keys.PublicKeyEntry
import org.apache.sshd.common.config.keys.PublicKeyEntryResolver
import org.apache.sshd.common.config.keys.FilePasswordProvider
import org.apache.sshd.common.session.Session
import org.apache.sshd.common.session.SessionListener
import org.apache.sshd.common.util.io.resource.PathResource
import org.apache.sshd.common.util.net.SshdSocketAddress
import org.apache.sshd.common.util.security.SecurityUtils
import org.apache.sshd.core.CoreModuleProperties
import org.apache.sshd.common.util.security.bouncycastle.BouncyCastleSecurityProviderRegistrar
import org.apache.sshd.common.util.security.eddsa.EdDSASecurityProviderRegistrar
import org.apache.sshd.server.forward.AcceptAllForwardingFilter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyPair
import java.security.PublicKey
import java.security.Security
import java.time.Duration
import kotlin.math.min
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
    val identityPath: String? = null,
    val certificatePath: String? = null,
    val privateKeyPassphrase: String? = null,
    val keepAliveIntervalSeconds: Int = 30,
    val connectTimeoutMillis: Int = 15_000,
    val readTimeoutMillis: Int = 15_000,
    val authPreference: AuthPreference = AuthPreference.AUTO,
    val identitiesOnly: Boolean = false,
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
            identityPath == other.identityPath &&
            certificatePath == other.certificatePath &&
            privateKeyPassphrase == other.privateKeyPassphrase &&
            keepAliveIntervalSeconds == other.keepAliveIntervalSeconds &&
            connectTimeoutMillis == other.connectTimeoutMillis &&
            readTimeoutMillis == other.readTimeoutMillis &&
            authPreference == other.authPreference &&
            identitiesOnly == other.identitiesOnly &&
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
        result = 31 * result + (identityPath?.hashCode() ?: 0)
        result = 31 * result + (certificatePath?.hashCode() ?: 0)
        result = 31 * result + (privateKeyPassphrase?.hashCode() ?: 0)
        result = 31 * result + keepAliveIntervalSeconds
        result = 31 * result + connectTimeoutMillis
        result = 31 * result + readTimeoutMillis
        result = 31 * result + authPreference.hashCode()
        result = 31 * result + identitiesOnly.hashCode()
        result = 31 * result + (jumpHost?.hashCode() ?: 0)
        result = 31 * result + forwardAgent.hashCode()
        result = 31 * result + verifyHostKeyCertificates.hashCode()
        return result
    }
}

fun SSHConnectionConfig.hasCredentials(): Boolean {
    return !password.isNullOrBlank() || privateKey != null || !identityPath.isNullOrBlank()
}

class SSHClient @Inject constructor(
    private val hostKeyVerifier: TermexHostKeyVerifier
) {

    init {
        ensureCryptoProvider()
    }

    @Volatile private var sshClient: SshClient? = null
    @Volatile private var session: ClientSession? = null
    @Volatile private var shell: ChannelShell? = null
    @Volatile private var jumpClient: SshClient? = null
    @Volatile private var jumpSession: ClientSession? = null
    @Volatile private var jumpForward: SshdSocketAddress? = null
    @Volatile private var activeConfig: SSHConnectionConfig? = null

    private val _connectionState = MutableStateFlow<SSHConnectionState>(SSHConnectionState.Disconnected)
    val connectionState: StateFlow<SSHConnectionState> = _connectionState.asStateFlow()

    @Volatile var inputStream: InputStream? = null
        private set
    @Volatile var outputStream: OutputStream? = null
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

    suspend fun connect(
        config: SSHConnectionConfig,
        openShell: Boolean = true
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect() // Ensure fresh start
            _connectionState.value = SSHConnectionState.Connecting

            val (client, sess) = connectInternal(config)
            sshClient = client
            session = sess
            activeConfig = config

            // Detect remote disconnects
            sess.addSessionListener(object : SessionListener {
                override fun sessionClosed(session: Session) {
                    if (_connectionState.value is SSHConnectionState.Connected) {
                        _connectionState.value = SSHConnectionState.Error("Connection closed by remote host")
                        cleanupConnection()
                    }
                }
            })

            if (openShell) {
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
            } else {
                shell = null
                inputStream = null
                outputStream = null
            }
            
            _connectionState.value = SSHConnectionState.Connected
            Result.success(Unit)
        } catch (e: Exception) {
            cleanupConnection() // Cleanup without overwriting error state
            val errorMsg = when {
                e.message?.contains("authentication", ignoreCase = true) == true -> 
                    "Authentication failed. Please check your username and password/key."
                e.message?.contains("timeout", ignoreCase = true) == true -> 
                    "Connection timed out. Please check your network and server address."
                e.message?.contains("refused", ignoreCase = true) == true -> 
                    "Connection refused. Please verify the server is running and accepting connections."
                e.message?.contains("host key", ignoreCase = true) == true -> 
                    "Host key verification failed. The server's identity may have changed."
                else -> e.message ?: "Connection failed"
            }
            _connectionState.value = SSHConnectionState.Error(errorMsg)
            Result.failure(e)
        }
    }

    private suspend fun connectInternal(config: SSHConnectionConfig): Pair<SshClient, ClientSession> {
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

    private suspend fun connectDirect(
        config: SSHConnectionConfig,
        connectHost: String,
        connectPort: Int,
        verifyHost: String = connectHost,
        verifyPort: Int = connectPort
    ): Pair<SshClient, ClientSession> {
        hostKeyVerifier.setTarget(verifyHost, verifyPort)
        hostKeyVerifier.primeKnownHost(verifyHost, verifyPort)

        val agentFactory = if (config.forwardAgent) {
            LocalAgentFactory()
        } else {
            null
        }

        // Serialize MINA client init to prevent NIO thread pool contention under parallel connects
        val client = synchronized(minaInitLock) {
            val c = SshClient.setUpDefaultClient()
            c.serverKeyVerifier = hostKeyVerifier
            CoreModuleProperties.IO_CONNECT_TIMEOUT.set(c, Duration.ofMillis(config.connectTimeoutMillis.toLong()))
            // AUTH_TIMEOUT must be long enough for the user to review a host key dialog (up to 2 min)
            CoreModuleProperties.AUTH_TIMEOUT.set(c, Duration.ofSeconds(120))
            CoreModuleProperties.CHANNEL_OPEN_TIMEOUT.set(c, Duration.ofMillis(config.connectTimeoutMillis.toLong()))
            if (config.keepAliveIntervalSeconds > 0) {
                CoreModuleProperties.HEARTBEAT_INTERVAL.set(
                    c,
                    Duration.ofSeconds(config.keepAliveIntervalSeconds.toLong())
                )
            }
            c.forwardingFilter = AcceptAllForwardingFilter.INSTANCE
            if (agentFactory != null) {
                c.agentFactory = agentFactory
            }
            CoreModuleProperties.ABORT_ON_INVALID_CERTIFICATE.set(c, config.verifyHostKeyCertificates)
            c.start()
            c
        }
        var session: ClientSession? = null
        try {
            session = client.connect(config.username, connectHost, connectPort)
                .verify(config.connectTimeoutMillis.toLong())
                .session
            if (agentFactory != null) {
                populateForwardingAgent(agentFactory.agent, session, config)
            }
            authenticate(session, config)
            return client to session
        } catch (e: Exception) {
            // Clean up on failure to prevent resource leaks
            try { session?.close() } catch (_: Exception) {}
            try { client.stop() } catch (_: Exception) {}
            try { client.close() } catch (_: Exception) {}
            throw e
        }
    }

    private fun authenticate(session: ClientSession, config: SSHConnectionConfig) {
        val keyPairs = loadKeyPairs(session, config)
        val hasKey = keyPairs.isNotEmpty()
        val hasPassword = !config.password.isNullOrBlank()

        if (!hasKey && !hasPassword) {
            throw IllegalArgumentException("No authentication method provided")
        }

        keyPairs.forEach { session.addPublicKeyIdentity(it) }
        if (shouldAddPasswordIdentity(config, hasKey, hasPassword)) {
            session.addPasswordIdentity(config.password)
        }

        val preferredOrder = when (config.authPreference) {
            AuthPreference.KEY -> listOf("publickey", "password")
            AuthPreference.PASSWORD -> listOf("password", "publickey")
            AuthPreference.AUTO -> listOf("publickey", "password")
        }
        CoreModuleProperties.PREFERRED_AUTHS.set(session, preferredOrder.joinToString(","))

        val authFuture = session.auth()
        // 120 s: must cover host-key dialog wait (user has time to read and respond)
        authFuture.verify(120_000L)
        if (!authFuture.isSuccess) {
            throw IllegalStateException("Authentication failed")
        }
    }

    private fun populateForwardingAgent(
        agent: org.apache.sshd.agent.SshAgent,
        session: ClientSession,
        config: SSHConnectionConfig
    ) {
        val keyPairs = loadKeyPairs(session, config)
        if (keyPairs.isEmpty()) return

        keyPairs.forEachIndexed { index, keyPair ->
            agent.addIdentity(keyPair, "termex-$index")
        }
    }

    private fun loadKeyPairs(session: ClientSession, config: SSHConnectionConfig): List<KeyPair> {
        val passwordProvider = config.privateKeyPassphrase?.let { FilePasswordProvider.of(it) }
        config.identityPath?.takeIf { it.isNotBlank() }?.let { identityPath ->
            val identityFile = File(identityPath)
            if (!identityFile.exists()) return emptyList()

            val keyPairs = ClientIdentityLoader.DEFAULT
                .loadClientIdentities(session, PathResource(identityFile.toPath()), passwordProvider)
                .toList()
            if (keyPairs.isEmpty()) return emptyList()

            val certificatePublicKey = config.certificatePath
                ?.takeIf { it.isNotBlank() }
                ?.let(::loadCertificatePublicKey)

            return if (certificatePublicKey == null) {
                keyPairs
            } else {
                keyPairs.map { keyPair -> KeyPair(certificatePublicKey, keyPair.private) }
            }
        }

        val keyBytes = config.privateKey ?: return emptyList()
        val resource = NamedResource.ofName("termex-key")
        val input = ByteArrayInputStream(keyBytes)
        return SecurityUtils.loadKeyPairIdentities(session, resource, input, passwordProvider).toList()
    }

    private fun loadCertificatePublicKey(certificatePath: String): PublicKey {
        val certificateFile = File(certificatePath)
        require(certificateFile.exists()) { "Certificate file not found: $certificatePath" }

        val entry = PublicKeyEntry.parsePublicKeyEntry(certificateFile.readText().trim())
        return entry.resolvePublicKey(
            null,
            emptyMap(),
            PublicKeyEntryResolver.FAILING
        )
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
            cleanupConnection()
        }
    }

    suspend fun sendData(data: String) = sendData(data.toByteArray())

    suspend fun runShellCommand(
        command: String,
        expectedFragment: String? = null,
        timeoutMillis: Long = 5_000L
    ): String = withContext(Dispatchers.IO) {
        val sess = session ?: error("Not connected")
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        val normalizedCommand = command.trim()
        if (normalizedCommand.isBlank()) {
            return@withContext ""
        }

        val exec = sess.createExecChannel(normalizedCommand)
        if (activeConfig?.forwardAgent == true) {
            exec.isAgentForwarding = true
        }
        exec.setOut(stdout)
        exec.setErr(stderr)

        try {
            exec.open().verify(timeoutMillis)
            val events = exec.waitFor(setOf(ClientChannelEvent.CLOSED), timeoutMillis)
            if (!events.contains(ClientChannelEvent.CLOSED)) {
                throw IllegalStateException("Command timed out: $normalizedCommand")
            }
            val output = stdout.toString(Charsets.UTF_8.name()) + stderr.toString(Charsets.UTF_8.name())
            if (expectedFragment.isNullOrBlank()) {
                return@withContext output
            }
            return@withContext output
        } finally {
            try {
                exec.close(false)
            } catch (_: Exception) {
            }
        }
    }
    
    private fun cleanupConnection() {
        try { shell?.close() } catch (_: Exception) {}
        try { session?.close() } catch (_: Exception) {}
        try { sshClient?.stop() } catch (_: Exception) {}
        try { sshClient?.close() } catch (_: Exception) {}
        try {
            jumpForward?.let { forward ->
                jumpSession?.stopLocalPortForwarding(forward)
            }
        } catch (_: Exception) {}
        try { jumpSession?.close() } catch (_: Exception) {}
        try { jumpClient?.stop() } catch (_: Exception) {}
        try { jumpClient?.close() } catch (_: Exception) {}

        shell = null
        session = null
        sshClient = null
        jumpClient = null
        jumpSession = null
        jumpForward = null
        activeConfig = null
        inputStream = null
        outputStream = null
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
        try {
            return sess.startRemotePortForwarding(remote, local)
        } catch (e: Exception) {
            val suffix = e.message?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""
            throw IllegalStateException(
                "Remote forward ${remote.hostName}:${remote.port} -> ${local.hostName}:${local.port} failed (${e::class.java.simpleName}$suffix)",
                e
            )
        }
    }

    internal fun stopRemotePortForwarding(remote: SshdSocketAddress) {
        val sess = session ?: throw IllegalStateException("Not connected")
        sess.stopRemotePortForwarding(remote)
    }

    internal fun startDynamicPortForwarding(local: SshdSocketAddress): SshdSocketAddress {
        val sess = session ?: throw IllegalStateException("Not connected")
        try {
            return sess.startDynamicPortForwarding(local)
        } catch (e: Exception) {
            val suffix = e.message?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: ""
            throw IllegalStateException(
                "Dynamic forward ${local.hostName}:${local.port} failed (${e::class.java.simpleName}$suffix)",
                e
            )
        }
    }

    internal fun stopDynamicPortForwarding(local: SshdSocketAddress) {
        val sess = session ?: throw IllegalStateException("Not connected")
        sess.stopDynamicPortForwarding(local)
    }

    companion object {
        internal fun shouldAddPasswordIdentity(
            config: SSHConnectionConfig,
            hasKey: Boolean,
            hasPassword: Boolean
        ): Boolean {
            if (!hasPassword) return false
            if (!hasKey) return true
            if (!config.identitiesOnly) return true
            return config.authPreference == AuthPreference.PASSWORD
        }

        // Serializes MINA SshClient setup+start to prevent NIO thread pool contention
        // when multiple connections are initiated simultaneously.
        private val minaInitLock = Any()

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
