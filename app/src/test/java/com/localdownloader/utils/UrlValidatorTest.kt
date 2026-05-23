package com.localdownloader.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlValidatorTest {
    private val validator = UrlValidator()

    @Test
    fun `http url is upgraded to https`() {
        val normalized = validator.normalizeForSecureUse("http://example.com/watch?v=123")

        assertNotNull(normalized)
        assertEquals("https://example.com/watch?v=123", normalized?.normalizedUrl)
        assertTrue(normalized?.upgradedToHttps == true)
    }

    @Test
    fun `https url stays unchanged`() {
        val normalized = validator.normalizeForSecureUse("https://example.com/path")

        assertNotNull(normalized)
        assertEquals("https://example.com/path", normalized?.normalizedUrl)
        assertFalse(normalized?.upgradedToHttps == true)
    }

    @Test
    fun `missing scheme defaults to https`() {
        val normalized = validator.normalizeForSecureUse("example.com/video")

        assertNotNull(normalized)
        assertEquals("https://example.com/video", normalized?.normalizedUrl)
        assertFalse(normalized?.upgradedToHttps == true)
    }

    @Test
    fun `invalid url is rejected`() {
        assertNull(validator.normalizeForSecureUse("not a real url"))
    }
}
