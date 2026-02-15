package com.termex.app.core.ssh

import com.termex.app.domain.KnownHost
import com.termex.app.domain.KnownHostRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.apache.sshd.client.keyverifier.ServerKeyVerifier
import org.apache.sshd.client.session.ClientSession
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.security.PublicKey
import java.util.Date
import javax.inject.Inject

sealed class HostKeyVerificationResult {
    data object Trusted : HostKeyVerificationResult()
    data class Unknown(
        val hostname: String,
        val port: Int,
        val keyType: String,
        val fingerprint: String,
        val publicKey: PublicKey
    ) : HostKeyVerificationResult()
    data class Changed(
        val hostname: String,
        val port: Int,
        val keyType: String,
        val newFingerprint: String,
        val oldFingerprint: String,
        val publicKey: PublicKey
    ) : HostKeyVerificationResult()
}

/**
 * Callback interface for host key verification decisions.
 * Used by SSHClient to get user decision asynchronously.
 */
interface HostKeyVerificationCallback {
    suspend fun onVerificationRequired(result: HostKeyVerificationResult): Boolean
}

class TermexHostKeyVerifier @Inject constructor(
    private val knownHostRepository: KnownHostRepository
) : ServerKeyVerifier {

    @Volatile
    private var pendingVerification: HostKeyVerificationResult? = null

    @Volatile
    private var verificationCallback: HostKeyVerificationCallback? = null

    @Volatile
    private var lastVerificationResult: Boolean = false

    @Volatile
    private var targetHost: String? = null

    @Volatile
    private var targetPort: Int? = null

    fun setCallback(callback: HostKeyVerificationCallback?) {
        verificationCallback = callback
    }

    fun setTarget(host: String, port: Int) {
        targetHost = host
        targetPort = port
    }

    fun getPendingVerification(): HostKeyVerificationResult? = pendingVerification

    fun clearPendingVerification() {
        pendingVerification = null
    }

    override fun verifyServerKey(
        clientSession: ClientSession?,
        remoteAddress: SocketAddress?,
        serverKey: PublicKey?
    ): Boolean {
        if (serverKey == null) return false
        val (hostname, port) = resolveTarget(remoteAddress)
        return runBlocking {
            withTimeout(120_000L) {
                verifyAsync(hostname, port, serverKey)
            }
        }
    }

    private fun resolveTarget(remoteAddress: SocketAddress?): Pair<String, Int> {
        val host = targetHost
        val port = targetPort
        if (host != null && port != null) {
            return host to port
        }
        val inet = remoteAddress as? InetSocketAddress
        val resolvedHost = inet?.hostString ?: "unknown"
        val resolvedPort = inet?.port ?: 22
        return resolvedHost to resolvedPort
    }

    private suspend fun verifyAsync(hostname: String, port: Int, key: PublicKey): Boolean {
        val fingerprint = KeyUtils.calculateFingerprint(key)
        val keyType = KeyUtils.getKeyTypeString(key.algorithm)

        val existingHost = knownHostRepository.getKnownHost(hostname, port)

        val result = when {
            existingHost == null -> {
                // New host - ask user
                HostKeyVerificationResult.Unknown(
                    hostname = hostname,
                    port = port,
                    keyType = keyType,
                    fingerprint = fingerprint,
                    publicKey = key
                )
            }
            existingHost.fingerprint == fingerprint -> {
                // Known host with matching key - update last seen and accept
                knownHostRepository.updateKnownHost(
                    existingHost.copy(lastSeenAt = Date())
                )
                HostKeyVerificationResult.Trusted
            }
            else -> {
                // Key has changed - potential MITM attack
                HostKeyVerificationResult.Changed(
                    hostname = hostname,
                    port = port,
                    keyType = keyType,
                    newFingerprint = fingerprint,
                    oldFingerprint = existingHost.fingerprint,
                    publicKey = key
                )
            }
        }

        return when (result) {
            is HostKeyVerificationResult.Trusted -> true
            is HostKeyVerificationResult.Unknown,
            is HostKeyVerificationResult.Changed -> {
                pendingVerification = result
                // Ask callback for decision
                val callback = verificationCallback
                if (callback != null) {
                    lastVerificationResult = callback.onVerificationRequired(result)
                    lastVerificationResult
                } else {
                    // No callback - reject by default for security
                    false
                }
            }
        }
    }

    suspend fun trustHostKey(result: HostKeyVerificationResult) {
        when (result) {
            is HostKeyVerificationResult.Unknown -> {
                acceptHostKey(
                    hostname = result.hostname,
                    port = result.port,
                    keyType = result.keyType,
                    fingerprint = result.fingerprint,
                    publicKey = result.publicKey
                )
            }
            is HostKeyVerificationResult.Changed -> {
                replaceHostKey(
                    hostname = result.hostname,
                    port = result.port,
                    keyType = result.keyType,
                    fingerprint = result.newFingerprint,
                    publicKey = result.publicKey
                )
            }
            is HostKeyVerificationResult.Trusted -> Unit
        }
    }

    /**
     * Accept and save a host key after user confirmation.
     */
    suspend fun acceptHostKey(
        hostname: String,
        port: Int,
        keyType: String,
        fingerprint: String,
        publicKey: PublicKey
    ) {
        val knownHost = KnownHost(
            hostname = hostname,
            port = port,
            keyType = keyType,
            fingerprint = fingerprint,
            publicKey = KeyUtils.encodePublicKey(publicKey)
        )
        knownHostRepository.addKnownHost(knownHost)
    }

    /**
     * Replace an existing host key after user confirmation.
     */
    suspend fun replaceHostKey(
        hostname: String,
        port: Int,
        keyType: String,
        fingerprint: String,
        publicKey: PublicKey
    ) {
        val existingHost = knownHostRepository.getKnownHost(hostname, port)
        if (existingHost != null) {
            knownHostRepository.deleteKnownHost(existingHost)
        }
        acceptHostKey(hostname, port, keyType, fingerprint, publicKey)
    }
}
