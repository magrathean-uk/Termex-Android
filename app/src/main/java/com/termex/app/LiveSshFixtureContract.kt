package com.termex.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

enum class TargetHostMode {
    EMULATOR_HOST_LOOPBACK,
    ADB_REVERSE_LOOPBACK
}

data class LiveSshKeyFixture(
    val host: String,
    val port: Int,
    val username: String,
    val serverName: String,
    val keyName: String,
    val keyText: String
) {
    val connectionLabel: String
        get() = "$username@$host:$port"
}

data class LiveSshPasswordFixture(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val serverName: String
)

data class LiveSshCertificateFixture(
    val host: String,
    val port: Int,
    val username: String,
    val serverName: String,
    val keyName: String,
    val keyText: String,
    val certificateName: String,
    val certificateText: String
) {
    val connectionLabel: String
        get() = "$username@$host:$port"
}

data class LiveSshJumpFixture(
    val host: String,
    val port: Int,
    val username: String,
    val serverName: String,
    val keyName: String,
    val targetHost: String,
    val targetPort: Int
)

data class LiveSshTestFixture(
    val vmName: String,
    val distro: String,
    val targetHostMode: TargetHostMode,
    val liveKey: LiveSshKeyFixture,
    val password: LiveSshPasswordFixture,
    val certificate: LiveSshCertificateFixture?,
    val jump: LiveSshJumpFixture?
)

object LiveSshFixtureContract {
    private val jsonFormat = Json {
        ignoreUnknownKeys = true
    }

    fun fromBase64(value: String): LiveSshTestFixture {
        val decoded = Base64.getDecoder().decode(value)
        return fromJson(String(decoded, Charsets.UTF_8))
    }

    fun fromJson(value: String): LiveSshTestFixture {
        val root = jsonFormat.parseToJsonElement(value).jsonObject
        return LiveSshTestFixture(
            vmName = root.string("vmName"),
            distro = root.string("distro"),
            targetHostMode = TargetHostMode.valueOf(root.string("targetHostMode").uppercase()),
            liveKey = root.requiredObject("liveKey").toLiveKeyFixture(),
            password = root.requiredObject("password").toPasswordFixture(),
            certificate = root.optionalObject("certificate")?.toCertificateFixture(),
            jump = root.optionalObject("jump")?.toJumpFixture()
        )
    }

    private fun JsonObject.toLiveKeyFixture(): LiveSshKeyFixture {
        return LiveSshKeyFixture(
            host = string("host"),
            port = int("port"),
            username = string("username"),
            serverName = string("serverName"),
            keyName = string("keyName"),
            keyText = string("keyText")
        )
    }

    private fun JsonObject.toPasswordFixture(): LiveSshPasswordFixture {
        return LiveSshPasswordFixture(
            host = string("host"),
            port = int("port"),
            username = string("username"),
            password = string("password"),
            serverName = string("serverName")
        )
    }

    private fun JsonObject.toCertificateFixture(): LiveSshCertificateFixture {
        return LiveSshCertificateFixture(
            host = string("host"),
            port = int("port"),
            username = string("username"),
            serverName = string("serverName"),
            keyName = string("keyName"),
            keyText = string("keyText"),
            certificateName = string("certificateName"),
            certificateText = string("certificateText")
        )
    }

    private fun JsonObject.toJumpFixture(): LiveSshJumpFixture {
        return LiveSshJumpFixture(
            host = string("host"),
            port = int("port"),
            username = string("username"),
            serverName = string("serverName"),
            keyName = string("keyName"),
            targetHost = string("targetHost"),
            targetPort = int("targetPort")
        )
    }

    private fun JsonObject.string(name: String): String {
        return getValue(name).jsonPrimitive.content
    }

    private fun JsonObject.int(name: String): Int {
        return getValue(name).jsonPrimitive.content.toInt()
    }

    private fun JsonObject.requiredObject(name: String): JsonObject {
        return getValue(name).jsonObject
    }

    private fun JsonObject.optionalObject(name: String): JsonObject? {
        return get(name)?.jsonObject
    }
}
