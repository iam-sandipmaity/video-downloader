package com.localdownloader.utils.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

sealed interface PinVerificationResult {
    data class Valid(val needsUpgrade: Boolean) : PinVerificationResult
    object Invalid : PinVerificationResult
}

@Singleton
class PinHasher @Inject constructor() {

    private val secureRandom = SecureRandom()

    fun hashPin(pin: String): String {
        val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
        val derivedBytes = deriveKey(pin.toCharArray(), salt, DEFAULT_ITERATIONS, KEY_LENGTH_BITS)
        val saltBase64 = Base64.getEncoder().encodeToString(salt)
        val hashBase64 = Base64.getEncoder().encodeToString(derivedBytes)
        return "$SCHEME_PBKDF2:$DEFAULT_ITERATIONS:$saltBase64:$hashBase64"
    }

    fun verifyPin(pin: String, storedHash: String): PinVerificationResult {
        val trimmedHash = storedHash.trim()
        if (trimmedHash.isBlank()) return PinVerificationResult.Invalid

        if (trimmedHash.startsWith("$SCHEME_PBKDF2:")) {
            val parts = trimmedHash.split(":")
            if (parts.size != 4) return PinVerificationResult.Invalid
            val iterations = parts[1].toIntOrNull() ?: return PinVerificationResult.Invalid
            val salt = runCatching { Base64.getDecoder().decode(parts[2]) }.getOrNull()
                ?: return PinVerificationResult.Invalid
            val expectedHash = runCatching { Base64.getDecoder().decode(parts[3]) }.getOrNull()
                ?: return PinVerificationResult.Invalid

            val actualHash = deriveKey(pin.toCharArray(), salt, iterations, expectedHash.size * 8)
            val isValid = MessageDigest.isEqual(actualHash, expectedHash)
            return if (isValid) {
                PinVerificationResult.Valid(needsUpgrade = iterations < DEFAULT_ITERATIONS)
            } else {
                PinVerificationResult.Invalid
            }
        }

        // Legacy fast SHA-256 verification (unsalted, 64 hex characters)
        if (LEGACY_HEX_REGEX.matches(trimmedHash)) {
            val legacyHash = hashLegacySha256(pin)
            val isValid = MessageDigest.isEqual(
                legacyHash.toByteArray(Charsets.UTF_8),
                trimmedHash.lowercase().toByteArray(Charsets.UTF_8),
            )
            return if (isValid) {
                PinVerificationResult.Valid(needsUpgrade = true)
            } else {
                PinVerificationResult.Invalid
            }
        }

        return PinVerificationResult.Invalid
    }

    private fun deriveKey(chars: CharArray, salt: ByteArray, iterations: Int, keyLengthBits: Int): ByteArray {
        val spec = PBEKeySpec(chars, salt, iterations, keyLengthBits)
        return try {
            val factory = SecretKeyFactory.getInstance(ALGORITHM_PBKDF2)
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun hashLegacySha256(pin: String): String {
        return runCatching {
            MessageDigest.getInstance("SHA-256")
                .digest(pin.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        }.getOrDefault("")
    }

    companion object {
        const val SCHEME_PBKDF2 = "pbkdf2"
        const val ALGORITHM_PBKDF2 = "PBKDF2WithHmacSHA256"
        const val DEFAULT_ITERATIONS = 100_000
        const val SALT_BYTES = 16
        const val KEY_LENGTH_BITS = 256
        private val LEGACY_HEX_REGEX = Regex("^[a-fA-F0-9]{64}$")
    }
}
