package com.termex.app.core.ssh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient as SshjClient
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.connection.channel.direct.PTYMode
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.Security
import javax.inject.Inject
import javax.inject.Singleton

sealed class SSHConnectionState {
    data object Disconnected : SSHConnectionState()
    data object Connecting : SSHConnectionState()
    data object Connected : SSHConnectionState()
    data class Error(val message: String) : SSHConnectionState()
}

data class SSHConnectionConfig(
    val hostname: String,
    val port: Int = 22,
    val username: String,
    val password: String? = null,
    val privateKey: ByteArray? = null,
    val privateKeyPassphrase: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SSHConnectionConfig
        return hostname == other.hostname &&
                port == other.port &&
                username == other.username &&
                password == other.password &&
                privateKey.contentEquals(other.privateKey) &&
                privateKeyPassphrase == other.privateKeyPassphrase
    }

    override fun hashCode(): Int {
        var result = hostname.hashCode()
        result = 31 * result + port
        result = 31 * result + username.hashCode()
        result = 31 * result + (password?.hashCode() ?: 0)
        result = 31 * result + (privateKey?.contentHashCode() ?: 0)
        result = 31 * result + (privateKeyPassphrase?.hashCode() ?: 0)
        return result
    }
}

@Singleton
class SSHClient @Inject constructor() {

    init {
        // Android ships with a stripped-down functionality Bouncy Castle provider
        // We must remove it and add the full one to support modern algorithms like X25519
        Security.removeProvider("BC")
        Security.addProvider(BouncyCastleProvider())
    }

    private var sshClient: SshjClient? = null
    private var session: Session? = null
    private var shell: Session.Shell? = null
    
    private val _connectionState = MutableStateFlow<SSHConnectionState>(SSHConnectionState.Disconnected)
    val connectionState: StateFlow<SSHConnectionState> = _connectionState.asStateFlow()
    
    var inputStream: InputStream? = null
        private set
    var outputStream: OutputStream? = null
        private set
    
    suspend fun connect(config: SSHConnectionConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            disconnect() // Ensure fresh start
            _connectionState.value = SSHConnectionState.Connecting
            
            val client = SshjClient()
            sshClient = client
            
            // For now, accept all host keys (equivalent to StrictHostKeyChecking=no)
            client.addHostKeyVerifier(PromiscuousVerifier())
            
            // Connect
            client.connect(config.hostname, config.port)
            
            // Auth
            if (config.privateKey != null) {
                // Write key to temp file because SSHJ loadKeys expects file path
                val keyFile = File.createTempFile("ssh_key", ".pem")
                keyFile.writeBytes(config.privateKey)
                keyFile.deleteOnExit()
                
                try {
                    val keyProvider: KeyProvider = if (config.privateKeyPassphrase != null) {
                        // For encrypted keys, we need a password finder or similar. 
                        // For now simplifying to simple load, assuming unencrypted or handled by simple helper if possible.
                        // Actually, let's use the FileKeyProvider which might support PasswordFinder or direct init.
                        // client.loadKeys returns KeyProvider.
                        // If passphrase provided, we rely on SSHJ logic.
                        // But loadKeys(File) doesn't take passphrase.
                        
                        // Use a custom PasswordFinder callback
                        client.loadKeys(keyFile.absolutePath, object : net.schmizz.sshj.userauth.password.PasswordFinder {
                             override fun reqPassword(resource: net.schmizz.sshj.userauth.password.Resource<*>?) = config.privateKeyPassphrase.toCharArray()
                             override fun shouldRetry(resource: net.schmizz.sshj.userauth.password.Resource<*>?) = false
                        })
                    } else {
                        client.loadKeys(keyFile.absolutePath)
                    }
                    client.authPublickey(config.username, keyProvider)
                } finally {
                    keyFile.delete()
                }
            } else if (config.password != null) {
                client.authPassword(config.username, config.password)
            } else {
                throw IllegalArgumentException("No authentication method provided")
            }
            
            // Start Session & PTY
            val sess = client.startSession()
            session = sess
            
            // Request PTY
            sess.allocatePTY("xterm-256color", 80, 24, 0, 0, emptyMap<PTYMode, Int>())
            
            // Start Shell
            val sh = sess.startShell()
            shell = sh
            
            inputStream = sh.inputStream
            outputStream = sh.outputStream
            
            _connectionState.value = SSHConnectionState.Connected
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            _connectionState.value = SSHConnectionState.Error(e.message ?: "Connection failed")
            disconnect() // Cleanup on failure
            Result.failure(e)
        }
    }
    
    fun resizeTerminal(cols: Int, rows: Int) {
        // SSHJ doesn't strictly enforce a simple resize method in the same way, 
        // but we can try session.changeWindowDimensions if supported, or via basic request.
        // Sadly SSHJ 'allocateDefaultPTY' is a one-off. 
        // Proper PTY resize request is strictly needed for full terminal emulators.
        // We will omit strict resize logic for now or try to use reflection/extensions if needed later.
        // Note: TerminalBuffer handles local resize. Remote resize requires sending signal.
        // session?.allocatePTY(...) would restart pty? No.
        // For now, no-op to avoid crashing, as SSHJ requires channel request for window change.
    }
    
    suspend fun sendData(data: ByteArray) = withContext(Dispatchers.IO) {
        try {
            outputStream?.write(data)
            outputStream?.flush()
        } catch (e: Exception) {
            _connectionState.value = SSHConnectionState.Error("Failed to send data: ${e.message}")
        }
    }
    
    suspend fun sendData(data: String) = sendData(data.toByteArray())
    
    fun disconnect() {
        try {
            shell?.close()
            session?.close()
            sshClient?.disconnect()
            sshClient?.close()
        } catch (_: Exception) {
        } finally {
            shell = null
            session = null
            sshClient = null
            inputStream = null
            outputStream = null
            _connectionState.value = SSHConnectionState.Disconnected
        }
    }
    
    fun isConnected(): Boolean = sshClient?.isConnected == true && session?.isOpen == true
}
