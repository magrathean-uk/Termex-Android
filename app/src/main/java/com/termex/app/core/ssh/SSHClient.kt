package com.termex.app.core.ssh

import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.Properties
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
    
    private val jsch = JSch()
    private var session: Session? = null
    private var channel: ChannelShell? = null
    
    private val _connectionState = MutableStateFlow<SSHConnectionState>(SSHConnectionState.Disconnected)
    val connectionState: StateFlow<SSHConnectionState> = _connectionState.asStateFlow()
    
    var inputStream: InputStream? = null
        private set
    var outputStream: OutputStream? = null
        private set
    
    suspend fun connect(config: SSHConnectionConfig): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = SSHConnectionState.Connecting
            
            // Add private key if provided
            config.privateKey?.let { key ->
                jsch.addIdentity(
                    "key",
                    key,
                    null,
                    config.privateKeyPassphrase?.toByteArray()
                )
            }
            
            // Create session
            session = jsch.getSession(config.username, config.hostname, config.port).apply {
                // Set password if provided
                config.password?.let { setPassword(it) }
                
                // Configure session
                val props = Properties().apply {
                    put("StrictHostKeyChecking", "no")
                    put("PreferredAuthentications", "publickey,keyboard-interactive,password")
                }
                setConfig(props)
                
                // Connect with timeout
                connect(30_000)
            }
            
            // Open shell channel
            channel = (session?.openChannel("shell") as? ChannelShell)?.apply {
                setPtyType("xterm-256color", 80, 24, 0, 0)
                inputStream = this.inputStream
                outputStream = this.outputStream
                connect(10_000)
            }
            
            _connectionState.value = SSHConnectionState.Connected
            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = SSHConnectionState.Error(e.message ?: "Connection failed")
            Result.failure(e)
        }
    }
    
    fun resizeTerminal(cols: Int, rows: Int) {
        channel?.setPtySize(cols, rows, cols * 8, rows * 16)
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
            channel?.disconnect()
            session?.disconnect()
        } catch (_: Exception) {
            // Ignore disconnect errors
        } finally {
            channel = null
            session = null
            inputStream = null
            outputStream = null
            _connectionState.value = SSHConnectionState.Disconnected
        }
    }
    
    fun isConnected(): Boolean = session?.isConnected == true && channel?.isConnected == true
}
