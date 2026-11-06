package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecurityStorage(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val TAG = "SecurityStorage"
        private const val PREFS_NAME = "expense_tracker_security_prefs"

        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_FIRST_LAUNCH_COMPLETED = "first_launch_completed"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_KEYSTORE_IV = "pin_keystore_iv"
        private const val KEY_USES_KEYSTORE = "pin_uses_keystore"

        private const val ANDROID_KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "ExpenseTrackerAuthKey_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
    }

    private fun getOrCreateSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER)
            keyStore.load(null)

            if (keyStore.containsAlias(KEY_ALIAS)) {
                val keyEntry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                keyEntry?.secretKey
            } else {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE_PROVIDER
                )
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .build()

                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Android Keystore unavailable or failed to initialize, using salted hash fallback", e)
            null
        }
    }

    /**
     * Securely saves the 4-digit PIN.
     * Generates a cryptographic salt, hashes the PIN using SHA-256,
     * encrypts the hash using Android Keystore AES-GCM (if available),
     * and synchronously commits the authentication metadata to persistent disk storage.
     * Raw plaintext PIN is NEVER stored.
     */
    fun savePin(pin: String): Boolean {
        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            Log.e(TAG, "Invalid PIN format provided")
            return false
        }

        val salt = SecurityHelper.generateSalt()
        val rawHash = SecurityHelper.hashPin(pin, salt)

        var encryptedHashBase64: String = rawHash
        var ivBase64: String? = null
        var usesKeystore = false

        val secretKey = getOrCreateSecretKey()
        if (secretKey != null) {
            try {
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val iv = cipher.iv
                val encryptedBytes = cipher.doFinal(rawHash.toByteArray(Charsets.UTF_8))
                encryptedHashBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
                ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
                usesKeystore = true
            } catch (e: Exception) {
                Log.w(TAG, "Keystore encryption failed, falling back to direct salted hash", e)
                usesKeystore = false
                encryptedHashBase64 = rawHash
                ivBase64 = null
            }
        }

        // Synchronous write using commit() to guarantee data is flushed to persistent flash storage
        return prefs.edit().apply {
            putString(KEY_PIN_SALT, salt)
            putString(KEY_PIN_HASH, encryptedHashBase64)
            if (ivBase64 != null) {
                putString(KEY_KEYSTORE_IV, ivBase64)
            } else {
                remove(KEY_KEYSTORE_IV)
            }
            putBoolean(KEY_USES_KEYSTORE, usesKeystore)
            putBoolean(KEY_APP_LOCK_ENABLED, true)
        }.commit()
    }

    /**
     * Verifies the user-entered PIN against persistent storage.
     * Returns true if valid, false otherwise.
     */
    fun verifyPin(enteredPin: String): Boolean {
        if (enteredPin.length != 4) return false

        val salt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val usesKeystore = prefs.getBoolean(KEY_USES_KEYSTORE, false)
        val ivBase64 = prefs.getString(KEY_KEYSTORE_IV, null)

        val candidateHash = SecurityHelper.hashPin(enteredPin, salt)

        if (usesKeystore && ivBase64 != null) {
            val secretKey = getOrCreateSecretKey()
            if (secretKey != null) {
                try {
                    val cipher = Cipher.getInstance(TRANSFORMATION)
                    val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
                    val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

                    val encryptedBytes = Base64.decode(storedHash, Base64.NO_WRAP)
                    val decryptedBytes = cipher.doFinal(encryptedBytes)
                    val decryptedHash = String(decryptedBytes, Charsets.UTF_8)

                    return MessageDigest.isEqual(
                        candidateHash.toByteArray(Charsets.UTF_8),
                        decryptedHash.toByteArray(Charsets.UTF_8)
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error decrypting hash with Keystore", e)
                    // If decryption fails, check direct comparison as fallback
                }
            }
        }

        // Direct comparison fallback for non-keystore or legacy hashes
        return MessageDigest.isEqual(
            candidateHash.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )
    }

    fun isAppLockEnabled(): Boolean {
        val hasCredentials = !prefs.getString(KEY_PIN_SALT, null).isNullOrEmpty() &&
                !prefs.getString(KEY_PIN_HASH, null).isNullOrEmpty()
        return prefs.getBoolean(KEY_APP_LOCK_ENABLED, false) && hasCredentials
    }

    fun setAppLockEnabled(enabled: Boolean): Boolean {
        return prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).commit()
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean): Boolean {
        return prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).commit()
    }

    fun isFirstLaunchCompleted(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH_COMPLETED, false)
    }

    fun setFirstLaunchCompleted(completed: Boolean): Boolean {
        return prefs.edit().putBoolean(KEY_FIRST_LAUNCH_COMPLETED, completed).commit()
    }

    fun getPinSalt(): String? = prefs.getString(KEY_PIN_SALT, null)
    fun getPinHash(): String? = prefs.getString(KEY_PIN_HASH, null)

    fun importLegacyCredentials(salt: String, hash: String, enabled: Boolean) {
        if (prefs.getString(KEY_PIN_SALT, null) == null && salt.isNotEmpty() && hash.isNotEmpty()) {
            prefs.edit()
                .putString(KEY_PIN_SALT, salt)
                .putString(KEY_PIN_HASH, hash)
                .putBoolean(KEY_APP_LOCK_ENABLED, enabled)
                .putBoolean(KEY_USES_KEYSTORE, false)
                .commit()
        }
    }

    fun clearSecurityData(): Boolean {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE_PROVIDER)
            keyStore.load(null)
            if (keyStore.containsAlias(KEY_ALIAS)) {
                keyStore.deleteEntry(KEY_ALIAS)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing Keystore alias", e)
        }
        return prefs.edit().clear().commit()
    }
}
