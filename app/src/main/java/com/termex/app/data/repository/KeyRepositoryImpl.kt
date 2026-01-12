package com.termex.app.data.repository

import android.content.Context
import com.jcraft.jsch.JSch
import com.jcraft.jsch.KeyPair
import com.termex.app.domain.KeyRepository
import com.termex.app.domain.SSHKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : KeyRepository {

    private val keysDir: File by lazy {
        File(context.filesDir, "ssh_keys").apply { mkdirs() }
    }
    
    // Simple file watcher mechanism (using flow emit on changes)
    private val refreshTrigger = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(replay = 1)

    init {
        refreshTrigger.tryEmit(Unit)
    }

    override fun getAllKeys(): Flow<List<SSHKey>> = callbackFlow {
        val scope = this
        val job = scope.launch {
            refreshTrigger.collect {
                val files = keysDir.listFiles { _, name -> !name.endsWith(".pub") } ?: emptyArray()
                val keys = files.map { file ->
                    val pubKeyFile = File(keysDir, "${file.name}.pub")
                    val pubKeyContent = if (pubKeyFile.exists()) pubKeyFile.readText().trim() else ""
                    
                    SSHKey(
                        name = file.name,
                        path = file.absolutePath,
                        publicKey = pubKeyContent,
                        type = if (pubKeyContent.startsWith("ssh-rsa")) "RSA" else "UNKNOWN", // Simplified type check
                        lastModified = Date(file.lastModified())
                    )
                }.sortedBy { it.name }
                trySend(keys)
            }
        }
        awaitClose { job.cancel() }
    }

    override suspend fun generateKey(name: String, type: String, bits: Int) = withContext(Dispatchers.IO) {
        val jsch = JSch()
        val keyPair = KeyPair.genKeyPair(jsch, KeyPair.RSA, bits)
        
        val privateKeyFile = File(keysDir, name)
        val publicKeyFile = File(keysDir, "$name.pub")
        
        val privOut = FileOutputStream(privateKeyFile)
        val pubOut = FileOutputStream(publicKeyFile)
        
        keyPair.writePrivateKey(privOut)
        keyPair.writePublicKey(pubOut, name)
        
        privOut.close()
        pubOut.close()
        keyPair.dispose()
        
        refreshTrigger.emit(Unit)
    }

    override suspend fun importKey(name: String, privateKeyContent: String, publicKeyContent: String?) = withContext(Dispatchers.IO) {
        val privateKeyFile = File(keysDir, name)
        privateKeyFile.writeText(privateKeyContent)
        
        if (publicKeyContent != null) {
            File(keysDir, "$name.pub").writeText(publicKeyContent)
        }
        
        refreshTrigger.emit(Unit)
    }

    override suspend fun deleteKey(key: SSHKey) = withContext(Dispatchers.IO) {
        File(key.path).delete()
        File(key.path + ".pub").delete()
        refreshTrigger.emit(Unit)
    }
    
    override fun getKeyPath(name: String): String {
        return File(keysDir, name).absolutePath
    }
}
