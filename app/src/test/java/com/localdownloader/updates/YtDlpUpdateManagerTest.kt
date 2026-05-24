package com.localdownloader.updates

import kotlin.test.Test
import kotlin.test.assertEquals

class YtDlpUpdateManagerTest {

    @Test
    fun normalizeComparableYtDlpVersion_keepsStableVersion() {
        assertEquals(
            "2026.05.05",
            normalizeComparableYtDlpVersion("2026.05.05"),
        )
    }

    @Test
    fun normalizeComparableYtDlpVersion_extractsNightlyTag() {
        assertEquals(
            "2026.05.05.233942",
            normalizeComparableYtDlpVersion(
                "nightly@2026.05.05.233942 from yt-dlp/yt-dlp-nightly-builds [abcdef123]",
            ),
        )
    }

    @Test
    fun normalizeComparableYtDlpVersion_extractsMasterTag() {
        assertEquals(
            "2026.05.24.101010",
            normalizeComparableYtDlpVersion(
                "master@2026.05.24.101010 from yt-dlp/yt-dlp-master-builds [deadbeef]",
            ),
        )
    }

    @Test
    fun buildYtDlpSelfUpdateArgs_targetsSelectedChannel() {
        assertEquals(
            listOf("--update-to", "stable"),
            buildYtDlpSelfUpdateArgs(YtDlpReleaseChannel.STABLE),
        )
        assertEquals(
            listOf("--update-to", "nightly"),
            buildYtDlpSelfUpdateArgs(YtDlpReleaseChannel.NIGHTLY),
        )
        assertEquals(
            listOf("--update-to", "master"),
            buildYtDlpSelfUpdateArgs(YtDlpReleaseChannel.MASTER),
        )
    }

    @Test
    fun extractYtDlpUpdateFailureMessage_prefersSanitizedErrorLine() {
        assertEquals(
            "Unable to update binary",
            extractYtDlpUpdateFailureMessage(
                stderr = "ERROR: Unable to update binary\n",
                stdout = "downloading...",
            ),
        )
    }
}
