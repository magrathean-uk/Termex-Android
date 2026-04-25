package com.termex.app.testing

import android.os.Build
import java.net.InetAddress
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.concurrent.thread

@Singleton
class AutomationSocketProbeHelper @Inject constructor() {

    data class BannerEndpoint(
        val host: String,
        val port: Int
    )

    private data class BannerHandle(
        val serverSocket: ServerSocket,
        val serverThread: Thread
    )

    private val bannerHandles = ConcurrentHashMap<Int, BannerHandle>()

    @Synchronized
    fun startBannerServer(banner: String): Int {
        return startBannerServer(banner, InetAddress.getByName("127.0.0.1")).port
    }

    @Synchronized
    fun startReachableBannerServer(banner: String): BannerEndpoint {
        val host = resolveReachableHost()
        return startBannerServer(banner, InetAddress.getByName("0.0.0.0")).let {
            BannerEndpoint(host = host, port = it.port)
        }
    }

    private fun startBannerServer(banner: String, bindAddress: InetAddress): BannerEndpoint {
        val serverSocket = ServerSocket(0, 50, bindAddress)
        val port = serverSocket.localPort
        val serverThread = thread(name = "termex-automation-banner-$port", isDaemon = true) {
            try {
                serverSocket.use { server ->
                    while (!server.isClosed) {
                        val client = server.accept()
                        client.use {
                            it.getOutputStream().write(banner.toByteArray(Charsets.UTF_8))
                            it.getOutputStream().flush()
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }
        bannerHandles[port] = BannerHandle(serverSocket, serverThread)
        val bindHost = bindAddress.hostAddress ?: bindAddress.hostName ?: "127.0.0.1"
        return BannerEndpoint(host = bindHost, port = port)
    }

    private fun resolveReachableHost(): String {
        val interfaces = runCatching { Collections.list(NetworkInterface.getNetworkInterfaces()) }
            .getOrDefault(emptyList())

        val address = interfaces.asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { Collections.list(it.inetAddresses).asSequence() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress && !it.isLinkLocalAddress }

        val hostAddress = address?.hostAddress
        return hostAddress?.takeIf { it.isNotBlank() }
            ?: if (isAndroidEmulator()) "10.0.2.15" else "127.0.0.1"
    }

    private fun isAndroidEmulator(): Boolean {
        return Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
            Build.MODEL.contains("sdk", ignoreCase = true) ||
            Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
            Build.HARDWARE.contains("ranchu", ignoreCase = true)
    }

    @Synchronized
    fun stopBannerServer(port: Int) {
        bannerHandles.remove(port)?.let { handle ->
            runCatching { handle.serverSocket.close() }
            runCatching { handle.serverThread.join(1_000) }
        }
    }
}
