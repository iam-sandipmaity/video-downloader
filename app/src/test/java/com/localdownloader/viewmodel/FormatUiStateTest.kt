package com.localdownloader.viewmodel

import com.localdownloader.domain.models.CookieProfile
import com.localdownloader.domain.models.YoutubeAuthConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatUiStateTest {

    @Test
    fun shouldShowDownloadSetupNotice_whenSettingsLoadedAndNoAuthOrCookies() {
        val state = FormatUiState(
            hasLoadedSettings = true,
        )

        assertTrue(state.shouldShowDownloadSetupNotice)
    }

    @Test
    fun shouldShowDownloadSetupNotice_isFalseWhenCookiesExist() {
        val state = FormatUiState(
            hasLoadedSettings = true,
            cookieProfiles = listOf(
                CookieProfile(
                    id = "cookie-1",
                    url = "https://youtube.com",
                    cookiesText = "SID=value",
                ),
            ),
        )

        assertFalse(state.shouldShowDownloadSetupNotice)
    }

    @Test
    fun shouldShowDownloadSetupNotice_isFalseWhenYoutubeAccessIsConfigured() {
        val state = FormatUiState(
            hasLoadedSettings = true,
            youtubeAuthConfig = YoutubeAuthConfig(
                enabled = true,
                gvsToken = "gvs123",
            ),
        )

        assertFalse(state.shouldShowDownloadSetupNotice)
    }

    @Test
    fun scopedMessages_onlyAppearOnMatchingScreen() {
        val state = FormatUiState(
            messageScope = FormatMessageScope.COOKIES,
            infoMessage = "Cookie saved.",
            errorMessage = null,
        )

        assertEquals("Cookie saved.", state.infoMessageFor(FormatMessageScope.COOKIES))
        assertNull(state.infoMessageFor(FormatMessageScope.BROWSER))
        assertNull(state.errorMessageFor(FormatMessageScope.YOUTUBE_ACCESS))
    }
}
