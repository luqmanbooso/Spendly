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

    private val ANDROID_KEYSTORE = "AndroidKeyStore"
    private val KEY_ALIAS = "SpendlyEncryptionKey"
    private val TRANSFORMATION = "AES/GCM/NoPadding"
    private val IV_LENGTH = 12
    private val TAG_LENGTH = 128

    init {
        getOrCreateSecretKey()
    }

    fun isEmailTaken(email: String): Boolean {
        return getUserPasswordByEmail(email) != null
    }

    fun registerUser(name: String, email: String, password: String): Boolean {
        try {
            if (isEmailTaken(email)) {
                return false
            }

            val encryptedPassword = encryptData(password)

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

    fun verifyCredentials(email: String, password: String): Boolean {
        val storedEncryptedPassword = getUserPasswordByEmail(email) ?: return false

        try {
            val decryptedPassword = decryptData(storedEncryptedPassword)
            return decryptedPassword == password
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun getUserPasswordByEmail(email: String): String? {
        return sharedPreferences.getString("user_password_$email", null)
    }

    fun setLoggedIn(isLoggedIn: Boolean) {
        sharedPreferences.edit().putBoolean("is_logged_in", isLoggedIn).apply()
    }

    fun isLoggedIn(): Boolean {
        val isLoggedInFlag = sharedPreferences.getBoolean("is_logged_in", false)
        val hasEmail = !getCurrentUserEmail().isNullOrEmpty()
        return isLoggedInFlag && hasEmail
    }

    fun saveUserEmail(email: String) {
        sharedPreferences.edit().putString("current_user_email", email).apply()
    }

    fun getCurrentUserEmail(): String? {
        return sharedPreferences.getString("current_user_email", null)
    }

    fun getCurrentUserName(): String? {
        val email = getCurrentUserEmail() ?: return null
        return sharedPreferences.getString("user_name_$email", null)
    }

    fun saveUserName(name: String) {
        val email = getCurrentUserEmail() ?: return
        sharedPreferences.edit().putString("user_name_$email", name).apply()
    }

    fun logout() {
        sharedPreferences.edit().apply {
            remove("is_logged_in")
            remove("current_user_email")
            apply()
        }
    }

    private fun encryptData(data: String): String {
        try {
            val key = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)

            val iv = cipher.iv
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))

            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)

            return Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            return data
        }
    }

    private fun decryptData(encryptedData: String): String {
        try {
            val key = getOrCreateSecretKey()
            val combined = Base64.decode(encryptedData, Base64.DEFAULT)

            val iv = ByteArray(IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH)

            val encrypted = ByteArray(combined.size - IV_LENGTH)
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val decrypted = cipher.doFinal(encrypted)
            return String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            return encryptedData
        }
    }

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