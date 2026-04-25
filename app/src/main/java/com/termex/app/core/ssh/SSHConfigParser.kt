package com.termex.app.core.ssh

import com.termex.app.domain.AuthMode
import com.termex.app.domain.PortForward
import com.termex.app.domain.PortForwardType
import com.termex.app.domain.Server
import com.termex.app.domain.SSHCertificate
import com.termex.app.domain.SSHKey
import java.io.File
import java.util.Base64

data class SSHConfigHost(
    val host: String,
    val hostname: String?,
    val user: String?,
    val port: Int?,
    val identityFile: String?,
    val certificateFile: String?,
    val proxyJump: String?,
    val forwardAgent: Boolean?,
    val identitiesOnly: Boolean?,
    val portForwards: List<PortForward> = emptyList()
)

object SSHConfigParser {

    /**
     * Parse SSH config file content and extract host configurations.
     * Supports: Host, HostName, User, Port, IdentityFile, CertificateFile, ProxyJump,
     * ForwardAgent, IdentitiesOnly, LocalForward, RemoteForward, DynamicForward
     */
    fun parse(content: String): List<SSHConfigHost> {
        val hosts = mutableListOf<SSHConfigHost>()
        var currentHost: String? = null
        var hostname: String? = null
        var user: String? = null
        var port: Int? = null
        var identityFile: String? = null
        var certificateFile: String? = null
        var proxyJump: String? = null
        var forwardAgent: Boolean? = null
        var identitiesOnly: Boolean? = null
        val portForwards = mutableListOf<PortForward>()

        fun saveCurrentHost() {
            if (currentHost != null && currentHost != "*") {
                hosts.add(
                    SSHConfigHost(
                        host = currentHost!!,
                        hostname = hostname,
                        user = user,
                        port = port,
                        identityFile = identityFile,
                        certificateFile = certificateFile,
                        proxyJump = proxyJump,
                        forwardAgent = forwardAgent,
                        identitiesOnly = identitiesOnly,
                        portForwards = portForwards.toList()
                    )
                )
            }
        }

        content.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach

            val parts = trimmed.split(Regex("\\s+"), 2)
            if (parts.size < 2) return@forEach

            val key = parts[0].lowercase()
            val value = parts[1]

            when (key) {
                "host" -> {
                    saveCurrentHost()
                    currentHost = value
                    hostname = null
                    user = null
                    port = null
                    identityFile = null
                    certificateFile = null
                    proxyJump = null
                    forwardAgent = null
                    identitiesOnly = null
                    portForwards.clear()
                }
                "hostname" -> hostname = value
                "user" -> user = value
                "port" -> port = value.toIntOrNull()
                "identityfile" -> identityFile = value
                "certificatefile" -> certificateFile = value
                "proxyjump" -> proxyJump = value
                "forwardagent" -> forwardAgent = value.lowercase() == "yes"
                "identitiesonly" -> identitiesOnly = value.lowercase() == "yes"
                "localforward" -> parseLocalForward(value)?.let(portForwards::add)
                "remoteforward" -> parseRemoteForward(value)?.let(portForwards::add)
                "dynamicforward" -> parseDynamicForward(value)?.let(portForwards::add)
            }
        }

        saveCurrentHost()
        return hosts
    }

    /**
     * Parse SSH config from a file path.
     */
    fun parseFile(path: String): List<SSHConfigHost> {
        val file = File(path)
        return if (file.exists()) {
            parse(file.readText())
        } else {
            emptyList()
        }
    }

    /**
     * Convert parsed SSH config hosts to Server domain objects.
     */
    fun toServers(hosts: List<SSHConfigHost>): List<Server> {
        return hosts.mapNotNull { host ->
            val hostname = host.hostname ?: host.host
            val user = host.user ?: return@mapNotNull null

            Server(
                name = host.host,
                hostname = hostname,
                port = host.port ?: 22,
                username = user,
                authMode = if (host.identityFile != null || host.certificateFile != null) AuthMode.KEY else AuthMode.PASSWORD,
                keyId = host.identityFile,
                certificatePath = host.certificateFile,
                forwardAgent = host.forwardAgent ?: false,
                identitiesOnly = host.identitiesOnly ?: false,
                portForwards = host.portForwards
            )
        }
    }

    fun encodePortForwards(forwards: List<PortForward>): String {
        if (forwards.isEmpty()) return ""
        val raw = forwards.joinToString(separator = "\u001E") { forward ->
            listOf(
                forward.id,
                forward.type.name,
                forward.localPort.toString(),
                forward.remoteHost,
                forward.remotePort.toString(),
                forward.enabled.toString(),
                forward.bindAddress
            ).joinToString(separator = "\u001F")
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray(Charsets.UTF_8))
    }

    fun decodePortForwards(encoded: String): List<PortForward> {
        if (encoded.isBlank()) return emptyList()
        val raw = try {
            String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            return emptyList()
        }
        return raw.split('\u001E')
            .mapNotNull { row ->
                val parts = row.split('\u001F')
                if (parts.size < 7) return@mapNotNull null
                val type = runCatching { PortForwardType.valueOf(parts[1]) }.getOrNull() ?: return@mapNotNull null
                val localPort = parts[2].toIntOrNull() ?: return@mapNotNull null
                val remotePort = parts[4].toIntOrNull() ?: return@mapNotNull null
                PortForward(
                    id = parts[0],
                    type = type,
                    localPort = localPort,
                    remoteHost = parts[3],
                    remotePort = remotePort,
                    enabled = parts[5].toBooleanStrictOrNull() ?: true,
                    bindAddress = parts[6]
                )
            }
    }

    fun findMatchingImportedKeyPath(identityFile: String?, keys: List<SSHKey>): String? {
        val basename = identityFile?.let { File(it).name } ?: return null
        return keys.firstOrNull { it.name == basename || File(it.path).name == basename }?.path
    }

    fun findMatchingImportedCertificatePath(certificateFile: String?, certificates: List<SSHCertificate>): String? {
        val basename = certificateFile?.let { File(it).name } ?: return null
        return certificates.firstOrNull { it.name == basename || File(it.path).name == basename }?.path
    }

    fun resolveJumpHostId(proxyJump: String?, availableServers: List<Server>): String? {
        val token = proxyJump?.split(",")?.firstOrNull()?.trim().orEmpty()
        if (token.isBlank()) return null
        val parts = token.split("@", limit = 2)
        val user = if (parts.size == 2) parts[0] else null
        val hostPort = if (parts.size == 2) parts[1] else parts[0]
        val hostParts = hostPort.split(":", limit = 2)
        val host = hostParts[0]
        val port = hostParts.getOrNull(1)?.toIntOrNull()
        return availableServers.firstOrNull { server ->
            server.hostname == host &&
                (port == null || server.port == port) &&
                (user == null || server.username == user)
        }?.id
    }

    private data class AddressAndPort(
        val address: String?,
        val port: Int
    )

    private data class HostAndPort(
        val host: String,
        val port: Int
    )

    private fun parseLocalForward(value: String): PortForward? {
        val parts = value.split(Regex("\\s+"), limit = 2)
        if (parts.size < 2) return null
        val bindAndPort = parseAddressAndPort(parts[0]) ?: return null
        val target = parseHostAndPort(parts[1]) ?: return null
        return PortForward(
            type = PortForwardType.LOCAL,
            localPort = bindAndPort.port,
            remoteHost = target.host,
            remotePort = target.port,
            bindAddress = bindAndPort.address ?: "127.0.0.1"
        )
    }

    private fun parseRemoteForward(value: String): PortForward? {
        val parts = value.split(Regex("\\s+"), limit = 2)
        if (parts.size < 2) return null
        val bindAndPort = parseAddressAndPort(parts[0]) ?: return null
        val target = parseHostAndPort(parts[1]) ?: return null
        return PortForward(
            type = PortForwardType.REMOTE,
            localPort = target.port,
            remoteHost = target.host,
            remotePort = bindAndPort.port,
            bindAddress = bindAndPort.address ?: "127.0.0.1"
        )
    }

    private fun parseDynamicForward(value: String): PortForward? {
        val bindAndPort = parseAddressAndPort(value) ?: return null
        return PortForward(
            type = PortForwardType.DYNAMIC,
            localPort = bindAndPort.port,
            remoteHost = "localhost",
            remotePort = 0,
            bindAddress = bindAndPort.address ?: "127.0.0.1"
        )
    }

    private fun parseAddressAndPort(value: String): AddressAndPort? {
        val parts = value.split(":", limit = 2)
        return if (parts.size == 1) {
            AddressAndPort(address = null, port = parts[0].toIntOrNull() ?: return null)
        } else {
            AddressAndPort(
                address = parts[0].ifBlank { null },
                port = parts[1].toIntOrNull() ?: return null
            )
        }
    }

    private fun parseHostAndPort(value: String): HostAndPort? {
        val parts = value.split(":", limit = 2)
        if (parts.size < 2) return null
        val host = parts[0].ifBlank { return null }
        val port = parts[1].toIntOrNull() ?: return null
        return HostAndPort(host = host, port = port)
    }
}
