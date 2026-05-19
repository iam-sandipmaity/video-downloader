package com.localdownloader.viewmodel

import com.localdownloader.domain.models.CookieProfile
import com.localdownloader.domain.models.YoutubeAuthConfig
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
}
