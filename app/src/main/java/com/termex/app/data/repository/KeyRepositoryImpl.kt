package com.termex.app.data.repository

import android.content.Context
import com.termex.app.core.ssh.KeyUtils
import com.termex.app.domain.KeyRepository
import com.termex.app.domain.SSHKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.FileWriter
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keyMetadataStore: KeyMetadataStore
) : KeyRepository {

    private val keysDir: File by lazy {
        File(context.filesDir, "ssh_keys").apply { mkdirs() }
    }

    private val refreshTrigger = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(replay = 1)

    init {
        refreshTrigger.tryEmit(Unit)
    }

    override fun getAllKeys(): Flow<List<SSHKey>> = callbackFlow {
        val scope = this
        val job = scope.launch {
            merge(refreshTrigger, keyMetadataStore.changesFlow()).collect {
                val files = keysDir.listFiles { _, name -> !name.endsWith(".pub") } ?: emptyArray()
                val actualKeys = files.map { file ->
                    val pubKeyFile = File(keysDir, "${file.name}.pub")
                    val pubKeyContent = if (pubKeyFile.exists()) pubKeyFile.readText().trim() else ""

                    val keyType = if (pubKeyContent.isNotEmpty()) {
                        KeyUtils.getKeyTypeFromOpenSSH(pubKeyContent)
                    } else {
                        "UNKNOWN"
                    }

                    val fingerprint = if (pubKeyContent.isNotEmpty()) {
                        KeyUtils.calculateFingerprintFromOpenSSH(pubKeyContent) ?: ""
                    } else {
                        ""
                    }

                    SSHKey(
                        name = file.name,
                        path = file.absolutePath,
                        publicKey = pubKeyContent,
                        type = keyType,
                        fingerprint = fingerprint,
                        lastModified = Date(file.lastModified())
                    )
                }
                val metadataOnlyKeys = keyMetadataStore.getAll().filter { metadataKey ->
                    actualKeys.none { actual -> actual.path == metadataKey.path }
                }
                val keys = (actualKeys + metadataOnlyKeys).sortedBy { it.name }
                trySend(keys)
            }
        }
        awaitClose { job.cancel() }
    }

    override suspend fun generateKey(name: String, type: String, bits: Int) = withContext(Dispatchers.IO) {
        val algorithm = if (type.equals("RSA", ignoreCase = true)) "RSA" else "Ed25519"
        val keyGen = if (algorithm == "RSA") {
            KeyPairGenerator.getInstance(algorithm)
        } else {
            KeyPairGenerator.getInstance(algorithm, org.bouncycastle.jce.provider.BouncyCastleProvider())
        }
        if (algorithm == "RSA") {
            keyGen.initialize(bits)
        }
        val pair = keyGen.generateKeyPair()

        val privateKeyFile = File(keysDir, name)
        val publicKeyFile = File(keysDir, "$name.pub")

        FileWriter(privateKeyFile).use { fw ->
            JcaPEMWriter(fw).use { pemWriter ->
                pemWriter.writeObject(pair.private)
            }
        }

        val pubKeyString = if (algorithm == "RSA") {
            encodeRsaPublicKey(pair.public as RSAPublicKey, name)
        } else {
            encodeEd25519PublicKey(pair.public, name)
        }
        publicKeyFile.writeText(pubKeyString)

        keyMetadataStore.deleteByPath(privateKeyFile.absolutePath)
        refreshTrigger.emit(Unit)
    }

    private fun encodeRsaPublicKey(publicKey: RSAPublicKey, comment: String): String {
        val byteOs = java.io.ByteArrayOutputStream()
        val dos = java.io.DataOutputStream(byteOs)

        val type = "ssh-rsa".toByteArray()
        dos.writeInt(type.size)
        dos.write(type)

        val e = publicKey.publicExponent.toByteArray()
        dos.writeInt(e.size)
        dos.write(e)

        val m = publicKey.modulus.toByteArray()
        dos.writeInt(m.size)
        dos.write(m)

        val b64 = Base64.getEncoder().encodeToString(byteOs.toByteArray())
        return "ssh-rsa $b64 $comment"
    }

    private fun encodeEd25519PublicKey(publicKey: PublicKey, comment: String): String {
        val publicKeyBytes = SubjectPublicKeyInfo.getInstance(publicKey.encoded).publicKeyData.bytes
        val byteOs = ByteArrayOutputStream()
        DataOutputStream(byteOs).use { dos ->
            val type = "ssh-ed25519".toByteArray()
            dos.writeInt(type.size)
            dos.write(type)
            dos.writeInt(publicKeyBytes.size)
            dos.write(publicKeyBytes)
        }

        val b64 = Base64.getEncoder().encodeToString(byteOs.toByteArray())
        return "ssh-ed25519 $b64 $comment"
    }

    override suspend fun importKey(name: String, privateKeyContent: String, publicKeyContent: String?) = withContext(Dispatchers.IO) {
        val sanitizedName = File(name).name.replace("..", "")
        require(sanitizedName.isNotBlank()) { "Invalid key name" }
        val privateKeyFile = File(keysDir, sanitizedName)
        require(privateKeyFile.canonicalPath.startsWith(keysDir.canonicalPath)) { "Invalid key path" }
        privateKeyFile.writeText(privateKeyContent)
        
        if (publicKeyContent != null) {
            File(keysDir, "$sanitizedName.pub").writeText(publicKeyContent)
        }

        keyMetadataStore.deleteByPath(privateKeyFile.absolutePath)
        refreshTrigger.emit(Unit)
    }

    override suspend fun deleteKey(key: SSHKey) = withContext(Dispatchers.IO) {
        File(key.path).delete()
        File(key.path + ".pub").delete()
        keyMetadataStore.deleteByPath(key.path)
        refreshTrigger.emit(Unit)
    }

    override fun getKeyPath(name: String): String {
        return File(keysDir, name).absolutePath
    }
}
