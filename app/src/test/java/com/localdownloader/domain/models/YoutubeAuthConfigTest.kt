package com.localdownloader.domain.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YoutubeAuthConfigTest {

    @Test
    fun buildPoTokenValue_buildsMultiContextBundleFromSavedTokens() {
        val config = YoutubeAuthConfig(
            clientHint = "web.gvs",
            gvsToken = "gvs123",
            playerToken = "player456",
            subsToken = "subs789",
        )

        assertEquals(
            "web.gvs+gvs123,web.player+player456,web.subs+subs789",
            config.buildPoTokenValue(),
        )
    }

    @Test
    fun buildPoTokenValue_returnsNullWhenAllTokensBlank() {
        assertNull(YoutubeAuthConfig().buildPoTokenValue())
    }
}
