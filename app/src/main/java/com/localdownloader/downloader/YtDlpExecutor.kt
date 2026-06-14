package com.localdownloader.downloader

import android.content.Context
import com.localdownloader.utils.Logger
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YtDlpExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val binaryInstaller: BinaryInstaller,
    private val processRunner: ProcessRunner,
    private val logger: Logger,
) {
    @Volatile
    private var isInitialized = false

    suspend fun execute(
        args: List<String>,
        logOutputLines: Boolean = false,
        onStdoutLine: ((String) -> Unit)? = null,
        onStderrLine: ((String) -> Unit)? = null,
        timeoutMs: Long? = null,
    ): CommandResult {
        ensureRuntimeInitialized()

        val ffmpegRuntime = binaryInstaller.ensureFfmpegRuntime(preferNative = true)
        val runtime = resolveRuntime(
            ffmpegRuntime = ffmpegRuntime,
        )
        val normalizedArgs = normalizeArgs(args = args, runtime = runtime)
        val command = listOf(runtime.pythonBinary.absolutePath, runtime.ytDlpScript.absolutePath) + normalizedArgs
        logger.d("YtDlpExecutor", "Executing embedded yt-dlp runtime: ${command.joinToString(" ")}")
        logger.d(
            "YtDlpExecutor",
            "Runtime paths python=${runtime.pythonBinary.absolutePath}, ytdlp=${runtime.ytDlpScript.absolutePath}, ffmpeg=${runtime.ffmpegBinary.absolutePath}, quickjs=${runtime.quickJsBinary.absolutePath}",
        )

        return runProcess(
            command = command,
            environment = runtime.environment,
            logOutputLines = logOutputLines,
            onStdoutLine = onStdoutLine,
            onStderrLine = onStderrLine,
            timeoutMs = timeoutMs,
        )
    }

    private suspend fun runProcess(
        command: List<String>,
        environment: Map<String, String>,
        logOutputLines: Boolean,
        onStdoutLine: ((String) -> Unit)?,
        onStderrLine: ((String) -> Unit)?,
        timeoutMs: Long?,
    ): CommandResult {
        return withContext(Dispatchers.IO) {
            val startMs = System.currentTimeMillis()
            val result = processRunner.runCommand(
                command = command,
                environment = environment,
                onStdoutLine = { line ->
                    if (logOutputLines) {
                        logger.d("YtDlpExecutor/stdout", line)
                    }
                    onStdoutLine?.invoke(line)
                },
                onStderrLine = { line ->
                    if (logOutputLines) {
                        logger.d("YtDlpExecutor/stderr", line)
                    }
                    onStderrLine?.invoke(line)
                },
                timeoutMs = timeoutMs,
            )
            logger.i(
                "YtDlpExecutor",
                "Command finished exitCode=${result.exitCode}, durationMs=${System.currentTimeMillis() - startMs}, stdoutLen=${result.stdout.length}, stderrLen=${result.stderr.length}",
            )
            result
        }
    }

    private fun ensureRuntimeInitialized() {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            logger.i("YtDlpExecutor", "Initializing embedded yt-dlp runtime")
            YoutubeDL.getInstance().init(context)
            runCatching {
                FFmpeg.getInstance().init(context)
            }.onFailure { error ->
                logger.w("YtDlpExecutor", "AAR FFmpeg init failed (non-fatal; yt-dlp receives the FFmpeg path via --ffmpeg-location)", error)
            }
            isInitialized = true
            logger.i("YtDlpExecutor", "Embedded yt-dlp runtime initialized")
        }
    }

    private fun resolveRuntime(ffmpegRuntime: FfmpegRuntime): YtDlpRuntime {
        val nativeLibraryDir = File(context.applicationInfo.nativeLibraryDir)
        val baseDir = File(context.noBackupFilesDir, YoutubeDL.baseName)
        val packagesDir = File(baseDir, "packages")
        val pythonUsrDir = File(packagesDir, "python/usr")

        val pythonBinary = File(nativeLibraryDir, "libpython.so")
        val quickJsBinary = File(nativeLibraryDir, "libqjs.so")
        val ytDlpScript = File(
            File(baseDir, YoutubeDL.ytdlpDirName),
            YoutubeDL.ytdlpBin,
        )

        val ldLibraryEntries = mutableListOf<String>()
        ldLibraryEntries += File(pythonUsrDir, "lib").absolutePath
        ffmpegRuntime.supportDir?.let { supportDir ->
            val usrLib = File(supportDir, "usr/lib")
            if (usrLib.exists()) {
                ldLibraryEntries += usrLib.absolutePath
            } else if (supportDir.exists()) {
                ldLibraryEntries += supportDir.absolutePath
            }
        }
        val ldLibraryPath = ldLibraryEntries.distinct().joinToString(":")

        val environment = mutableMapOf(
            "LD_LIBRARY_PATH" to ldLibraryPath,
            "SSL_CERT_FILE" to File(pythonUsrDir, "etc/tls/cert.pem").absolutePath,
            "PYTHONHOME" to pythonUsrDir.absolutePath,
            "HOME" to pythonUsrDir.absolutePath,
            "TMPDIR" to context.cacheDir.absolutePath,
            "PATH" to listOfNotNull(
                System.getenv("PATH")?.takeIf { it.isNotBlank() },
                ffmpegRuntime.executable.parentFile?.absolutePath,
                nativeLibraryDir.absolutePath,
            ).joinToString(":"),
        )

        check(pythonBinary.exists()) { "Missing runtime binary: ${pythonBinary.absolutePath}" }
        check(quickJsBinary.exists()) { "Missing runtime binary: ${quickJsBinary.absolutePath}" }
        check(ffmpegRuntime.executable.exists()) { "Missing runtime binary: ${ffmpegRuntime.executable.absolutePath}" }
        check(ytDlpScript.exists()) { "Missing yt-dlp script: ${ytDlpScript.absolutePath}" }

        return YtDlpRuntime(
            pythonBinary = pythonBinary,
            quickJsBinary = quickJsBinary,
            ffmpegBinary = ffmpegRuntime.executable,
            ytDlpScript = ytDlpScript,
            environment = environment,
        )
    }

    private fun normalizeArgs(args: List<String>, runtime: YtDlpRuntime): List<String> {
        val normalized = args.toMutableList()
        var insertionIndex = normalized.indexOfFirst {
            it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true)
        }.let { if (it >= 0) it else normalized.size }

        val hasCacheDir = normalized.any { it == "--cache-dir" }
        if (!hasCacheDir) {
            normalized.add(insertionIndex, "--no-cache-dir")
            insertionIndex += 1
        }

        if (!normalized.contains("--js-runtimes")) {
            normalized.addAll(insertionIndex, listOf("--js-runtimes", "quickjs:${runtime.quickJsBinary.absolutePath}"))
            insertionIndex += 2
        }

        if (!normalized.contains("--ffmpeg-location")) {
            normalized.addAll(insertionIndex, listOf("--ffmpeg-location", runtime.ffmpegBinary.absolutePath))
        }

        return normalized
    }

    private data class YtDlpRuntime(
        val pythonBinary: File,
        val quickJsBinary: File,
        val ffmpegBinary: File,
        val ytDlpScript: File,
        val environment: Map<String, String>,
    )
}
