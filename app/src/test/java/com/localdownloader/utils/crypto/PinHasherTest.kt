package com.localdownloader.utils.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class PinHasherTest {

    private val pinHasher = PinHasher()

    @Test
    fun hashPin_producesValidPbkdf2FormatWithSalt() {
        val pin = "1234"
        val hash1 = pinHasher.hashPin(pin)
        val hash2 = pinHasher.hashPin(pin)

        assertTrue(hash1.startsWith("pbkdf2:100000:"))
        assertTrue(hash2.startsWith("pbkdf2:100000:"))
        // Different random salts ensure different hashes
        assertNotEquals(hash1, hash2)
    }

    @Test
    fun verifyPin_correctPinReturnsValidWithoutUpgrade() {
        val pin = "5678"
        val hash = pinHasher.hashPin(pin)

        val result = pinHasher.verifyPin(pin, hash)
        assertEquals(PinVerificationResult.Valid(needsUpgrade = false), result)
    }

    @Test
    fun verifyPin_wrongPinReturnsInvalid() {
        val hash = pinHasher.hashPin("1234")
        val result = pinHasher.verifyPin("9999", hash)

        assertEquals(PinVerificationResult.Invalid, result)
    }

    @Test
    fun verifyPin_legacySha256ReturnsValidWithNeedsUpgrade() {
        val pin = "4321"
        val legacySha256 = MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        val result = pinHasher.verifyPin(pin, legacySha256)
        assertEquals(PinVerificationResult.Valid(needsUpgrade = true), result)

        val wrongResult = pinHasher.verifyPin("0000", legacySha256)
        assertEquals(PinVerificationResult.Invalid, wrongResult)
    }

    @Test
    fun verifyPin_malformedOrBlankHashReturnsInvalid() {
        assertEquals(PinVerificationResult.Invalid, pinHasher.verifyPin("1234", ""))
        assertEquals(PinVerificationResult.Invalid, pinHasher.verifyPin("1234", "   "))
        assertEquals(PinVerificationResult.Invalid, pinHasher.verifyPin("1234", "not-a-valid-hash"))
        assertEquals(PinVerificationResult.Invalid, pinHasher.verifyPin("1234", "pbkdf2:invalid:salt:hash"))
    }
}
