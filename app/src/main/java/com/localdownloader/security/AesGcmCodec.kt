package com.localdownloader.security

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal object AesGcmCodec {
    val MAGIC: ByteArray = "VDENC1".toByteArray(Charsets.US_ASCII)
    const val IV_BYTES: Int = 12
    const val TAG_BITS: Int = 128
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun hasMagic(payload: ByteArray): Boolean {
        if (payload.size < MAGIC.size) return false
        return payload.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)
    }

    fun isEncryptedFile(file: File): Boolean {
        if (!file.exists() || !file.isFile || file.length() < MAGIC.size + IV_BYTES + 16L) {
            return false
        }
        return runCatching {
            file.inputStream().use { input ->
                val header = ByteArray(MAGIC.size)
                val read = input.read(header)
                read == MAGIC.size && header.contentEquals(MAGIC)
            }
        }.getOrDefault(false)
    }

    fun seal(key: SecretKey, plaintext: ByteArray): ByteArray {
        val cipher = initEncryptCipher(key)
        val iv = requireIv(cipher)
        val ciphertext = cipher.doFinal(plaintext)
        return MAGIC + iv + ciphertext
    }

    fun open(key: SecretKey, payload: ByteArray): ByteArray {
        require(hasMagic(payload)) { "Payload is not an encrypted local-downloader blob" }
        require(payload.size > MAGIC.size + IV_BYTES) { "Encrypted payload is truncated" }
        val iv = payload.copyOfRange(MAGIC.size, MAGIC.size + IV_BYTES)
        val ciphertext = payload.copyOfRange(MAGIC.size + IV_BYTES, payload.size)
        val cipher = newCipher()
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    fun encryptStream(key: SecretKey, input: InputStream, output: OutputStream) {
        val cipher = initEncryptCipher(key)
        output.write(MAGIC)
        output.write(requireIv(cipher))
        copyThroughCipher(cipher, input, output)
    }

    fun decryptStream(key: SecretKey, input: InputStream, output: OutputStream) {
        val header = ByteArray(MAGIC.size)
        check(input.read(header) == MAGIC.size && header.contentEquals(MAGIC)) {
            "File is not an encrypted vault artifact"
        }
        val iv = ByteArray(IV_BYTES)
        check(input.read(iv) == IV_BYTES) { "Encrypted vault artifact is truncated" }
        val cipher = newCipher()
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        copyThroughCipher(cipher, input, output)
    }

    fun encryptFile(key: SecretKey, source: File, target: File) {
        target.parentFile?.mkdirs()
        source.inputStream().use { input ->
            target.outputStream().use { output ->
                encryptStream(key, input, output)
            }
        }
    }

    fun decryptFile(key: SecretKey, source: File, target: File) {
        target.parentFile?.mkdirs()
        source.inputStream().use { input ->
            target.outputStream().use { output ->
                decryptStream(key, input, output)
            }
        }
    }

    private fun copyThroughCipher(cipher: Cipher, input: InputStream, output: OutputStream) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            val encoded = cipher.update(buffer, 0, read) ?: continue
            output.write(encoded)
        }
        output.write(cipher.doFinal())
    }

    private fun initEncryptCipher(key: SecretKey): Cipher {
        val cipher = newCipher()
        // Android Keystore forbids caller-provided IVs when randomized encryption is required.
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher
    }

    private fun requireIv(cipher: Cipher): ByteArray {
        val iv = cipher.iv
        check(iv != null && iv.size == IV_BYTES) {
            "Unexpected GCM IV length: ${iv?.size ?: 0}"
        }
        return iv
    }

    private fun newCipher(): Cipher {
        return Cipher.getInstance(TRANSFORMATION)
    }
}
