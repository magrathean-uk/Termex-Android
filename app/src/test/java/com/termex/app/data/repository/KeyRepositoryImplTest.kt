package com.termex.app.data.repository

import android.content.Context
import com.termex.app.domain.KeyRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class KeyRepositoryImplTest {

    @Test
    fun `generateKey defaults to ed25519 and records matching metadata`() = runTest {
        val root = createTempDir(prefix = "keys-repo-ed25519")
        val repository = createRepository(root)

        repository.generateKey("id_ed25519")

        val keys = repository.getAllKeys().first()
        val key = keys.single()

        assertEquals("id_ed25519", key.name)
        assertEquals("ED25519", key.type)
        assertTrue(key.fingerprint.startsWith("SHA256:"))
        assertTrue(File(root, "ssh_keys/id_ed25519.pub").readText().startsWith("ssh-ed25519 "))
    }

    @Test
    fun `generateKey still supports rsa`() = runTest {
        val root = createTempDir(prefix = "keys-repo-rsa")
        val repository = createRepository(root)

        repository.generateKey("id_rsa", type = "RSA")

        val keys = repository.getAllKeys().first()
        val key = keys.single()

        assertEquals("RSA", key.type)
        assertTrue(File(root, "ssh_keys/id_rsa.pub").readText().startsWith("ssh-rsa "))
    }

    private fun createRepository(root: File): KeyRepository {
        val context = mockk<Context>()
        every { context.filesDir } returns root
        return KeyRepositoryImpl(context, KeyMetadataStore(context))
    }
}
