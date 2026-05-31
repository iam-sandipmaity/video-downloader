package com.localdownloader.updates

import android.content.Context
import android.os.Build
import com.localdownloader.BuildConfig
import com.localdownloader.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gitHubReleaseClient: GitHubReleaseClient,
    private val apkSignatureVerifier: ApkSignatureVerifier,
    private val logger: Logger,
) {

    fun currentVersionLabel(): String {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        return "${BuildConfig.VERSION_NAME} ($abi)"
    }

    suspend fun checkForUpdate(includePrerelease: Boolean): ComponentUpdateCheck {
        val releases = gitHubReleaseClient.fetchReleases(APP_REPOSITORY)
        val targetRelease = releases.firstOrNull { release ->
            if (isNightlyChannel()) {
                release.tag_name.equals(NIGHTLY_TAG, ignoreCase = true)
            } else {
                !release.tag_name.equals(NIGHTLY_TAG, ignoreCase = true) &&
                    (includePrerelease || !release.prerelease)
            }
        }
            ?: throw IOException("No matching app releases were found on GitHub")
        val selectedAsset = selectApkAsset(targetRelease)
        val latestVersion = releaseVersion(targetRelease, selectedAsset)
        val versionComparison = compareLooseVersions(BuildConfig.VERSION_NAME, latestVersion)
        val updateAvailable = versionComparison < 0 && selectedAsset != null
        return ComponentUpdateCheck(
            currentVersion = BuildConfig.VERSION_NAME,
            latestVersion = latestVersion,
            updateAvailable = updateAvailable,
            summary = if (versionComparison < 0 && selectedAsset == null) {
                "A newer release exists, but no installable APK asset was found for this device."
            } else if (updateAvailable) {
                "A newer app release is available."
            } else {
                "You are already on the latest app release."
            },
            releaseNotes = targetRelease.body,
            releasePageUrl = targetRelease.html_url,
            downloadUrl = selectedAsset?.browser_download_url,
            assetName = selectedAsset?.name,
        )
    }

    suspend fun prepareInstall(
        check: ComponentUpdateCheck,
        onProgress: ((Int) -> Unit)? = null,
    ): PreparedAppUpdate = withContext(Dispatchers.IO) {
        val downloadUrl = check.downloadUrl ?: throw IOException("This release does not expose an installable APK asset")
        val assetName = check.assetName ?: "app-update.apk"
        validateSelectedAssetChannel(assetName)
        val updatesDir = File(context.cacheDir, "app-updates").apply {
            mkdirs()
            listFiles()?.forEach { if (it.isFile) it.delete() }
        }
        val apkFile = File(updatesDir, assetName)
        try {
            gitHubReleaseClient.downloadFile(downloadUrl, apkFile, onProgress)
            apkSignatureVerifier.verifyMatchesInstalledSigner(apkFile)
            PreparedAppUpdate(
                apkPath = apkFile.absolutePath,
                requiresInstallPermission = !canRequestPackageInstalls(),
                assetName = assetName,
            )
        } catch (error: Throwable) {
            apkFile.delete()
            logger.e("AppUpdateManager", "Failed preparing app update install", error)
            throw error
        }
    }

    fun refreshPreparedInstall(preparedUpdate: PreparedAppUpdate): PreparedAppUpdate? {
        val apkFile = File(preparedUpdate.apkPath)
        if (!apkFile.exists() || !apkFile.isFile) return null
        return preparedUpdate.copy(requiresInstallPermission = !canRequestPackageInstalls())
    }

    fun canRequestPackageInstalls(): Boolean {
        return context.packageManager.canRequestPackageInstalls()
    }

    private fun selectApkAsset(release: GitHubReleaseDto): GitHubAssetDto? {
        val apkAssets = release.assets
            .filter { it.name.endsWith(".apk", ignoreCase = true) }
            .filter { asset ->
                if (isNightlyChannel()) {
                    isNightlyAsset(asset.name)
                } else {
                    !isNightlyAsset(asset.name)
                }
            }
        if (apkAssets.isEmpty()) return null
        if (isNightlyChannel()) {
            return apkAssets.maxWithOrNull { left, right ->
                compareLooseVersions(
                    parseNightlyAssetVersion(left.name),
                    parseNightlyAssetVersion(right.name),
                )
            }
        }

        val abi = when {
            Build.SUPPORTED_ABIS.firstOrNull()?.startsWith("arm64") == true -> "arm64-v8a"
            Build.SUPPORTED_ABIS.firstOrNull()?.startsWith("armeabi") == true -> "armeabi-v7a"
            Build.SUPPORTED_ABIS.firstOrNull()?.startsWith("x86_64") == true -> "x86_64"
            Build.SUPPORTED_ABIS.firstOrNull()?.startsWith("x86") == true -> "x86"
            else -> null
        }

        return apkAssets.firstOrNull { asset ->
            abi != null && asset.name.contains(abi, ignoreCase = true)
        } ?: apkAssets.firstOrNull { asset ->
            asset.name.contains("universal", ignoreCase = true) || asset.name.contains("github", ignoreCase = true)
        } ?: apkAssets.firstOrNull { asset ->
            !asset.name.contains("debug", ignoreCase = true)
        } ?: apkAssets.first()
    }

    private fun releaseVersion(release: GitHubReleaseDto, selectedAsset: GitHubAssetDto?): String? {
        return if (isNightlyChannel()) {
            parseNightlyAssetVersion(selectedAsset?.name)
                ?: parseNightlyReleaseNameVersion(release.name)
                ?: normalizeReleaseVersion(release.tag_name)
        } else {
            normalizeReleaseVersion(release.tag_name)
        }
    }

    private fun isNightlyChannel(): Boolean {
        return BuildConfig.APP_RELEASE_CHANNEL.equals(NIGHTLY_TAG, ignoreCase = true)
    }

    private fun validateSelectedAssetChannel(assetName: String) {
        val nightlyAsset = isNightlyAsset(assetName)
        if (isNightlyChannel() && !nightlyAsset) {
            throw IOException("Blocked update: nightly builds can only install APK assets from the nightly release channel.")
        }
        if (!isNightlyChannel() && nightlyAsset) {
            throw IOException("Blocked update: stable builds cannot install nightly APK assets.")
        }
    }

    private companion object {
        private const val APP_REPOSITORY = "iam-sandipmaity/video-downloader"
        private const val NIGHTLY_TAG = "nightly"
        private val NIGHTLY_ASSET_VERSION_REGEX =
            Regex("""^(?:nightly-v?([0-9]+(?:\.[0-9]+)+)|video-downloader-v?([0-9]+(?:\.[0-9]+)+)-nightly-debug)\.apk$""")
        private val NIGHTLY_RELEASE_NAME_VERSION_REGEX =
            Regex("""\bv([0-9]+(?:\.[0-9]+)+)\b""")

        private fun isNightlyAsset(name: String): Boolean {
            return NIGHTLY_ASSET_VERSION_REGEX.matches(name)
        }

        private fun parseNightlyAssetVersion(name: String?): String? {
            val match = name
                ?.let(NIGHTLY_ASSET_VERSION_REGEX::matchEntire)
                ?: return null
            return match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
                ?: match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
        }

        private fun parseNightlyReleaseNameVersion(name: String?): String? {
            return name
                ?.let(NIGHTLY_RELEASE_NAME_VERSION_REGEX::find)
                ?.groupValues
                ?.getOrNull(1)
        }
    }
}
