package com.termex.app.core.ssh

import com.termex.app.domain.KnownHost
import com.termex.app.domain.KnownHostRepository
import kotlinx.coroutines.runBlocking
import net.schmizz.sshj.transport.verification.HostKeyVerifier
import java.security.PublicKey
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

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

@Singleton
class TermexHostKeyVerifier @Inject constructor(
    private val knownHostRepository: KnownHostRepository
) : HostKeyVerifier {

    @Volatile
    private var pendingVerification: HostKeyVerificationResult? = null

    @Volatile
    private var verificationCallback: HostKeyVerificationCallback? = null

    @Volatile
    private var lastVerificationResult: Boolean = false

    fun setCallback(callback: HostKeyVerificationCallback?) {
        verificationCallback = callback
    }

    fun getPendingVerification(): HostKeyVerificationResult? = pendingVerification

    fun clearPendingVerification() {
        pendingVerification = null
    }

    override fun findExistingAlgorithms(hostname: String, port: Int): List<String> {
        // Return empty list - we handle verification ourselves
        return emptyList()
    }

    override fun verify(hostname: String, port: Int, key: PublicKey): Boolean {
        // This runs on the IO thread during SSH connection
        return runBlocking {
            verifyAsync(hostname, port, key)
        }
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
