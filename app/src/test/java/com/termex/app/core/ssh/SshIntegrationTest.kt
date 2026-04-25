package com.termex.app.core.ssh

import com.termex.app.domain.KnownHost
import com.termex.app.domain.KnownHostRepository
import com.termex.app.domain.PortForward
import com.termex.app.domain.PortForwardType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap

class SshIntegrationTest {

    @Test
    fun ssh_connect_and_forwarding_smoke() = runBlocking {
        val config = loadConfig()
        assumeTrue(config != null)

        val repo = InMemoryKnownHostRepository()
        val verifier = TermexHostKeyVerifier(repo)
        val client = SSHClient(verifier)
        client.setHostKeyVerificationCallback(object : HostKeyVerificationCallback {
            override fun onVerificationRequiredAsync(result: HostKeyVerificationResult) {
                runBlocking {
                    verifier.trustHostKey(result)
                }
            }
        })

        val connectResult = client.connect(config!!)
        connectResult.exceptionOrNull()?.printStackTrace()
        assertTrue("SSH connect failed: ${connectResult.exceptionOrNull()?.message}", connectResult.isSuccess)

        client.sendData("echo TERMEX_TEST\n")
        val shellOutput = readUntil(client.inputStream!!, "TERMEX_TEST", 5_000)
        assertTrue(shellOutput.contains("TERMEX_TEST"))

        // Ensure host key stored
        val knownHost = repo.getKnownHost(config.hostname, config.port)
        assertTrue(knownHost != null)

        val pfManager = PortForwardManager()
        pfManager.setClient("integration", client)

        // Local port forward: localhost:localPort -> remoteHost:remotePort
        val localPort = pickFreePort()
        val localForward = PortForward(
            type = PortForwardType.LOCAL,
            localPort = localPort,
            remoteHost = config.hostname,
            remotePort = config.port
        )
        pfManager.initializeForwards("integration", listOf(localForward))
        assertTrue(pfManager.startForward("integration", localForward).isSuccess)
        delay(200)
        val banner = readBannerFromSocket("127.0.0.1", localPort)
        assertTrue(banner.startsWith("SSH-"))
        pfManager.stopForward("integration", localForward.id)

        // Dynamic SOCKS5 forward
        val socksPort = pickFreePort()
        val dynamicForward = PortForward(
            type = PortForwardType.DYNAMIC,
            localPort = socksPort,
            remoteHost = "localhost",
            remotePort = 0
        )
        pfManager.initializeForwards("integration", listOf(dynamicForward))
        assertTrue(pfManager.startForward("integration", dynamicForward).isSuccess)
        delay(200)
        val socksBanner = readBannerViaSocks("127.0.0.1", socksPort, config.hostname, config.port)
        assertTrue(socksBanner.startsWith("SSH-"))
        pfManager.stopForward("integration", dynamicForward.id)

        client.disconnect()
    }

    private fun loadConfig(): SSHConnectionConfig? {
        val host = env("TERMEX_TEST_SSH_HOST") ?: return null
        val user = env("TERMEX_TEST_SSH_USER") ?: return null
        val port = env("TERMEX_TEST_SSH_PORT")?.toIntOrNull() ?: 22
        val keyPath = env("TERMEX_TEST_SSH_KEY_PATH") ?: defaultKeyPath()
        val password = env("TERMEX_TEST_SSH_PASSWORD")

        val keyBytes = keyPath?.let { path ->
            val file = java.io.File(path)
            if (file.exists()) file.readBytes() else null
        }

        if (keyBytes == null && password.isNullOrBlank()) return null

        return SSHConnectionConfig(
            hostname = host,
            port = port,
            username = user,
            password = password,
            privateKey = keyBytes,
            keepAliveIntervalSeconds = 15,
            connectTimeoutMillis = 5_000,
            readTimeoutMillis = 2_000,
            authPreference = if (keyBytes != null) AuthPreference.KEY else AuthPreference.PASSWORD,
            verifyHostKeyCertificates = false
        )
    }

    private fun env(key: String): String? = System.getenv(key)?.takeIf { it.isNotBlank() }

    private fun defaultKeyPath(): String? {
        val home = System.getProperty("user.home") ?: return null
        val candidates = listOf(
            "$home/.ssh/id_ed25519",
            "$home/.ssh/id_ecdsa",
            "$home/.ssh/id_rsa"
        )
        return candidates.firstOrNull { java.io.File(it).exists() }
    }

    private fun readUntil(input: InputStream, expected: String, timeoutMs: Long): String {
        val deadline = System.currentTimeMillis() + timeoutMs
        val buffer = StringBuilder()
        val temp = ByteArray(2048)
        while (System.currentTimeMillis() < deadline) {
            if (input.available() == 0) {
                Thread.sleep(50)
                continue
            }
            val read = input.read(temp)
            if (read > 0) {
                buffer.append(String(temp, 0, read, Charsets.UTF_8))
                if (buffer.contains(expected)) {
                    return buffer.toString()
                }
            } else if (read == -1) {
                break
            }
        }
        return buffer.toString()
    }

    private fun pickFreePort(): Int {
        ServerSocket(0).use { socket ->
            return socket.localPort
        }
    }

    private fun readBannerFromSocket(host: String, port: Int): String {
        Socket(host, port).use { socket ->
            socket.soTimeout = 2_000
            val input = socket.getInputStream()
            val buffer = ByteArray(256)
            val read = input.read(buffer)
            return if (read > 0) String(buffer, 0, read, StandardCharsets.US_ASCII) else ""
        }
    }

    private fun readBannerViaSocks(
        socksHost: String,
        socksPort: Int,
        targetHost: String,
        targetPort: Int
    ): String {
        Socket(socksHost, socksPort).use { socket ->
            socket.soTimeout = 2_000
            val input = socket.getInputStream()
            val output = socket.getOutputStream()

            // Greeting: ver=5, nmethods=1, method=0 (no auth)
            output.write(byteArrayOf(0x05, 0x01, 0x00))
            output.flush()
            input.readNBytes(ByteArray(2), 0, 2)

            // Connect request
            val hostBytes = targetHost.toByteArray(StandardCharsets.UTF_8)
            val portHi = (targetPort shr 8) and 0xFF
            val portLo = targetPort and 0xFF
            val request = ByteArray(7 + hostBytes.size)
            request[0] = 0x05
            request[1] = 0x01
            request[2] = 0x00
            request[3] = 0x03
            request[4] = hostBytes.size.toByte()
            System.arraycopy(hostBytes, 0, request, 5, hostBytes.size)
            request[5 + hostBytes.size] = portHi.toByte()
            request[6 + hostBytes.size] = portLo.toByte()
            output.write(request)
            output.flush()

            val response = ByteArray(10 + hostBytes.size)
            if (input.readNBytes(response, 0, 4) != 4) return ""
            if (response[1].toInt() != 0x00) return ""
            val atyp = response[3].toInt() and 0xFF
            val addrLen = when (atyp) {
                0x01 -> 4
                0x04 -> 16
                0x03 -> {
                    val len = input.read()
                    if (len <= 0) return ""
                    len
                }
                else -> return ""
            }
            if (addrLen > 0) {
                input.readNBytes(ByteArray(addrLen), 0, addrLen)
            }
            input.readNBytes(ByteArray(2), 0, 2)

            val banner = ByteArray(256)
            val read = input.read(banner)
            return if (read > 0) String(banner, 0, read, StandardCharsets.US_ASCII) else ""
        }
    }

    private class InMemoryKnownHostRepository : KnownHostRepository {
        private val store = ConcurrentHashMap<String, KnownHost>()
        private val flow = MutableStateFlow<List<KnownHost>>(emptyList())

        override fun getAllKnownHosts(): Flow<List<KnownHost>> = flow

        override suspend fun getKnownHost(hostname: String, port: Int): KnownHost? {
            return store["$hostname:$port"]
        }

        override suspend fun addKnownHost(knownHost: KnownHost) {
            store[knownHost.hostKey] = knownHost
            flow.value = store.values.toList()
        }

        override suspend fun updateKnownHost(knownHost: KnownHost) {
            store[knownHost.hostKey] = knownHost
            flow.value = store.values.toList()
        }

        override suspend fun deleteKnownHost(knownHost: KnownHost) {
            store.remove(knownHost.hostKey)
            flow.value = store.values.toList()
        }

        override suspend fun deleteAll() {
            store.clear()
            flow.value = emptyList()
        }
    }
}
