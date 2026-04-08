package com.termex.app.data.crypto

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

data class ResolvedPassword(
    val keyId: String?,
    val password: String?
)

@Singleton
class SecurePasswordStore @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private const val PREFS_NAME = "termex_secrets_v2"
        private const val LEGACY_PREFS_NAME = "termex_secrets"
        private const val KEY_PREFIX = "pwd_"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEYSTORE_ALIAS = "termex_password_store_key"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_BITS = 128
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Suppress("DEPRECATION")
    private val legacyPrefs by lazy {
        runCatching {
            EncryptedSharedPreferences.create(
                context,
                LEGACY_PREFS_NAME,
                MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }.getOrNull()
    }

    fun resolvePassword(serverId: String, storedKeyOrPassword: String?): ResolvedPassword {
        if (storedKeyOrPassword.isNullOrBlank()) {
            return ResolvedPassword(null, null)
        }

        readPassword(storedKeyOrPassword)?.let { password ->
            return ResolvedPassword(storedKeyOrPassword, password)
        }

        if (storedKeyOrPassword.startsWith(KEY_PREFIX)) {
            return ResolvedPassword(storedKeyOrPassword, null)
        }

        // Legacy plaintext migration from older database rows.
        val keyId = keyIdForServer(serverId)
        saveEncrypted(keyId, storedKeyOrPassword)
        return ResolvedPassword(keyId, storedKeyOrPassword)
    }

    fun getPassword(keyId: String?): String? {
        if (keyId.isNullOrBlank()) return null
        return readPassword(keyId)
    }

    fun savePasswordForServer(serverId: String, password: String): String {
        val keyId = keyIdForServer(serverId)
        saveEncrypted(keyId, password)
        return keyId
    }

    fun deletePassword(keyId: String?) {
        if (keyId.isNullOrBlank()) return
        prefs.edit().remove(keyId).apply()
        runCatching {
            legacyPrefs?.edit()?.remove(keyId)?.apply()
        }
    }

    private fun readPassword(keyId: String): String? {
        val encrypted = prefs.getString(keyId, null)
        if (!encrypted.isNullOrBlank()) {
            return decrypt(encrypted)
        }
        return migrateLegacyPassword(keyId)
    }

    private fun migrateLegacyPassword(keyId: String): String? {
        val legacyValue = runCatching {
            legacyPrefs?.getString(keyId, null)
        }.getOrNull() ?: return null

        saveEncrypted(keyId, legacyValue)
        runCatching {
            legacyPrefs?.edit()?.remove(keyId)?.apply()
        }
        return legacyValue
    }

    private fun saveEncrypted(keyId: String, password: String) {
        val encrypted = encrypt(password)
        prefs.edit().putString(keyId, encrypted).apply()
    }

    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encrypted = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
        val iv = Base64.getEncoder().encodeToString(cipher.iv)
        val payload = Base64.getEncoder().encodeToString(encrypted)
        return "$iv:$payload"
    }

    private fun decrypt(ciphertext: String): String? {
        return runCatching {
            val parts = ciphertext.split(':', limit = 2)
            require(parts.size == 2)
            val iv = Base64.getDecoder().decode(parts[0])
            val payload = Base64.getDecoder().decode(parts[1])
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(GCM_TAG_BITS, iv)
            )
            String(cipher.doFinal(payload), StandardCharsets.UTF_8)
        }.getOrNull()
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun keyIdForServer(serverId: String): String = "$KEY_PREFIX$serverId"
}
