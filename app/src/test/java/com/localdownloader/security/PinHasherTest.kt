package com.localdownloader.security

import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PinHasherTest {

    @Test
    fun hash_isSaltedAndVerifiable() {
        val first = PinHasher.hash("2468")
        val second = PinHasher.hash("2468")

        assertTrue(first.startsWith("pbkdf2$"))
        assertTrue(first != second)
        assertTrue(PinHasher.verify("2468", first))
        assertTrue(PinHasher.verify("2468", second))
        assertFalse(PinHasher.verify("0000", first))
        assertFalse(PinHasher.needsUpgrade(first))
    }

    @Test
    fun verify_acceptsLegacyUnsaltedSha256AndMarksUpgrade() {
        val legacy = MessageDigest.getInstance("SHA-256")
            .digest("1357".toByteArray())
            .joinToString("") { "%02x".format(it) }

        assertTrue(PinHasher.verify("1357", legacy))
        assertFalse(PinHasher.verify("0000", legacy))
        assertTrue(PinHasher.needsUpgrade(legacy))
    }
}
