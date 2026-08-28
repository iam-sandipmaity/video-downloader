package com.localdownloader.updates

import com.localdownloader.downloader.YtDlpExecutor
import com.localdownloader.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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
        val comparableCurrentVersion = normalizeComparableYtDlpVersion(currentVersion)
        val release = gitHubReleaseClient.fetchRelease(channel.apiUrl)
        val latestVersion = normalizeReleaseVersion(release.tag_name)
        val updateAvailable = compareLooseVersions(comparableCurrentVersion, latestVersion) < 0
        return ComponentUpdateCheck(
            currentVersion = currentVersion,
            latestVersion = latestVersion,
            updateAvailable = updateAvailable,
            summary = if (updateAvailable) {
                "A newer ${channel.id} build is available."
            } else {
                "yt-dlp is already up to date on ${channel.id}."
            },
            releaseNotes = release.body,
            releasePageUrl = release.html_url,
        )
    }

    suspend fun installUpdate(
        channel: YtDlpReleaseChannel,
        onProgress: ((Int) -> Unit)? = null,
    ): RuntimeInstallResult {
        val release = gitHubReleaseClient.fetchRelease(channel.apiUrl)
        val latestVersion = normalizeReleaseVersion(release.tag_name)
        val currentVersion = currentVersion()
        val comparableCurrentVersion = normalizeComparableYtDlpVersion(currentVersion)
        if (compareLooseVersions(comparableCurrentVersion, latestVersion) >= 0) {
            return RuntimeInstallResult(
                updated = false,
                version = currentVersion,
                message = "yt-dlp is already up to date on ${channel.id}.",
            )
        }

        return runCatching {
            onProgress?.invoke(0)
            val updateArgs = buildYtDlpSelfUpdateArgs(channel)
            logger.i(
                "YtDlpUpdateManager",
                "Running yt-dlp self-update for channel=${channel.id} args=${updateArgs.joinToString(" ")}",
            )
            val scriptFile = ytDlpExecutor.installedYtDlpScript()
            val backupFile = File(scriptFile.parentFile, "${scriptFile.name}.pre-update")
            withContext(Dispatchers.IO) {
                if (!scriptFile.exists()) {
                    throw IllegalStateException("yt-dlp script is missing: ${scriptFile.absolutePath}")
                }
                scriptFile.copyTo(backupFile, overwrite = true)
            }
            try {
                val result = ytDlpExecutor.execute(updateArgs, logOutputLines = false)
                if (!result.isSuccess) {
                    throw IllegalStateException(
                        extractYtDlpUpdateFailureMessage(
                            stderr = result.stderr,
                            stdout = result.stdout,
                        ),
                    )
                }

                verifyInstalledYtDlpIntegrity(
                    release = release,
                    installedFile = scriptFile,
                )

                onProgress?.invoke(100)
                val installedVersion = currentVersion()
                val comparableInstalledVersion = normalizeComparableYtDlpVersion(installedVersion)
                if (compareLooseVersions(comparableInstalledVersion, latestVersion) < 0) {
                    throw IllegalStateException(
                        "yt-dlp self-update did not reach the expected ${channel.id} release.",
                    )
                }

                withContext(Dispatchers.IO) {
                    backupFile.delete()
                }

                RuntimeInstallResult(
                    updated = true,
                    version = installedVersion,
                    message = "Updated yt-dlp to ${installedVersion ?: latestVersion ?: channel.id}.",
                )
            } catch (error: Throwable) {
                withContext(Dispatchers.IO) {
                    if (backupFile.exists()) {
                        backupFile.copyTo(scriptFile, overwrite = true)
                        backupFile.delete()
                    }
                }
                throw error
            }
        }.getOrElse { error ->
            logger.e("YtDlpUpdateManager", "Failed installing yt-dlp update", error)
            throw error
        }
    }

    private suspend fun verifyInstalledYtDlpIntegrity(
        release: GitHubReleaseDto,
        installedFile: File,
    ) {
        val checksumAsset = findYtDlpChecksumAsset(release)
            ?: throw IllegalStateException("yt-dlp release is missing SHA2-256SUMS; refusing to keep an unverified binary.")
        val checksumPayload = gitHubReleaseClient.downloadText(checksumAsset.browser_download_url)
        val expectedDigest = findExpectedSha256ForInstalledYtDlp(
            checksumPayload = checksumPayload,
            installedFileName = installedFile.name,
        ) ?: throw IllegalStateException(
            "SHA2-256SUMS does not list a digest for ${installedFile.name}.",
        )
        val actualDigest = withContext(Dispatchers.IO) { sha256Hex(installedFile) }
        if (!expectedDigest.equals(actualDigest, ignoreCase = true)) {
            throw IllegalStateException("yt-dlp update failed integrity verification.")
        }
        logger.i(
            "YtDlpUpdateManager",
            "Verified yt-dlp checksum for ${installedFile.name} (${actualDigest.take(12)}...)",
        )
    }
}

internal fun buildYtDlpSelfUpdateArgs(channel: YtDlpReleaseChannel): List<String> {
    return listOf("--update-to", channel.id)
}

internal fun normalizeComparableYtDlpVersion(raw: String?): String? {
    val normalized = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
    val channelTaggedVersion = CHANNEL_TAGGED_YTDLP_VERSION_REGEX.find(normalized)
        ?.groupValues
        ?.getOrNull(1)
        ?.takeIf { it.isNotBlank() }
    if (channelTaggedVersion != null) {
        return channelTaggedVersion
    }
    val numericVersion = NUMERIC_YTDLP_VERSION_REGEX.find(normalized)?.value
    return numericVersion ?: normalizeReleaseVersion(normalized)
}

internal fun extractYtDlpUpdateFailureMessage(stderr: String, stdout: String): String {
    return sequenceOf(stderr, stdout)
        .flatMap { output ->
            output.lineSequence()
                .map { line -> line.trim().removePrefix("ERROR: ").trim() }
                .filter { it.isNotBlank() }
        }
        .firstOrNull()
        ?: "yt-dlp update failed"
}

private val CHANNEL_TAGGED_YTDLP_VERSION_REGEX = Regex(
    "^(?:stable|nightly|master)@([0-9]{4}\\.[0-9]{2}\\.[0-9]{2}(?:\\.[0-9]+)?)",
    RegexOption.IGNORE_CASE,
)
private val NUMERIC_YTDLP_VERSION_REGEX = Regex("[0-9]{4}\\.[0-9]{2}\\.[0-9]{2}(?:\\.[0-9]+)?")
