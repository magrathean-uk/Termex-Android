package com.termex.app.core.ssh

import com.termex.app.domain.AuthMode
import com.termex.app.domain.Server
import java.io.File

data class SSHConfigHost(
    val host: String,
    val hostname: String?,
    val user: String?,
    val port: Int?,
    val identityFile: String?,
    val proxyJump: String?,
    val forwardAgent: Boolean?
)

object SSHConfigParser {

    /**
     * Parse SSH config file content and extract host configurations.
     * Supports: Host, HostName, User, Port, IdentityFile, ProxyJump, ForwardAgent
     */
    fun parse(content: String): List<SSHConfigHost> {
        val hosts = mutableListOf<SSHConfigHost>()
        var currentHost: String? = null
        var hostname: String? = null
        var user: String? = null
        var port: Int? = null
        var identityFile: String? = null
        var proxyJump: String? = null
        var forwardAgent: Boolean? = null

        fun saveCurrentHost() {
            if (currentHost != null && currentHost != "*") {
                hosts.add(
                    SSHConfigHost(
                        host = currentHost!!,
                        hostname = hostname,
                        user = user,
                        port = port,
                        identityFile = identityFile,
                        proxyJump = proxyJump,
                        forwardAgent = forwardAgent
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
                    proxyJump = null
                    forwardAgent = null
                }
                "hostname" -> hostname = value
                "user" -> user = value
                "port" -> port = value.toIntOrNull()
                "identityfile" -> identityFile = expandPath(value)
                "proxyjump" -> proxyJump = value
                "forwardagent" -> forwardAgent = value.lowercase() == "yes"
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
                authMode = if (host.identityFile != null) AuthMode.KEY else AuthMode.PASSWORD,
                forwardAgent = host.forwardAgent ?: false
            )
        }
    }

    private fun expandPath(path: String): String {
        return if (path.startsWith("~")) {
            path.replaceFirst("~", System.getProperty("user.home") ?: "")
        } else {
            path
        }
    }
}
