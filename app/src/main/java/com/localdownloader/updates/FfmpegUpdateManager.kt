package com.localdownloader.updates

import android.content.Context
import android.os.Build
import com.localdownloader.downloader.BinaryInstaller
import com.localdownloader.downloader.ProcessRunner
import com.localdownloader.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FfmpegUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gitHubReleaseClient: GitHubReleaseClient,
    private val apkSignatureVerifier: ApkSignatureVerifier,
    private val binaryInstaller: BinaryInstaller,
    private val processRunner: ProcessRunner,
    private val logger: Logger,
) {
    private val overlayVersionFile: File
        get() = File(binaryInstaller.ffmpegOverlayDir(), ".version")

    suspend fun currentVersion(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val runtime = binaryInstaller.ensureFfmpegRuntime(preferNative = true)
            val result = processRunner.runCommand(
                command = listOf(runtime.executable.absolutePath, "-version"),
                environment = runtime.environment,
            )
            parseFfmpegVersion(result.stdout.lineSequence().firstOrNull { it.isNotBlank() })
                ?: parseFfmpegVersion(result.stderr.lineSequence().firstOrNull { it.isNotBlank() })
        }.getOrElse { error ->
            logger.w("FfmpegUpdateManager", "Failed to read current FFmpeg version", error)
            null
        }
    }

    fun installedPackageVersion(): String? {
        return overlayVersionFile.takeIf { it.exists() && it.isFile }
            ?.readText()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    suspend fun check(channel: FfmpegReleaseChannel): ComponentUpdateCheck {
        val runtimeVersion = currentVersion()
        val installedPackageVersion = installedPackageVersion()
        val release = resolveLatestRelease(channel)
        val latestVersion = release.version
        val requiresInitialInstall = installedPackageVersion.isNullOrBlank()
        val updateAvailable = !requiresInitialInstall &&
            compareLooseVersions(installedPackageVersion, latestVersion) < 0
        return ComponentUpdateCheck(
            currentVersion = buildDisplayVersion(
                runtimeVersion = runtimeVersion,
                packageVersion = installedPackageVersion,
            ),
            latestVersion = latestVersion,
            updateAvailable = updateAvailable,
            requiresInitialInstall = requiresInitialInstall,
            summary = when {
                requiresInitialInstall ->
                    "Bundled FFmpeg is active. Install the managed runtime package if you want direct in-app FFmpeg updates."
                updateAvailable ->
                    "A newer managed FFmpeg runtime package is available."
                else ->
                    "Managed FFmpeg runtime is already on the latest stable feed."
            },
            releaseNotes = release.release.body,
            releasePageUrl = release.release.html_url,
            downloadUrl = release.asset.browser_download_url,
            assetName = release.asset.name,
        )
    }

    suspend fun installUpdate(
        channel: FfmpegReleaseChannel,
        onProgress: ((Int) -> Unit)? = null,
    ): RuntimeInstallResult = withContext(Dispatchers.IO) {
        val currentPackageVersion = installedPackageVersion()
        val release = resolveLatestRelease(channel)
        if (!currentPackageVersion.isNullOrBlank() && compareLooseVersions(currentPackageVersion, release.version) >= 0) {
            return@withContext RuntimeInstallResult(
                updated = false,
                version = buildDisplayVersion(
                    runtimeVersion = currentVersion(),
                    packageVersion = currentPackageVersion,
                ),
                message = "FFmpeg is already on the latest stable runtime feed.",
            )
        }

        val tempApk = File.createTempFile("ffmpeg-runtime-", ".apk", context.cacheDir)
        val overlayDir = binaryInstaller.ffmpegOverlayDir()
        val backupDir = File(overlayDir.parentFile, "${overlayDir.name}.backup")

        try {
            gitHubReleaseClient.downloadFile(release.asset.browser_download_url, tempApk, onProgress)
            apkSignatureVerifier.verifySignerDigest(tempApk, TRUSTED_RUNTIME_SIGNER_SHA256_DIGESTS)

            val stagedDir = File(overlayDir.parentFile, "${overlayDir.name}.staging")
            stagedDir.deleteRecursively()
            stagedDir.mkdirs()
            extractRuntimeFromApk(tempApk, stagedDir)
            File(stagedDir, ".version").writeText(release.version.orEmpty())

            backupDir.deleteRecursively()
            if (overlayDir.exists()) {
                overlayDir.copyRecursively(backupDir, overwrite = true)
            }

            overlayDir.deleteRecursively()
            stagedDir.copyRecursively(overlayDir, overwrite = true)
            stagedDir.deleteRecursively()

            val installedRuntimeVersion = currentVersion()
            val installedPackageVersion = installedPackageVersion()
            if (installedRuntimeVersion.isNullOrBlank()) {
                throw IllegalStateException("FFmpeg update verification failed because the runtime did not report a version")
            }
            if (installedPackageVersion.isNullOrBlank() || compareLooseVersions(installedPackageVersion, release.version) < 0) {
                throw IllegalStateException("FFmpeg update verification failed after installing the overlay runtime")
            }

            RuntimeInstallResult(
                updated = true,
                version = buildDisplayVersion(
                    runtimeVersion = installedRuntimeVersion,
                    packageVersion = installedPackageVersion,
                ),
                message = "Updated FFmpeg runtime package to ${installedPackageVersion ?: release.version}.",
            )
        } catch (error: Throwable) {
            logger.e("FfmpegUpdateManager", "Failed installing FFmpeg runtime update", error)
            overlayDir.deleteRecursively()
            if (backupDir.exists()) {
                backupDir.copyRecursively(overlayDir, overwrite = true)
            }
            throw error
        } finally {
            tempApk.delete()
            backupDir.deleteRecursively()
        }
    }

    private suspend fun resolveLatestRelease(channel: FfmpegReleaseChannel): ResolvedFfmpegRelease {
        val releases = gitHubReleaseClient.fetchReleases(PACKAGE_REPOSITORY)
        val filtered = releases.asSequence()
            .filter { it.tag_name.contains(PACKAGE_NAME, ignoreCase = true) }
            .mapNotNull { release ->
                val asset = release.assets.firstOrNull { asset ->
                    asset.name.endsWith(".apk", ignoreCase = true) &&
                        asset.name.contains(supportedAbiSuffix(), ignoreCase = true)
                } ?: return@mapNotNull null
                ResolvedFfmpegRelease(
                    release = release,
                    asset = asset,
                    version = parsePackageVersion(release.tag_name),
                )
            }
            .sortedByDescending { it.release.published_at.orEmpty() }
            .toList()

        return filtered.firstOrNull()
            ?: throw IOException("No FFmpeg runtime packages were found for ${supportedAbiSuffix()} on ${channel.id}")
    }

    private fun extractRuntimeFromApk(sourceApk: File, targetDir: File) {
        val abiPrefix = "lib/${supportedAbiSuffix()}/"
        val binaryOutput = File(targetDir, "libffmpeg.so")
        val supportArchiveOutput = File(targetDir, "libffmpeg.zip.so")
        ZipInputStream(FileInputStream(sourceApk)).use { zip ->
            var foundBinary = false
            var foundSupportArchive = false
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                when (entry.name) {
                    "${abiPrefix}libffmpeg.so" -> {
                        binaryOutput.outputStream().use { output ->
                            zip.copyTo(output)
                        }
                        foundBinary = true
                    }
                    "${abiPrefix}libffmpeg.zip.so" -> {
                        supportArchiveOutput.outputStream().use { output ->
                            zip.copyTo(output)
                        }
                        foundSupportArchive = true
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
            if (!foundBinary) {
                throw IOException("FFmpeg update package is missing libffmpeg.so for ${supportedAbiSuffix()}")
            }
            if (!foundSupportArchive) {
                logger.w(
                    "FfmpegUpdateManager",
                    "FFmpeg update package did not include libffmpeg.zip.so; continuing without support archive",
                )
            }
        }
        if (!binaryOutput.setExecutable(true, false) && !binaryOutput.canExecute()) {
            throw IOException("Updated FFmpeg binary could not be marked executable")
        }
    }

    private fun supportedAbiSuffix(): String {
        val abi = Build.SUPPORTED_ABIS.firstOrNull().orEmpty()
        return when {
            abi.startsWith("arm64") -> "arm64-v8a"
            abi.startsWith("armeabi") -> "armeabi-v7a"
            abi.startsWith("x86_64") -> "x86_64"
            abi.startsWith("x86") -> "x86"
            else -> "arm64-v8a"
        }
    }

    private fun parsePackageVersion(tagName: String): String? {
        return tagName.split("-").getOrNull(1)?.takeIf { it.isNotBlank() }
            ?: normalizeReleaseVersion(tagName)
    }

    private fun parseFfmpegVersion(line: String?): String? {
        val match = FFMPEG_VERSION_REGEX.find(line.orEmpty()) ?: return null
        return match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    private fun buildDisplayVersion(runtimeVersion: String?, packageVersion: String?): String? {
        return when {
            !runtimeVersion.isNullOrBlank() && !packageVersion.isNullOrBlank() ->
                "$runtimeVersion (package $packageVersion)"
            !runtimeVersion.isNullOrBlank() ->
                "$runtimeVersion (bundled)"
            !packageVersion.isNullOrBlank() ->
                "package $packageVersion"
            else -> null
        }
    }

    private data class ResolvedFfmpegRelease(
        val release: GitHubReleaseDto,
        val asset: GitHubAssetDto,
        val version: String?,
    )

    private companion object {
        private const val PACKAGE_REPOSITORY = "deniscerri/ytdlnis-packages"
        private const val PACKAGE_NAME = "ffmpeg"
        // Trusted YTDLnis signer fingerprint documented by the upstream project.
        private val TRUSTED_RUNTIME_SIGNER_SHA256_DIGESTS = setOf(
            "263645cb5272eb290759fe1f59149ae24df6ce171e9f6666eead981d3fc64c95",
        )
        private val FFMPEG_VERSION_REGEX = Regex("ffmpeg version\\s+n?([0-9]+(?:\\.[0-9]+)+)", RegexOption.IGNORE_CASE)
    }
}
