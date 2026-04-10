package com.termex.app.core.ssh

import com.termex.app.domain.KnownHost
import com.termex.app.domain.KnownHostRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.apache.sshd.client.keyverifier.ServerKeyVerifier
import org.apache.sshd.client.session.ClientSession
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.security.PublicKey
import java.util.concurrent.ConcurrentHashMap
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
 * Callback interface for host key verification.
 * Called fire-and-forget from verifyServerKey() so the NIO2 thread is NEVER blocked.
 * The user decision (accept/reject) is handled asynchronously in the ViewModel.
 */
interface HostKeyVerificationCallback {
    fun onVerificationRequiredAsync(result: HostKeyVerificationResult)
}

class TermexHostKeyVerifier @Inject constructor(
    private val knownHostRepository: KnownHostRepository
) : ServerKeyVerifier {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val knownHostCache = ConcurrentHashMap<String, KnownHost>()

    @Volatile
    private var pendingVerification: HostKeyVerificationResult? = null

    @Volatile
    private var verificationCallback: HostKeyVerificationCallback? = null

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

    suspend fun primeKnownHost(hostname: String, port: Int) {
        val knownHost = knownHostRepository.getKnownHost(hostname, port)
        if (knownHost != null) {
            knownHostCache[cacheKey(hostname, port)] = knownHost
        } else {
            knownHostCache.remove(cacheKey(hostname, port))
        }
    }

    override fun verifyServerKey(
        clientSession: ClientSession?,
        remoteAddress: SocketAddress?,
        serverKey: PublicKey?
    ): Boolean {
        if (serverKey == null) return false
        val (hostname, port) = resolveTarget(remoteAddress)
        val fingerprint = KeyUtils.calculateFingerprint(serverKey)
        val keyType = KeyUtils.getKeyTypeString(serverKey.algorithm)

        val existingHost = knownHostCache[cacheKey(hostname, port)]

        return when {
            existingHost != null && existingHost.fingerprint == fingerprint -> {
                val refreshedHost = existingHost.copy(lastSeenAt = Date())
                knownHostCache[cacheKey(hostname, port)] = refreshedHost
                scope.launch {
                    knownHostRepository.updateKnownHost(refreshedHost)
                }
                true
            }
            existingHost != null -> {
                val result = HostKeyVerificationResult.Changed(
                    hostname = hostname, port = port, keyType = keyType,
                    newFingerprint = fingerprint, oldFingerprint = existingHost.fingerprint,
                    publicKey = serverKey
                )
                pendingVerification = result
                verificationCallback?.onVerificationRequiredAsync(result)
                false
            }
            else -> {
                val result = HostKeyVerificationResult.Unknown(
                    hostname = hostname, port = port, keyType = keyType,
                    fingerprint = fingerprint, publicKey = serverKey
                )
                pendingVerification = result
                verificationCallback?.onVerificationRequiredAsync(result)
                true
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
        knownHostCache[cacheKey(hostname, port)] = knownHost
        knownHostRepository.addKnownHost(knownHost)
        pendingVerification = null
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

    private fun cacheKey(hostname: String, port: Int): String = "$hostname:$port"
}
