package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class SecureApiKey(
    val id: String,
    val type: String, // "GEMINI" or "LOCAL"
    val label: String,
    val maskedValue: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

class SecureKeyStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("yslam_secure_keys_store", Context.MODE_PRIVATE)

    private val keyStoreAlias = "YslamMasterKeyAlias"
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    init {
        ensureMasterKey()
    }

    private fun ensureMasterKey() {
        try {
            if (!keyStore.containsAlias(keyStoreAlias)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    "AndroidKeyStore"
                )
                val spec = KeyGenParameterSpec.Builder(
                    keyStoreAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            Log.e("SecureKeyStorage", "Failed to generate master key", e)
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            (keyStore.getEntry(keyStoreAlias, null) as? KeyStore.SecretKeyEntry)?.secretKey
        } catch (e: Exception) {
            Log.e("SecureKeyStorage", "Failed to get secret key", e)
            null
        }
    }

    private fun encrypt(plainText: String): String {
        return try {
            val key = getSecretKey() ?: return Base64.encodeToString(plainText.toByteArray(), Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e("SecureKeyStorage", "Encryption error", e)
            Base64.encodeToString(plainText.toByteArray(), Base64.NO_WRAP)
        }
    }

    private fun decrypt(encryptedBase64: String): String {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val key = getSecretKey() ?: return String(combined, Charsets.UTF_8)
            val iv = ByteArray(12)
            System.arraycopy(combined, 0, iv, 0, 12)
            val encryptedBytes = ByteArray(combined.size - 12)
            System.arraycopy(combined, 12, encryptedBytes, 0, encryptedBytes.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e("SecureKeyStorage", "Decryption error, falling back", e)
            try {
                String(Base64.decode(encryptedBase64, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (_: Exception) {
                ""
            }
        }
    }

    fun saveKey(id: String, type: String, label: String, secretKey: String) {
        val encrypted = encrypt(secretKey.trim())
        val keysList = getKeys().toMutableList()

        // Remove existing if same ID
        keysList.removeAll { it.id == id }

        val masked = maskSecret(secretKey.trim())
        keysList.add(
            SecureApiKey(
                id = id,
                type = type,
                label = label.ifBlank { "$type Key" },
                maskedValue = masked,
                createdAt = System.currentTimeMillis(),
                isActive = true
            )
        )

        // Store encrypted payload in private prefs
        prefs.edit().putString("raw_$id", encrypted).apply()
        saveKeysMetadata(keysList)
    }

    fun getKeys(): List<SecureApiKey> {
        val jsonString = prefs.getString("keys_metadata", "[]") ?: "[]"
        val result = mutableListOf<SecureApiKey>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    SecureApiKey(
                        id = obj.getString("id"),
                        type = obj.getString("type"),
                        label = obj.optString("label", "Key"),
                        maskedValue = obj.optString("maskedValue", "****"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        isActive = obj.optBoolean("isActive", true)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("SecureKeyStorage", "Failed to parse keys metadata", e)
        }
        return result
    }

    fun getPlaintextKey(id: String): String? {
        val encrypted = prefs.getString("raw_$id", null) ?: return null
        return decrypt(encrypted)
    }

    fun deleteKey(id: String) {
        val keysList = getKeys().toMutableList()
        keysList.removeAll { it.id == id }
        prefs.edit()
            .remove("raw_$id")
            .apply()
        saveKeysMetadata(keysList)
    }

    fun toggleKeyActive(id: String) {
        val keysList = getKeys().map {
            if (it.id == id) it.copy(isActive = !it.isActive) else it
        }
        saveKeysMetadata(keysList)
    }

    fun getActiveKeyForType(type: String): String? {
        val activeMeta = getKeys().firstOrNull { it.type == type && it.isActive } ?: return null
        return getPlaintextKey(activeMeta.id)
    }

    private fun saveKeysMetadata(list: List<SecureApiKey>) {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("type", item.type)
                put("label", item.label)
                put("maskedValue", item.maskedValue)
                put("createdAt", item.createdAt)
                put("isActive", item.isActive)
            }
            array.put(obj)
        }
        prefs.edit().putString("keys_metadata", array.toString()).apply()
    }

    private fun maskSecret(secret: String): String {
        return if (secret.length <= 8) {
            "****"
        } else {
            "${secret.take(4)}...${secret.takeLast(4)}"
        }
    }
}
