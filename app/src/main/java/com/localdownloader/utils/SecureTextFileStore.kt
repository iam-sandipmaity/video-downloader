package com.localdownloader.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.io.File
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureTextFileStore @Inject constructor() {

    fun writeText(file: File, value: String) {
        val parent = file.parentFile ?: throw IllegalStateException("Unable to resolve secure file parent.")
        if (!parent.exists() && !parent.mkdirs()) {
            throw IllegalStateException("Unable to create secure storage directory ${parent.absolutePath}")
        }
        val tempFile = File(parent, "${file.name}.tmp")
        tempFile.writeBytes(encrypt(value))
        if (!tempFile.renameTo(file)) {
            tempFile.copyTo(file, overwrite = true)
            tempFile.delete()
        }
    }

    fun readText(
        file: File,
        migrateLegacyPlaintext: Boolean = true,
    ): String? {
        if (!file.exists() || !file.isFile) return null
        val bytes = file.readBytes()
        return if (isEncryptedPayload(bytes)) {
            decrypt(bytes)
        } else {
            val plaintext = String(bytes, StandardCharsets.UTF_8)
            if (migrateLegacyPlaintext) {
                runCatching { writeText(file, plaintext) }
                    .onFailure { error ->
                        Log.w(TAG, "Failed migrating legacy plaintext file ${file.name}: ${error.message}")
                    }
            }
            plaintext
        }
    }

    private fun encrypt(value: String): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        return ByteArrayOutputStream(MAGIC.size + 2 + iv.size + ciphertext.size).use { output ->
            output.write(MAGIC)
            output.write(VERSION.toInt())
            output.write(iv.size)
            output.write(iv)
            output.write(ciphertext)
            output.toByteArray()
        }
    }

    private fun decrypt(payload: ByteArray): String {
        require(payload.size >= MIN_ENCRYPTED_SIZE) { "Encrypted payload is too short." }
        val version = payload[MAGIC.size]
        require(version == VERSION) { "Unsupported encrypted payload version." }
        val ivLength = payload[MAGIC.size + 1].toInt() and 0xFF
        require(ivLength in MIN_IV_LENGTH..MAX_IV_LENGTH) { "Invalid encrypted payload IV length." }

        val ivStart = MAGIC.size + 2
        val cipherStart = ivStart + ivLength
        require(payload.size > cipherStart) { "Encrypted payload is incomplete." }

        val iv = payload.copyOfRange(ivStart, cipherStart)
        val ciphertext = payload.copyOfRange(cipherStart, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
        )
        return String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8)
    }

    private fun isEncryptedPayload(bytes: ByteArray): Boolean {
        if (bytes.size < MIN_ENCRYPTED_SIZE) return false
        return bytes.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) return existingKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(parameterSpec)
        return keyGenerator.generateKey()
    }

    private companion object {
        private const val TAG = "SecureTextFileStore"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "localdownloader_secure_text_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val MIN_IV_LENGTH = 12
        private const val MAX_IV_LENGTH = 16
        private val MAGIC = byteArrayOf('L'.code.toByte(), 'D'.code.toByte(), 'S'.code.toByte(), '1'.code.toByte())
        private const val VERSION: Byte = 1
        private val MIN_ENCRYPTED_SIZE = MAGIC.size + 2 + MIN_IV_LENGTH + 1
    }
}
