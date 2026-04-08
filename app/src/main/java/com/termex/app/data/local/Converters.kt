package com.termex.app.data.local

import androidx.room.TypeConverter
import com.termex.app.domain.PortForward
import com.termex.app.domain.PortForwardType
import java.util.Date
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    @Serializable
    private data class PortForwardPayload(
        val id: String,
        val type: String,
        val localPort: Int,
        val remoteHost: String = "localhost",
        val remotePort: Int,
        val enabled: Boolean = true,
        val bindAddress: String = "127.0.0.1"
    )

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    fun portForwardsToJson(portForwards: List<PortForward>): String? {
        if (portForwards.isEmpty()) return null
        return json.encodeToString(
            portForwards.map { pf ->
                PortForwardPayload(
                    id = pf.id,
                    type = pf.type.name,
                    localPort = pf.localPort,
                    remoteHost = pf.remoteHost,
                    remotePort = pf.remotePort,
                    enabled = pf.enabled,
                    bindAddress = pf.bindAddress
                )
            }
        )
    }

    fun portForwardsFromJson(json: String?): List<PortForward> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            this.json.decodeFromString<List<PortForwardPayload>>(json).map { payload ->
                PortForward(
                    id = payload.id,
                    type = PortForwardType.valueOf(payload.type),
                    localPort = payload.localPort,
                    remoteHost = payload.remoteHost,
                    remotePort = payload.remotePort,
                    enabled = payload.enabled,
                    bindAddress = payload.bindAddress
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
