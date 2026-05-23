package com.localdownloader.updates

import android.content.Context
import com.localdownloader.downloader.YtDlpExecutor
import com.localdownloader.utils.Logger
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
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
        val asset = release.assets.firstOrNull { it.name == YTDLP_ASSET_NAME }
        val checksumAsset = release.assets.firstOrNull { it.name.equals(CHECKSUM_ASSET_NAME, ignoreCase = true) }
        val updateAvailable = compareLooseVersions(currentVersion, latestVersion) < 0 &&
            asset != null &&
            checksumAsset != null
        return ComponentUpdateCheck(
            currentVersion = currentVersion,
            latestVersion = latestVersion,
            updateAvailable = updateAvailable,
            summary = when {
                compareLooseVersions(currentVersion, latestVersion) < 0 && asset == null ->
                    "A newer ${channel.id} build exists, but the yt-dlp release asset was missing."
                compareLooseVersions(currentVersion, latestVersion) < 0 && checksumAsset == null ->
                    "A newer ${channel.id} build exists, but its checksum manifest was missing."
                updateAvailable ->
                    "A newer ${channel.id} build is available."
                else ->
                    "yt-dlp is already up to date on ${channel.id}."
            },
            releaseNotes = release.body,
            releasePageUrl = release.html_url,
            downloadUrl = asset?.browser_download_url,
            assetName = asset?.name,
        )
    }

    suspend fun installUpdate(
        channel: YtDlpReleaseChannel,
        onProgress: ((Int) -> Unit)? = null,
    ): RuntimeInstallResult = withContext(Dispatchers.IO) {
        val release = gitHubReleaseClient.fetchRelease(channel.apiUrl)
        val latestVersion = normalizeReleaseVersion(release.tag_name)
        val currentVersion = currentVersion()
        if (compareLooseVersions(currentVersion, latestVersion) >= 0) {
            return@withContext RuntimeInstallResult(
                updated = false,
                version = currentVersion,
                message = "yt-dlp is already up to date on ${channel.id}.",
            )
        }

        val asset = release.assets.firstOrNull { it.name == YTDLP_ASSET_NAME }
            ?: error("yt-dlp release asset '$YTDLP_ASSET_NAME' was not found")
        val checksumAsset = release.assets.firstOrNull { it.name.equals(CHECKSUM_ASSET_NAME, ignoreCase = true) }
            ?: error("yt-dlp checksum asset '$CHECKSUM_ASSET_NAME' was not found")

        val runtimeFile = runtimeFile()
        val backupFile = File(runtimeFile.parentFile, "${runtimeFile.name}.bak")
        val tempFile = File.createTempFile("ytdlp-update-", ".tmp", context.cacheDir)

        try {
            val checksumPayload = gitHubReleaseClient.downloadText(checksumAsset.browser_download_url)
            val expectedDigest = findExpectedSha256(checksumPayload, asset.name)
                ?: error("Checksum manifest did not contain an entry for '${asset.name}'")
            gitHubReleaseClient.downloadFile(asset.browser_download_url, tempFile, onProgress)
            val actualDigest = sha256Hex(tempFile)
            check(actualDigest.equals(expectedDigest, ignoreCase = true)) {
                "yt-dlp checksum verification failed for ${asset.name}"
            }
            backupFile.delete()
            if (runtimeFile.exists()) {
                runtimeFile.copyTo(backupFile, overwrite = true)
            } else {
                runtimeFile.parentFile?.mkdirs()
            }
            tempFile.copyTo(runtimeFile, overwrite = true)
            runtimeFile.setExecutable(true, false)

            val installedVersion = currentVersion()
            if (compareLooseVersions(installedVersion, latestVersion) < 0) {
                throw IllegalStateException("yt-dlp update verification failed after replacing the runtime file")
            }

            RuntimeInstallResult(
                updated = true,
                version = installedVersion,
                message = "Updated yt-dlp to ${installedVersion ?: latestVersion ?: channel.id}.",
            )
        } catch (error: Throwable) {
            logger.e("YtDlpUpdateManager", "Failed installing yt-dlp update", error)
            if (backupFile.exists()) {
                backupFile.copyTo(runtimeFile, overwrite = true)
                runtimeFile.setExecutable(true, false)
            }
            throw error
        } finally {
            tempFile.delete()
            backupFile.delete()
        }
    }

    private fun runtimeFile(): File {
        val baseDir = File(context.noBackupFilesDir, YoutubeDL.baseName)
        return File(File(baseDir, YoutubeDL.ytdlpDirName), YoutubeDL.ytdlpBin)
    }

    private companion object {
        private const val YTDLP_ASSET_NAME = "yt-dlp"
        private const val CHECKSUM_ASSET_NAME = "SHA2-256SUMS"
    }
}
