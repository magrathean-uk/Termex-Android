package com.termex.app.data.repository

import com.termex.app.data.local.SessionStateDao
import com.termex.app.data.local.SessionStateEntity
import com.termex.app.domain.SessionState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SessionRepositoryTest {

    private lateinit var sessionStateDao: SessionStateDao
    private lateinit var repository: SessionRepository

    @Before
    fun setup() {
        sessionStateDao = mockk(relaxed = true)
        repository = SessionRepository(sessionStateDao)
    }

    @Test
    fun test_saveSession() = runTest {
        val session = SessionState(
            id = "test-id",
            serverId = "server-123",
            terminalBuffer = "ls\nfile1.txt\nfile2.txt",
            workingDirectory = "/home/user",
            connectedAt = 1000L,
            lastActiveAt = 2000L
        )

        repository.saveSession(session)

        coVerify(exactly = 1) {
            sessionStateDao.insertSession(
                withArg { entity ->
                    assertEquals("test-id", entity.id)
                    assertEquals("server-123", entity.serverId)
                    assertEquals("ls\nfile1.txt\nfile2.txt", entity.terminalBuffer)
                }
            )
        }
    }

    @Test
    fun test_getLatestSessionForServer() = runTest {
        val entity = SessionStateEntity(
            id = "test-id",
            serverId = "server-123",
            terminalBuffer = "echo hello\nhello",
            workingDirectory = null,
            connectedAt = 1000L,
            lastActiveAt = 2000L
        )

        coEvery { sessionStateDao.getLatestSessionForServer("server-123") } returns entity

        val result = repository.getLatestSessionForServer("server-123")

        assertNotNull(result)
        assertEquals("test-id", result?.id)
        assertEquals("server-123", result?.serverId)
        assertEquals("echo hello\nhello", result?.terminalBuffer)
    }

    @Test
    fun test_deleteSession() = runTest {
        repository.deleteSession("test-id")

        coVerify(exactly = 1) { sessionStateDao.deleteSession("test-id") }
    }
}
