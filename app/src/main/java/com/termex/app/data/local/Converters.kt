package com.termex.app.data.local

import androidx.room.TypeConverter
import com.termex.app.domain.PortForward
import com.termex.app.domain.PortForwardType
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

class Converters {
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
        val jsonArray = JSONArray()
        portForwards.forEach { pf ->
            val obj = JSONObject().apply {
                put("id", pf.id)
                put("type", pf.type.name)
                put("localPort", pf.localPort)
                put("remoteHost", pf.remoteHost)
                put("remotePort", pf.remotePort)
                put("enabled", pf.enabled)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    fun portForwardsFromJson(json: String?): List<PortForward> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            val jsonArray = JSONArray(json)
            (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                PortForward(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    type = PortForwardType.valueOf(obj.optString("type", "LOCAL")),
                    localPort = obj.getInt("localPort"),
                    remoteHost = obj.optString("remoteHost", "localhost"),
                    remotePort = obj.getInt("remotePort"),
                    enabled = obj.optBoolean("enabled", true)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
