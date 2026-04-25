package com.termex.app.ui.viewmodel

import com.termex.app.domain.AuthMode
import com.termex.app.domain.Server
import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticsViewModelTest {

    @Test
    fun `count credential issues flags missing key password and jump host`() {
        val servers = listOf(
            Server(
                id = "missing-key",
                name = "Missing Key",
                hostname = "one.example.com",
                username = "root",
                authMode = AuthMode.KEY,
                keyId = "/definitely/missing/key"
            ),
            Server(
                id = "missing-password",
                name = "Missing Password",
                hostname = "two.example.com",
                username = "root",
                authMode = AuthMode.PASSWORD,
                passwordKeychainID = "pwd_missing"
            ),
            Server(
                id = "missing-jump",
                name = "Missing Jump",
                hostname = "three.example.com",
                username = "root",
                jumpHostId = "does-not-exist"
            ),
            Server(
                id = "healthy",
                name = "Healthy",
                hostname = "four.example.com",
                username = "root",
                jumpHostId = "missing-key"
            )
        )

        val issueCount = countCredentialIssues(servers) { keyId ->
            if (keyId == "pwd_missing") null else "secret"
        }

        assertEquals(3, issueCount)
    }
}
