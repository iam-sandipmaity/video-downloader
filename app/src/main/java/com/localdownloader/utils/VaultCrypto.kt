package com.localdownloader.utils

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object VaultCrypto {
    private const val KEY_ALIAS = "VideoDownloaderVaultKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128

    @Synchronized
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val key = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (key != null) return key

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encryptFile(source: File, destination: File) {
        val secretKey = getOrCreateKey()
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv

        FileOutputStream(destination).use { fos ->
            fos.write(iv)
            CipherOutputStream(fos, cipher).use { cos ->
                FileInputStream(source).use { fis ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (fis.read(buffer).also { read = it } != -1) {
                        cos.write(buffer, 0, read)
                    }
                }
            }
        }
    }

    fun decryptFile(source: File, destination: File) {
        val secretKey = getOrCreateKey()
        FileInputStream(source).use { fis ->
            val iv = ByteArray(IV_SIZE)
            if (fis.read(iv) != IV_SIZE) {
                throw IllegalArgumentException("Invalid encrypted file: IV missing")
            }

            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE, iv))

            CipherInputStream(fis, cipher).use { cis ->
                FileOutputStream(destination).use { fos ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (cis.read(buffer).also { read = it } != -1) {
                        fos.write(buffer, 0, read)
                    }
                }
            }
        }
    }
}
