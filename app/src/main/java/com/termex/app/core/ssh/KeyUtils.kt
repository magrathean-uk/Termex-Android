package com.termex.app.core.ssh

import java.security.MessageDigest
import java.security.PublicKey
import java.util.Base64

object KeyUtils {

    /**
     * Calculate SHA256 fingerprint of a public key.
     * Returns format like "SHA256:XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
     */
    fun calculateFingerprint(publicKey: PublicKey): String {
        val encoded = publicKey.encoded
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(encoded)
        val base64 = Base64.getEncoder().withoutPadding().encodeToString(hash)
        return "SHA256:$base64"
    }

    /**
     * Calculate SHA256 fingerprint from raw public key bytes.
     */
    fun calculateFingerprint(publicKeyBytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(publicKeyBytes)
        val base64 = Base64.getEncoder().withoutPadding().encodeToString(hash)
        return "SHA256:$base64"
    }

    /**
     * Calculate SHA256 fingerprint from OpenSSH format public key string.
     * Input format: "ssh-rsa BASE64DATA comment" or "ssh-ed25519 BASE64DATA comment"
     * Returns format like "SHA256:XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"
     */
    fun calculateFingerprintFromOpenSSH(openSSHPublicKey: String): String? {
        return try {
            val parts = openSSHPublicKey.trim().split(" ")
            if (parts.size < 2) return null

            val keyData = Base64.getDecoder().decode(parts[1])
            calculateFingerprint(keyData)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extract key type from OpenSSH format public key string.
     * Input format: "ssh-rsa BASE64DATA comment" or "ssh-ed25519 BASE64DATA comment"
     */
    fun getKeyTypeFromOpenSSH(openSSHPublicKey: String): String {
        val parts = openSSHPublicKey.trim().split(" ")
        if (parts.isEmpty()) return "UNKNOWN"

        return when {
            parts[0].contains("rsa", ignoreCase = true) -> "RSA"
            parts[0].contains("ed25519", ignoreCase = true) -> "ED25519"
            parts[0].contains("ecdsa", ignoreCase = true) -> "ECDSA"
            parts[0].contains("dsa", ignoreCase = true) -> "DSA"
            else -> parts[0].removePrefix("ssh-").uppercase()
        }
    }

    /**
     * Calculate MD5 fingerprint (legacy format) of a public key.
     * Returns format like "MD5:xx:xx:xx:xx:..."
     */
    fun calculateMD5Fingerprint(publicKey: PublicKey): String {
        val encoded = publicKey.encoded
        val digest = MessageDigest.getInstance("MD5")
        val hash = digest.digest(encoded)
        val hex = hash.joinToString(":") { "%02x".format(it) }
        return "MD5:$hex"
    }

    /**
     * Encode public key to Base64 string for storage.
     */
    fun encodePublicKey(publicKey: PublicKey): String {
        return Base64.getEncoder().encodeToString(publicKey.encoded)
    }

    /**
     * Get a human-readable key type string.
     */
    fun getKeyTypeString(algorithm: String): String {
        return when {
            algorithm.contains("RSA", ignoreCase = true) -> "RSA"
            algorithm.contains("EC", ignoreCase = true) -> "ECDSA"
            algorithm.contains("ED25519", ignoreCase = true) -> "ED25519"
            algorithm.contains("DSA", ignoreCase = true) -> "DSA"
            else -> algorithm
        }
    }
}
