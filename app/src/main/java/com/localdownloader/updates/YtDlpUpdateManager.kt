package com.localdownloader.updates

import com.localdownloader.downloader.YtDlpExecutor
import com.localdownloader.utils.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpUpdateManager @Inject constructor(
    private val gitHubReleaseClient: GitHubReleaseClient,
    private val ytDlpExecutor: YtDlpExecutor,
    private val logger: Logger,
) {

    suspend fun currentVersion(): String? {
        return runCatching {
            val result = ytDlpExecutor.execute(listOf("--version"))
            result.stdout.lineSequence()
                .map(String::trim)
                .firstOrNull { it.isNotBlank() }
                ?.takeIf { result.exitCode == 0 }
        }.getOrElse { error ->
            logger.w("YtDlpUpdateManager", "Failed to read current yt-dlp version", error)
            null
        }
    }

    suspend fun check(channel: YtDlpReleaseChannel): ComponentUpdateCheck {
        val currentVersion = currentVersion()
        val release = gitHubReleaseClient.fetchRelease(channel.apiUrl)
        val latestVersion = normalizeReleaseVersion(release.tag_name)
        return ComponentUpdateCheck(
            currentVersion = currentVersion,
            latestVersion = latestVersion,
            updateAvailable = false,
            summary = buildDisabledSummary(channel, latestVersion),
            releaseNotes = release.body,
            releasePageUrl = release.html_url,
        )
    }

    suspend fun installUpdate(
        channel: YtDlpReleaseChannel,
        onProgress: ((Int) -> Unit)? = null,
    ): RuntimeInstallResult {
        onProgress?.invoke(0)
        logger.w(
            "YtDlpUpdateManager",
            "Blocked in-app yt-dlp update for ${channel.id}: $IN_APP_RUNTIME_UPDATES_DISABLED_REASON",
        )
        throw SecurityException(IN_APP_RUNTIME_UPDATES_DISABLED_REASON)
    }

    private fun buildDisabledSummary(
        channel: YtDlpReleaseChannel,
        latestVersion: String?,
    ): String {
        val latestLabel = latestVersion?.takeIf { it.isNotBlank() } ?: "unknown"
        return "$IN_APP_RUNTIME_UPDATES_DISABLED_REASON Latest ${channel.id} release: $latestLabel."
    }

    companion object {
        const val IN_APP_RUNTIME_UPDATES_ENABLED: Boolean = false
        const val IN_APP_RUNTIME_UPDATES_DISABLED_REASON: String =
            "In-app yt-dlp updates are disabled until release authenticity can be verified independently."
    }
}
