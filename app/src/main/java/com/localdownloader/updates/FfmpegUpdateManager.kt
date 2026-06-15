package com.localdownloader.updates

import com.localdownloader.downloader.BinaryInstaller
import com.localdownloader.downloader.ProcessRunner
import com.localdownloader.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FfmpegUpdateManager @Inject constructor(
    private val binaryInstaller: BinaryInstaller,
    private val processRunner: ProcessRunner,
    private val logger: Logger,
) {
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
            logger.w("FfmpegUpdateManager", "Failed to read bundled FFmpeg version", error)
            null
        }
    }

    suspend fun check(): ComponentUpdateCheck {
        val runtimeVersion = currentVersion()
        return ComponentUpdateCheck(
            currentVersion = runtimeVersion?.let { "$it (bundled)" },
            latestVersion = runtimeVersion,
            updateAvailable = false,
            requiresInitialInstall = false,
            summary = if (runtimeVersion.isNullOrBlank()) {
                "Bundled FFmpeg is unavailable. Rebuild the packaged runtime from official FFmpeg sources."
            } else {
                "Bundled FFmpeg is active. Runtime updates are delivered with app releases."
            },
            releaseNotes = null,
            releasePageUrl = "https://ffmpeg.org/",
        )
    }

    private fun parseFfmpegVersion(line: String?): String? {
        val match = FFMPEG_VERSION_REGEX.find(line.orEmpty()) ?: return null
        return match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    private companion object {
        private val FFMPEG_VERSION_REGEX = Regex("ffmpeg version\\s+n?([0-9]+(?:\\.[0-9]+)+)", RegexOption.IGNORE_CASE)
    }
}
