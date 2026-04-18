package com.termex.app.core.sync

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

class GoogleDriveAppDataFolderClient(
    private val accessToken: String,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val snapshotFileName: String = DEFAULT_SNAPSHOT_FILE_NAME
) {
    suspend fun listSnapshotFiles(): List<DriveAppDataFile> = withContext(Dispatchers.IO) {
        val query = "name='$snapshotFileName' and 'appDataFolder' in parents and trashed=false"
        val url = buildUrl(
            "/files",
            mapOf(
                "q" to query,
                "spaces" to "appDataFolder",
                "fields" to "files(id,name,modifiedTime,md5Checksum,size)"
            )
        )
        val response = executeJsonRequest(url, "GET")
        metadataJson.decodeFromString(DriveFileListResponse.serializer(), response).files
    }

    suspend fun readSnapshot(): MetadataSnapshot? = withContext(Dispatchers.IO) {
        val file = listSnapshotFiles().firstOrNull() ?: return@withContext null
        val url = buildUrl("/files/${file.id}", mapOf("alt" to "media"))
        val body = executeTextRequest(url, "GET")
        metadataSnapshotJson.decodeFromString(MetadataSnapshot.serializer(), body)
    }

    suspend fun writeSnapshot(snapshot: MetadataSnapshot): DriveAppDataFile = withContext(Dispatchers.IO) {
        val existing = listSnapshotFiles().firstOrNull()
        val path = if (existing == null) "/upload/drive/v3/files" else "/upload/drive/v3/files/${existing.id}"
        val methodOverride = if (existing == null) null else "PATCH"
        val metadata = if (existing == null) {
            DriveUploadMetadata(name = snapshotFileName, parents = listOf("appDataFolder"))
        } else {
            DriveUploadMetadata(name = snapshotFileName)
        }
        val response = executeMultipartUpload(
            url = buildUploadUrl(
                path = path,
                query = mapOf("uploadType" to "multipart", "fields" to "id,name,modifiedTime,md5Checksum,size")
            ),
            methodOverride = methodOverride,
            metadataJson = metadataJson.encodeToString(DriveUploadMetadata.serializer(), metadata),
            mediaJson = snapshot.toJson()
        )
        metadataJson.decodeFromString(DriveAppDataFile.serializer(), response)
    }

    private fun buildUrl(path: String, query: Map<String, String>): URL {
        return buildUrlAgainst(baseUrl, path, query)
    }

    private fun buildUploadUrl(path: String, query: Map<String, String>): URL {
        return buildUrlAgainst(uploadBaseUrl(), path, query)
    }

    private fun buildUrlAgainst(base: String, path: String, query: Map<String, String>): URL {
        val queryString = query.entries.joinToString("&") { (key, value) ->
            "${encodeQuery(key)}=${encodeQuery(value)}"
        }
        val normalizedBase = base.trimEnd('/')
        val normalizedPath = path.trimStart('/')
        val full = if (queryString.isBlank()) {
            "$normalizedBase/$normalizedPath"
        } else {
            "$normalizedBase/$normalizedPath?$queryString"
        }
        return URL(full)
    }

    private fun executeJsonRequest(url: URL, method: String): String {
        val connection = openConnection(url, method)
        return readResponse(connection)
    }

    private fun executeTextRequest(url: URL, method: String): String {
        val connection = openConnection(url, method)
        return readResponse(connection)
    }

    private fun executeMultipartUpload(
        url: URL,
        methodOverride: String?,
        metadataJson: String,
        mediaJson: String
    ): String {
        val boundary = "----TermexSyncBoundary${System.nanoTime()}"
        val connection = openConnection(url, "POST")
        methodOverride?.let { connection.setRequestProperty("X-HTTP-Method-Override", it) }
        connection.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        connection.doOutput = true

        val payload = buildMultipartBody(boundary, metadataJson, mediaJson).toByteArray(StandardCharsets.UTF_8)
        connection.outputStream.use { output ->
            output.write(payload)
        }

        return readResponse(connection)
    }

    private fun buildMultipartBody(boundary: String, metadataJson: String, mediaJson: String): String {
        return buildString {
            append("--")
            append(boundary)
            append("\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadataJson)
            append("\r\n--")
            append(boundary)
            append("\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(mediaJson)
            append("\r\n--")
            append(boundary)
            append("--\r\n")
        }
    }

    private fun openConnection(url: URL, method: String): HttpURLConnection {
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 15_000
            instanceFollowRedirects = true
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Accept", "application/json")
            if (method != "GET") {
                doOutput = true
            }
        }
    }

    private fun readResponse(connection: HttpURLConnection): String {
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.readText().orEmpty()
        if (code !in 200..299) {
            throw IOException("Drive request failed ($code): $body")
        }
        return body
    }

    private fun encodeQuery(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun uploadBaseUrl(): String {
        val normalized = baseUrl.trimEnd('/')
        val marker = "/drive/v3"
        return if (normalized.endsWith(marker)) {
            normalized.removeSuffix(marker)
        } else {
            normalized
        }
    }

    private fun InputStream.readText(): String = reader(StandardCharsets.UTF_8).use { it.readText() }

    companion object {
        private const val DEFAULT_BASE_URL = "https://www.googleapis.com/drive/v3"
        private const val DEFAULT_SNAPSHOT_FILE_NAME = "termex-metadata-snapshot.json"
    }
}

@Serializable
data class DriveAppDataFile(
    val id: String,
    val name: String,
    val modifiedTime: String? = null,
    val md5Checksum: String? = null,
    val size: String? = null
)

@Serializable
private data class DriveFileListResponse(
    @SerialName("files") val files: List<DriveAppDataFile> = emptyList()
)

@Serializable
private data class DriveUploadMetadata(
    val name: String,
    val parents: List<String>? = null
)

private val metadataJson = kotlinx.serialization.json.Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = true
}
