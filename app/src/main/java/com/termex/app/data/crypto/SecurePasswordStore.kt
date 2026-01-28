package com.termex.app.data.crypto

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class ResolvedPassword(
    val keyId: String?,
    val password: String?
)

@Singleton
class SecurePasswordStore @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private const val PREFS_NAME = "termex_secrets"
        private const val KEY_PREFIX = "pwd_"
    }

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun resolvePassword(serverId: String, storedKeyOrPassword: String?): ResolvedPassword {
        if (storedKeyOrPassword.isNullOrBlank()) {
            return ResolvedPassword(null, null)
        }

        if (prefs.contains(storedKeyOrPassword)) {
            return ResolvedPassword(
                storedKeyOrPassword,
                prefs.getString(storedKeyOrPassword, null)
            )
        }

        if (storedKeyOrPassword.startsWith(KEY_PREFIX)) {
            return ResolvedPassword(storedKeyOrPassword, null)
        }

        // Legacy plaintext migration
        val keyId = keyIdForServer(serverId)
        prefs.edit().putString(keyId, storedKeyOrPassword).apply()
        return ResolvedPassword(keyId, storedKeyOrPassword)
    }

    fun getPassword(keyId: String?): String? {
        if (keyId.isNullOrBlank()) return null
        return prefs.getString(keyId, null)
    }

    fun savePasswordForServer(serverId: String, password: String): String {
        val keyId = keyIdForServer(serverId)
        prefs.edit().putString(keyId, password).apply()
        return keyId
    }

    fun deletePassword(keyId: String?) {
        if (keyId.isNullOrBlank()) return
        prefs.edit().remove(keyId).apply()
    }

    private fun keyIdForServer(serverId: String): String = "$KEY_PREFIX$serverId"
}
