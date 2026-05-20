package com.localdownloader.support

import android.content.Context
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.localdownloader.BuildConfig
import com.localdownloader.updates.FfmpegUpdateManager
import com.localdownloader.updates.YtDlpUpdateManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AppDiagnosticsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ytDlpUpdateManager: YtDlpUpdateManager,
    private val ffmpegUpdateManager: FfmpegUpdateManager,
) {
    suspend fun buildReport(): String = withContext(Dispatchers.IO) {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val appVersionName = packageInfo.versionName ?: BuildConfig.VERSION_NAME
        val appVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
        val ytDlpVersion = ytDlpUpdateManager.currentVersion() ?: "Unavailable"
        val ffmpegVersion = ffmpegUpdateManager.currentVersion() ?: "Unavailable"
        val ffmpegPackageVersion = ffmpegUpdateManager.installedPackageVersion() ?: "Bundled runtime"
        val logsDir = File(context.filesDir, "logs")
        val logFiles = logsDir.listFiles()
            ?.filter { it.isFile && it.length() > 0L }
            .orEmpty()

        buildString {
            appendLine("App diagnostics")
            appendLine()
            appendLine("App")
            appendLine("- Package: ${context.packageName}")
            appendLine("- Version: $appVersionName ($appVersionCode)")
            appendLine("- Build type: ${BuildConfig.BUILD_TYPE}")
            appendLine()
            appendLine("Device")
            appendLine("- Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("- Manufacturer: ${Build.MANUFACTURER}")
            appendLine("- Model: ${Build.MODEL}")
            appendLine("- ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine()
            appendLine("Runtime")
            appendLine("- yt-dlp: $ytDlpVersion")
            appendLine("- FFmpeg: $ffmpegVersion")
            appendLine("- FFmpeg package: $ffmpegPackageVersion")
            appendLine()
            appendLine("Files")
            appendLine("- Cache dir: ${context.cacheDir.absolutePath}")
            appendLine("- Files dir: ${context.filesDir.absolutePath}")
            appendLine("- Logs: ${if (logFiles.isEmpty()) "No logs yet" else logFiles.joinToString { it.name }}")
        }.trim()
    }
}
