package com.localdownloader.security

import java.nio.file.Files
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AesGcmCodecTest {

    @Test
    fun sealAndOpen_roundTripsUtf8Payload() {
        val key = testKey()
        val plaintext = "cookies=secret; po_token=abc".toByteArray()

        val sealed = AesGcmCodec.seal(key, plaintext)

        assertTrue(AesGcmCodec.hasMagic(sealed))
        assertFalse(AesGcmCodec.hasMagic(plaintext))
        assertEquals("cookies=secret; po_token=abc", AesGcmCodec.open(key, sealed).decodeToString())
    }

    @Test
    fun encryptFile_roundTripsAndMarksFileEncrypted() {
        val key = testKey()
        val source = Files.createTempFile("vault-src", ".bin").toFile().apply {
            writeBytes(ByteArray(4096) { index -> (index % 251).toByte() })
            deleteOnExit()
        }
        val encrypted = Files.createTempFile("vault-enc", ".bin").toFile().apply { deleteOnExit() }
        val restored = Files.createTempFile("vault-out", ".bin").toFile().apply { deleteOnExit() }

        AesGcmCodec.encryptFile(key, source, encrypted)

        assertTrue(AesGcmCodec.isEncryptedFile(encrypted))
        assertFalse(AesGcmCodec.isEncryptedFile(source))
        AesGcmCodec.decryptFile(key, encrypted, restored)
        assertEquals(source.readBytes().toList(), restored.readBytes().toList())
    }

    private fun testKey(): SecretKey {
        return SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")
    }
}
