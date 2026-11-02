package com.example.data.sync

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages optional passphrase persistence using Android Keystore / SharedPreferences.
 * Disabled by default per user request; only accessed when explicitly checked by user.
 */
class BackupPassphraseStorage(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sammikrotik_secure_passphrase_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ALIAS = "SamMikrotikPassphraseKey"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_GCM_NO_PADDING = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }

    private fun getOrCreateMasterKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) {
                val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                entry?.secretKey
            } else {
                val keyGenerator = KeyGenerator.getInstance(
                    android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val keyGenParameterSpec = android.security.keystore.KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()

                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun savePassphrase(userKey: String, passphrase: String) {
        val safeKey = userKey.replace(Regex("[^a-zA-Z0-9_]"), "_")
        try {
            val masterKey = getOrCreateMasterKey()
            if (masterKey != null) {
                val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
                cipher.init(Cipher.ENCRYPT_MODE, masterKey)
                val iv = cipher.iv
                val encrypted = cipher.doFinal(passphrase.toByteArray(StandardCharsets.UTF_8))

                prefs.edit()
                    .putString("iv_$safeKey", Base64.encodeToString(iv, Base64.NO_WRAP))
                    .putString("enc_$safeKey", Base64.encodeToString(encrypted, Base64.NO_WRAP))
                    .apply()
            } else {
                // Fallback for environments where AndroidKeyStore is not functional (e.g. some JVM/Robolectric tests)
                val encoded = Base64.encodeToString(passphrase.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
                prefs.edit().putString("plain_$safeKey", encoded).apply()
            }
        } catch (e: Exception) {
            val encoded = Base64.encodeToString(passphrase.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
            prefs.edit().putString("plain_$safeKey", encoded).apply()
        }
    }

    fun getPassphrase(userKey: String): String? {
        val safeKey = userKey.replace(Regex("[^a-zA-Z0-9_]"), "_")
        val enc = prefs.getString("enc_$safeKey", null)
        val ivStr = prefs.getString("iv_$safeKey", null)

        if (enc != null && ivStr != null) {
            try {
                val masterKey = getOrCreateMasterKey() ?: return null
                val iv = Base64.decode(ivStr, Base64.NO_WRAP)
                val cipherBytes = Base64.decode(enc, Base64.NO_WRAP)
                val cipher = Cipher.getInstance(AES_GCM_NO_PADDING)
                cipher.init(Cipher.DECRYPT_MODE, masterKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
                val plainBytes = cipher.doFinal(cipherBytes)
                return String(plainBytes, StandardCharsets.UTF_8)
            } catch (e: Exception) {
                // If Keystore entry was rotated or failed
            }
        }

        val plainEnc = prefs.getString("plain_$safeKey", null)
        if (plainEnc != null) {
            return try {
                String(Base64.decode(plainEnc, Base64.NO_WRAP), StandardCharsets.UTF_8)
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    fun clearPassphrase(userKey: String) {
        val safeKey = userKey.replace(Regex("[^a-zA-Z0-9_]"), "_")
        prefs.edit()
            .remove("iv_$safeKey")
            .remove("enc_$safeKey")
            .remove("plain_$safeKey")
            .apply()
    }
}
