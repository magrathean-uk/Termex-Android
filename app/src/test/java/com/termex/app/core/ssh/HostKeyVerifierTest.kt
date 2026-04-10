package com.termex.app.core.ssh

import com.termex.app.domain.KnownHost
import com.termex.app.domain.KnownHostRepository
import io.mockk.mockk
import java.security.KeyPairGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HostKeyVerifierTest {

    @Test
    fun `changed host key is rejected and callback fires`() = runBlocking {
        val knownKey = generatePublicKey()
        val knownFingerprint = KeyUtils.calculateFingerprint(knownKey)
        val repo = InMemoryKnownHostRepository(
            listOf(
                KnownHost(
                    hostname = "example.com",
                    port = 22,
                    keyType = KeyUtils.getKeyTypeString(knownKey.algorithm),
                    fingerprint = knownFingerprint,
                    publicKey = KeyUtils.encodePublicKey(knownKey)
                )
            )
        )
        val verifier = TermexHostKeyVerifier(repo)
        verifier.setTarget("example.com", 22)
        verifier.primeKnownHost("example.com", 22)

        var callbackResult: HostKeyVerificationResult? = null
        verifier.setCallback(object : HostKeyVerificationCallback {
            override fun onVerificationRequiredAsync(result: HostKeyVerificationResult) {
                callbackResult = result
            }
        })

        val allowed = verifier.verifyServerKey(
            clientSession = mockk(relaxed = true),
            remoteAddress = null,
            serverKey = generatePublicKey()
        )

        assertFalse(allowed)
        assertTrue(callbackResult is HostKeyVerificationResult.Changed)
        assertTrue(verifier.getPendingVerification() is HostKeyVerificationResult.Changed)
    }

    @Test
    fun `matching known host key is accepted without callback`() = runBlocking {
        val key = generatePublicKey()
        val fingerprint = KeyUtils.calculateFingerprint(key)
        val repo = InMemoryKnownHostRepository(
            listOf(
                KnownHost(
                    hostname = "example.com",
                    port = 22,
                    keyType = KeyUtils.getKeyTypeString(key.algorithm),
                    fingerprint = fingerprint,
                    publicKey = KeyUtils.encodePublicKey(key)
                )
            )
        )
        val verifier = TermexHostKeyVerifier(repo)
        verifier.setTarget("example.com", 22)
        verifier.primeKnownHost("example.com", 22)

        var callbackCalled = false
        verifier.setCallback(object : HostKeyVerificationCallback {
            override fun onVerificationRequiredAsync(result: HostKeyVerificationResult) {
                callbackCalled = true
            }
        })

        val allowed = verifier.verifyServerKey(
            clientSession = mockk(relaxed = true),
            remoteAddress = null,
            serverKey = key
        )

        assertTrue(allowed)
        assertFalse(callbackCalled)
        assertNull(verifier.getPendingVerification())
    }

    private fun generatePublicKey() = KeyPairGenerator.getInstance("RSA").apply {
        initialize(2048)
    }.generateKeyPair().public

    private class InMemoryKnownHostRepository(
        hosts: List<KnownHost> = emptyList()
    ) : KnownHostRepository {
        private val items = MutableStateFlow(hosts)
        override fun getAllKnownHosts(): Flow<List<KnownHost>> = items

        override suspend fun getKnownHost(hostname: String, port: Int): KnownHost? {
            return items.value.firstOrNull { it.hostname == hostname && it.port == port }
        }

        override suspend fun addKnownHost(knownHost: KnownHost) {
            items.value = items.value.filterNot {
                it.hostname == knownHost.hostname && it.port == knownHost.port
            } + knownHost
        }

        override suspend fun updateKnownHost(knownHost: KnownHost) {
            addKnownHost(knownHost)
        }

        override suspend fun deleteKnownHost(knownHost: KnownHost) {
            items.value = items.value.filterNot {
                it.hostname == knownHost.hostname && it.port == knownHost.port
            }
        }

        override suspend fun deleteAll() {
            items.value = emptyList()
        }
    }
}
