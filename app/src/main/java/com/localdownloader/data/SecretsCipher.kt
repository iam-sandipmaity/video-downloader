package com.localdownloader.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypts/decrypts the download-option secrets sidecar at rest using AES-256-GCM
 * backed by the Android Keystore, so that session cookies, PO tokens, user-agent
 * headers and `youtubeDataSyncId` are never written to disk as plaintext.
 *
 * On-disk layout produced by [KeystoreSecretsCipher.encrypt]:
 *   [MAGIC][12-byte IV][ciphertext + 16-byte GCM auth tag]
 * The [SecretsCipherFormat] helpers own that layout so they can be unit-tested on
 * the JVM without an Android Keystore.
 */
internal interface SecretsCipher {
    fun encrypt(plaintext: ByteArray): ByteArray
    fun decrypt(blob: ByteArray): ByteArray?
    fun isEncryptedBlob(bytes: ByteArray): Boolean
}

internal object SecretsCipherFormat {
    /** Leading byte that marks an encrypted (v1) sidecar. */
    const val MAGIC: Byte = 0x01
    const val IV_LENGTH = 12

    fun isEncryptedBlob(bytes: ByteArray): Boolean =
        bytes.size > IV_LENGTH + 1 && bytes[0] == MAGIC

    fun formatBlob(iv: ByteArray, ciphertext: ByteArray): ByteArray =
        ByteArray(1 + IV_LENGTH + ciphertext.size).also { out ->
            out[0] = MAGIC
            System.arraycopy(iv, 0, out, 1, IV_LENGTH)
            System.arraycopy(ciphertext, 0, out, 1 + IV_LENGTH, ciphertext.size)
        }

    fun extractIvAndCiphertext(blob: ByteArray): Pair<ByteArray, ByteArray>? {
        if (!isEncryptedBlob(blob)) return null
        val iv = blob.copyOfRange(1, 1 + IV_LENGTH)
        val ciphertext = blob.copyOfRange(1 + IV_LENGTH, blob.size)
        return iv to ciphertext
    }
}

@Singleton
internal class KeystoreSecretsCipher @Inject constructor() : SecretsCipher {

    private val secretKey: SecretKey by lazy { getOrCreateKey() }

    override fun encrypt(plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext)
        return SecretsCipherFormat.formatBlob(iv, ciphertext)
    }

    override fun decrypt(blob: ByteArray): ByteArray? {
        val (iv, ciphertext) = SecretsCipherFormat.extractIvAndCiphertext(blob) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            cipher.doFinal(ciphertext)
        }.getOrNull()
    }

    override fun isEncryptedBlob(bytes: ByteArray): Boolean =
        SecretsCipherFormat.isEncryptedBlob(bytes)

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        keyStore.getKey(KEY_ALIAS, null)?.let { return it as SecretKey }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return keyGenerator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "download_option_secrets_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_LENGTH_BITS = 128
    }
}
