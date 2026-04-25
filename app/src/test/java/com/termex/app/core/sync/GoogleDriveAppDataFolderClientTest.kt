package com.termex.app.core.sync

import com.termex.app.domain.AuthMode
import com.termex.app.domain.Server
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class GoogleDriveAppDataFolderClientTest {
    @Test
    fun `client lists reads and writes the snapshot file`() = runBlocking {
        val snapshot = MetadataSnapshot.fromDomain(
            servers = listOf(
                Server(
                    id = "server-1",
                    name = "Alpha",
                    hostname = "alpha.example.com",
                    username = "root",
                    authMode = AuthMode.KEY,
                    keyId = "/keys/id_ed25519"
                )
            ),
            keys = emptyList(),
            certificates = emptyList(),
            workplaces = emptyList(),
            snippets = emptyList(),
            createdAtMillis = 1_700_000_000_000
        )
        val snapshotV2 = snapshot.copy(createdAtMillis = 1_700_000_000_100)
        val snapshotFileId = "snapshot-file-1"
        val latestSnapshotJson = AtomicReference(snapshot.toJson())
        val fileExists = AtomicBoolean(false)
        val lastUploadMethod = AtomicReference("")
        val lastUploadOverride = AtomicReference<String?>(null)
        val lastUploadBody = AtomicReference("")

        val server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when {
                    request.method == "GET" && request.path?.startsWith("/drive/v3/files?") == true -> {
                        val body = if (fileExists.get()) {
                            """{"files":[{"id":"$snapshotFileId","name":"termex-metadata-snapshot.json","modifiedTime":"2024-01-01T00:00:00Z","md5Checksum":"abc","size":"${latestSnapshotJson.get().length}"}]}"""
                        } else {
                            """{"files":[]}"""
                        }
                        MockResponse()
                            .setResponseCode(200)
                            .addHeader("Content-Type", "application/json")
                            .setBody(body)
                    }

                    request.method == "GET" && request.path?.contains("/drive/v3/files/$snapshotFileId?alt=media") == true -> {
                        MockResponse()
                            .setResponseCode(200)
                            .addHeader("Content-Type", "application/json")
                            .setBody(latestSnapshotJson.get())
                    }

                    request.method == "POST" && request.path?.contains("/upload/drive/v3/files") == true -> {
                        lastUploadMethod.set(request.method)
                        lastUploadOverride.set(request.getHeader("X-HTTP-Method-Override"))
                        lastUploadBody.set(request.body.readUtf8())
                        fileExists.set(true)
                        MockResponse()
                            .setResponseCode(200)
                            .addHeader("Content-Type", "application/json")
                            .setBody(
                                """{"id":"$snapshotFileId","name":"termex-metadata-snapshot.json","modifiedTime":"2024-01-01T00:00:00Z","md5Checksum":"abc","size":"${latestSnapshotJson.get().length}"}"""
                            )
                    }

                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        server.start()
        try {
            val client = GoogleDriveAppDataFolderClient(
                accessToken = "token",
                baseUrl = server.url("/drive/v3").toString().removeSuffix("/")
            )

            val created = client.writeSnapshot(snapshot)
            assertEquals(snapshotFileId, created.id)
            assertEquals("POST", lastUploadMethod.get())
            assertNull(lastUploadOverride.get())
            assertTrue(lastUploadBody.get().contains("termex-metadata-snapshot.json"))
            assertTrue(lastUploadBody.get().contains(snapshot.toJson()))

            val listed = client.listSnapshotFiles()
            assertEquals(1, listed.size)
            assertEquals(snapshotFileId, listed.single().id)

            val read = client.readSnapshot()
            assertEquals(snapshot, read)

            latestSnapshotJson.set(snapshotV2.toJson())
            client.writeSnapshot(snapshotV2)

            assertEquals("POST", lastUploadMethod.get())
            assertEquals("PATCH", lastUploadOverride.get())
            assertTrue(lastUploadBody.get().contains(snapshotV2.toJson()))
        } finally {
            server.shutdown()
        }
    }
}
