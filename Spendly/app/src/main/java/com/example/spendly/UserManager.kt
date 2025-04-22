package com.example.spendly

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class UserManager(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "spendly_user_prefs", Context.MODE_PRIVATE
    )

    // Encryption constants
    private val ANDROID_KEYSTORE = "AndroidKeyStore"
    private val KEY_ALIAS = "SpendlyEncryptionKey"
    private val TRANSFORMATION = "AES/GCM/NoPadding"
    private val IV_LENGTH = 12
    private val TAG_LENGTH = 128

    init {
        // Generate encryption key if it doesn't exist
        getOrCreateSecretKey()
    }

    /**
     * Check if user exists by email
     */
    fun isEmailTaken(email: String): Boolean {
        return getUserPasswordByEmail(email) != null
    }

    /**
     * Registers a new user
     */
    fun registerUser(name: String, email: String, password: String): Boolean {
        try {
            // Don't allow duplicate emails
            if (isEmailTaken(email)) {
                return false
            }

            // Hash password
            val encryptedPassword = encryptData(password)

            // Store in SharedPreferences
            sharedPreferences.edit().apply {
                putString("user_name_$email", name)
                putString("user_password_$email", encryptedPassword)
                apply()
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Verifies login credentials
     */
    fun verifyCredentials(email: String, password: String): Boolean {
        val storedEncryptedPassword = getUserPasswordByEmail(email) ?: return false

        try {
            // Decrypt and compare passwords
            val decryptedPassword = decryptData(storedEncryptedPassword)
            return decryptedPassword == password
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Gets user password by email
     */
    private fun getUserPasswordByEmail(email: String): String? {
        return sharedPreferences.getString("user_password_$email", null)
    }

    /**
     * Saves whether the user is logged in
     */
    fun setLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit().putBoolean("is_logged_in", isLoggedIn).apply()
    }

    /**
     * Gets the logged in status
     */
    fun isLoggedIn(): Boolean {
        val isLoggedInFlag = sharedPreferences.getBoolean("is_logged_in", false)
        val hasEmail = !getCurrentUserEmail().isNullOrEmpty()
        return isLoggedInFlag && hasEmail
    }

    /**
     * Saves user email for session
     */
    fun saveUserEmail(email: String) {
        sharedPreferences.edit().putString("current_user_email", email).apply()
    }

    /**
     * Gets the current logged in user's email
     */
    fun getCurrentUserEmail(): String? {
        return sharedPreferences.getString("current_user_email", null)
    }

    /**
     * Gets the current logged in user's name
     */
    fun getCurrentUserName(): String? {
        val email = getCurrentUserEmail() ?: return null
        return sharedPreferences.getString("user_name_$email", null)
    }

    /**
     * Logs out the current user
     */
    fun logout() {
        sharedPreferences.edit().apply {
            remove("is_logged_in")
            remove("current_user_email")
            apply()
        }
    }

    /**
     * Encrypts data using Android KeyStore
     */
    private fun encryptData(data: String): String {
        try {
            val key = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)

            // Get IV and encrypt
            val iv = cipher.iv
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

            // Combine IV and encrypted data
            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)

            return Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            return data // Fallback if encryption fails
        }
    }

    /**
     * Decrypts data using Android KeyStore
     */
    private fun decryptData(encryptedData: String): String {
        try {
            val key = getOrCreateSecretKey()
            val combined = Base64.decode(encryptedData, Base64.DEFAULT)

            // Extract IV
            val iv = ByteArray(IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH)

            // Extract encrypted data
            val encrypted = ByteArray(combined.size - IV_LENGTH)
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.size)

            // Decrypt
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val decrypted = cipher.doFinal(encrypted)
            return String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return encryptedData // Fallback if decryption fails
        }
    }

    /**
     * Gets or creates encryption key in KeyStore
     */
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
            )

            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )

            return keyGenerator.generateKey()
        }

        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }
}