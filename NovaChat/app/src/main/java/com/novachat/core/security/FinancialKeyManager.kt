package com.novachat.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

private val Context.financialKeyStore by preferencesDataStore(name = "financial_key_prefs")

@Singleton
class FinancialKeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val KEYSTORE_ALIAS = "novachat_financial_db_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private val ENCRYPTED_PASSPHRASE_KEY = stringPreferencesKey("encrypted_passphrase")
        private val IV_KEY = stringPreferencesKey("passphrase_iv")
    }

    fun getPassphrase(): ByteArray = runBlocking {
        val prefs = context.financialKeyStore.data.first()
        val encryptedB64 = prefs[ENCRYPTED_PASSPHRASE_KEY]
        val ivB64 = prefs[IV_KEY]

        if (encryptedB64 != null && ivB64 != null) {
            decryptPassphrase(
                Base64.getDecoder().decode(encryptedB64),
                Base64.getDecoder().decode(ivB64)
            )
        } else {
            generateAndStorePassphrase()
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getEntry(KEYSTORE_ALIAS, null)?.let {
            return (it as KeyStore.SecretKeyEntry).secretKey
        }
        val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGen.init(
            KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGen.generateKey()
    }

    private suspend fun generateAndStorePassphrase(): ByteArray {
        val passphrase = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encrypted = cipher.doFinal(passphrase)
        val iv = cipher.iv

        context.financialKeyStore.edit { prefs ->
            prefs[ENCRYPTED_PASSPHRASE_KEY] = Base64.getEncoder().encodeToString(encrypted)
            prefs[IV_KEY] = Base64.getEncoder().encodeToString(iv)
        }
        return passphrase
    }

    private fun decryptPassphrase(encrypted: ByteArray, iv: ByteArray): ByteArray {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(encrypted)
    }
}
