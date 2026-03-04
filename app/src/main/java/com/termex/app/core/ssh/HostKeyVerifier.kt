package com.termex.app.core.ssh

import com.termex.app.domain.KnownHost
import com.termex.app.domain.KnownHostRepository
import kotlinx.coroutines.runBlocking
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

    override fun verifyServerKey(
        clientSession: ClientSession?,
        remoteAddress: SocketAddress?,
        serverKey: PublicKey?
    ): Boolean {
        if (serverKey == null) return false
        val (hostname, port) = resolveTarget(remoteAddress)
        val fingerprint = KeyUtils.calculateFingerprint(serverKey)
        val keyType = KeyUtils.getKeyTypeString(serverKey.algorithm)

        // runBlocking is acceptable here for a brief DB read on this background thread
        val existingHost = runBlocking { knownHostRepository.getKnownHost(hostname, port) }

        return when {
            existingHost != null && existingHost.fingerprint == fingerprint -> {
                // Known and trusted — update last-seen timestamp, proceed immediately
                runBlocking { knownHostRepository.updateKnownHost(existingHost.copy(lastSeenAt = Date())) }
                true
            }
            existingHost != null -> {
                // Key has changed — warn user, tentatively accept so connection can complete,
                // ViewModel will disconnect if user rejects
                val result = HostKeyVerificationResult.Changed(
                    hostname = hostname, port = port, keyType = keyType,
                    newFingerprint = fingerprint, oldFingerprint = existingHost.fingerprint,
                    publicKey = serverKey
                )
                pendingVerification = result
                verificationCallback?.onVerificationRequiredAsync(result)
                true
            }
            else -> {
                // Unknown host — tentatively accept so connection can complete,
                // ViewModel will disconnect if user rejects
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
