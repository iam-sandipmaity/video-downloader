package com.localdownloader.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal object PinHasher {
    private const val PBKDF2_PREFIX = "pbkdf2"
    private const val DEFAULT_ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val KEY_LENGTH_BITS = 256

    fun hash(pin: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val digest = pbkdf2(pin, salt, DEFAULT_ITERATIONS)
        return listOf(
            PBKDF2_PREFIX,
            DEFAULT_ITERATIONS.toString(),
            salt.toHex(),
            digest.toHex(),
        ).joinToString("$")
    }

    fun verify(pin: String, stored: String): Boolean {
        if (stored.isBlank()) return false
        if (stored.startsWith("$PBKDF2_PREFIX$")) {
            val parts = stored.split("$")
            if (parts.size != 4) return false
            val iterations = parts[1].toIntOrNull() ?: return false
            val salt = parts[2].fromHex() ?: return false
            val expected = parts[3].fromHex() ?: return false
            val actual = pbkdf2(pin, salt, iterations)
            return MessageDigest.isEqual(expected, actual)
        }
        return MessageDigest.isEqual(legacySha256(pin), stored.toByteArray(Charsets.UTF_8))
    }

    fun needsUpgrade(stored: String): Boolean {
        return stored.isNotBlank() && !stored.startsWith("$PBKDF2_PREFIX$")
    }

    private fun pbkdf2(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec)
            .encoded
    }

    private fun legacySha256(pin: String): ByteArray {
        return MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .toByteArray(Charsets.UTF_8)
    }

    private fun ByteArray.toHex(): String {
        return joinToString("") { "%02x".format(it) }
    }

    private fun String.fromHex(): ByteArray? {
        if (length % 2 != 0) return null
        return runCatching {
            ByteArray(length / 2) { index ->
                substring(index * 2, index * 2 + 2).toInt(16).toByte()
            }
        }.getOrNull()
    }
}
