package com.localdownloader.updates

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdateModelsTest {

    @Test
    fun startupUpdatePrompt_hasAnyUpdate_returnsTrueWhenAnyComponentHasUpdate() {
        val nonePrompt = StartupUpdatePrompt(
            appUpdate = ComponentUpdateCheck(
                currentVersion = "1.0.0",
                latestVersion = "1.0.0",
                updateAvailable = false,
                summary = "Up to date",
            ),
            ytDlpUpdate = null,
            ffmpegUpdate = null,
        )
        assertFalse(nonePrompt.hasAnyUpdate)

        val appPrompt = StartupUpdatePrompt(
            appUpdate = ComponentUpdateCheck(
                currentVersion = "1.0.0",
                latestVersion = "1.1.0",
                updateAvailable = true,
                summary = "Update available",
            ),
        )
        assertTrue(appPrompt.hasAnyUpdate)

        val ytdlpPrompt = StartupUpdatePrompt(
            ytDlpUpdate = ComponentUpdateCheck(
                currentVersion = "2026.01.01",
                latestVersion = "2026.02.01",
                updateAvailable = true,
                summary = "Update available",
            ),
        )
        assertTrue(ytdlpPrompt.hasAnyUpdate)

        val ffmpegPrompt = StartupUpdatePrompt(
            ffmpegUpdate = ComponentUpdateCheck(
                currentVersion = "6.0",
                latestVersion = "7.0",
                updateAvailable = true,
                summary = "Update available",
            ),
        )
        assertTrue(ffmpegPrompt.hasAnyUpdate)
    }

    @Test
    fun updatePreferences_defaultCheckUpdatesOnStartup_isTrue() {
        val prefs = UpdatePreferences()
        assertTrue(prefs.checkUpdatesOnStartup)
    }

    @Test
    fun compareLooseVersions_comparesSemVerAndDateVersionsCorrectly() {
        assertTrue(compareLooseVersions("1.0.0", "1.0.1") < 0)
        assertTrue(compareLooseVersions("1.1.0", "1.0.1") > 0)
        assertTrue(compareLooseVersions("2.0.0", "2.0.0") == 0)
        assertTrue(compareLooseVersions("2026.01.01", "2026.02.01") < 0)
        assertTrue(compareLooseVersions("v2.1.0", "2.1.0") == 0)
        assertTrue(compareLooseVersions("7.1.1", "7.1.2") < 0)
        assertTrue(compareLooseVersions("7.1.2", "7.1.1") > 0)
        assertTrue(compareLooseVersions("7.1.2", "v7.1.2") == 0)
    }
}
