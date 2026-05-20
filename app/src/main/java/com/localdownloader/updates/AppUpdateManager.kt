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
    private val logger: Logger,
) {

    fun currentVersionLabel(): String {
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        return "${BuildConfig.VERSION_NAME} ($abi)"
    }

    suspend fun checkForUpdate(includePrerelease: Boolean): ComponentUpdateCheck {
        val releases = gitHubReleaseClient.fetchReleases(APP_REPOSITORY)
        val targetRelease = releases.firstOrNull { includePrerelease || !it.prerelease }
            ?: throw IOException("No matching app releases were found on GitHub")
        val latestVersion = normalizeReleaseVersion(targetRelease.tag_name)
        val selectedAsset = selectApkAsset(targetRelease)
        val updateAvailable = compareLooseVersions(BuildConfig.VERSION_NAME, latestVersion) < 0 && selectedAsset != null
        return ComponentUpdateCheck(
            currentVersion = BuildConfig.VERSION_NAME,
            latestVersion = latestVersion,
            updateAvailable = updateAvailable,
            summary = if (compareLooseVersions(BuildConfig.VERSION_NAME, latestVersion) < 0 && selectedAsset == null) {
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
        val updatesDir = File(context.cacheDir, "app-updates").apply {
            mkdirs()
            listFiles()?.forEach { if (it.isFile) it.delete() }
        }
        val apkFile = File(updatesDir, assetName)
        gitHubReleaseClient.downloadFile(downloadUrl, apkFile, onProgress)
        PreparedAppUpdate(
            apkPath = apkFile.absolutePath,
            requiresInstallPermission = !canRequestPackageInstalls(),
            assetName = assetName,
        )
    }

    fun refreshPreparedInstall(preparedUpdate: PreparedAppUpdate): PreparedAppUpdate? {
        val apkFile = File(preparedUpdate.apkPath)
        if (!apkFile.exists() || !apkFile.isFile) return null
        return preparedUpdate.copy(requiresInstallPermission = !canRequestPackageInstalls())
    }

    fun canRequestPackageInstalls(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    private fun selectApkAsset(release: GitHubReleaseDto): GitHubAssetDto? {
        val apkAssets = release.assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        if (apkAssets.isEmpty()) return null

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

    private companion object {
        private const val APP_REPOSITORY = "iam-sandipmaity/video-downloader"
    }
}
