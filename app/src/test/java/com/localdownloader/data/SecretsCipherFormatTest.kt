package com.localdownloader.data

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecretsCipherFormatTest {

    @Test
    fun isEncryptedBlob_recognizesMagicPrefixedBlob() {
        val iv = ByteArray(SecretsCipherFormat.IV_LENGTH) { 1 }
        val ciphertext = byteArrayOf(2, 3, 4, 5)
        val blob = SecretsCipherFormat.formatBlob(iv, ciphertext)

        assertTrue(SecretsCipherFormat.isEncryptedBlob(blob))
    }

    @Test
    fun isEncryptedBlob_rejectsPlaintextJson() {
        val plaintext = """{"youtubePoToken":"abc"}""".toByteArray()
        assertFalse(SecretsCipherFormat.isEncryptedBlob(plaintext))
    }

    @Test
    fun isEncryptedBlob_rejectsTooShortInput() {
        assertFalse(SecretsCipherFormat.isEncryptedBlob(byteArrayOf(SecretsCipherFormat.MAGIC)))
        assertFalse(SecretsCipherFormat.isEncryptedBlob(ByteArray(0)))
    }

    @Test
    fun formatBlob_thenExtract_reconstructsIvAndCiphertext() {
        val iv = ByteArray(SecretsCipherFormat.IV_LENGTH) { 0x42 }
        val ciphertext = byteArrayOf(10, 20, 30, 40, 50)

        val blob = SecretsCipherFormat.formatBlob(iv, ciphertext)
        val (extractedIv, extractedCiphertext) = SecretsCipherFormat.extractIvAndCiphertext(blob)

        assertNotNull(extractedIv)
        assertContentEquals(iv, extractedIv)
        assertContentEquals(ciphertext, extractedCiphertext)
    }

    @Test
    fun extractIvAndCiphertext_returnsNullForPlaintext() {
        val plaintext = """{"extractorArgs":"x"}""".toByteArray()
        assertNull(SecretsCipherFormat.extractIvAndCiphertext(plaintext))
    }

    @Test
    fun formatBlob_leadsWithMagicByte() {
        val iv = ByteArray(SecretsCipherFormat.IV_LENGTH)
        val blob = SecretsCipherFormat.formatBlob(iv, byteArrayOf(1))
        assertEquals(SecretsCipherFormat.MAGIC, blob[0])
    }

    @Test
    fun roundTrip_throughFakeCipher_preservesPlaintext() {
        val cipher = FakeSecretsCipher()
        val plaintext = """{"youtubePoToken":"secret-token"}""".toByteArray()

        val encrypted = cipher.encrypt(plaintext)
        assertTrue(cipher.isEncryptedBlob(encrypted))
        assertFalse(encrypted.contentEquals(plaintext))

        val decrypted = cipher.decrypt(encrypted)
        assertNotNull(decrypted)
        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun decrypt_returnsNullForPlaintextInput() {
        val cipher = FakeSecretsCipher()
        val plaintext = """{"youtubeCookiesPath":"/c.txt"}""".toByteArray()
        // A plaintext payload handed to decrypt() should fail closed, not return garbage.
        assertNull(cipher.decrypt(plaintext))
    }

    private class FakeSecretsCipher : SecretsCipher {
        override fun encrypt(plaintext: ByteArray): ByteArray {
            // Invert every byte as a stand-in "cipher" so the output differs from plaintext
            // but is reversible; wrap in the real on-disk format.
            val transformed = plaintext.map { (it.toInt() xor 0xFF).toByte() }.toByteArray()
            val iv = ByteArray(SecretsCipherFormat.IV_LENGTH) { 0x7A }
            return SecretsCipherFormat.formatBlob(iv, transformed)
        }

        override fun decrypt(blob: ByteArray): ByteArray? {
            val (iv, ciphertext) = SecretsCipherFormat.extractIvAndCiphertext(blob) ?: return null
            return ciphertext.map { (it.toInt() xor 0xFF).toByte() }.toByteArray()
        }

        override fun isEncryptedBlob(bytes: ByteArray): Boolean =
            SecretsCipherFormat.isEncryptedBlob(bytes)
    }
}
