package com.localdownloader.downloader

import android.content.Context
import com.localdownloader.utils.Logger
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
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
        onStdoutLine: ((String) -> Unit)? = null,
        onStderrLine: ((String) -> Unit)? = null,
    ): CommandResult {
        return try {
            runStandaloneCommand(
                args = args,
                preferNative = true,
                onStdoutLine = onStdoutLine,
                onStderrLine = onStderrLine,
            )
        } catch (error: IOException) {
            logger.w(
                "YtDlpExecutor",
                "Standalone yt-dlp launch failed; falling back to embedded runtime",
                error,
            )
            runEmbeddedRuntimeCommand(args, onStdoutLine, onStderrLine)
        }
    }

    private suspend fun runStandaloneCommand(
        args: List<String>,
        preferNative: Boolean,
        onStdoutLine: ((String) -> Unit)?,
        onStderrLine: ((String) -> Unit)?,
    ): CommandResult {
        val ytDlpBinary = binaryInstaller.ensureYtDlpBinary(preferNative = preferNative)
        val ffmpegBinary = runCatching {
            binaryInstaller.ensureFfmpegBinary(preferNative = preferNative)
        }.getOrNull()

        val normalizedArgs = normalizeArgsForStandalone(args = args, ffmpegPath = ffmpegBinary?.absolutePath)
        val command = listOf(ytDlpBinary.absolutePath) + normalizedArgs
        logger.d(
            "YtDlpExecutor",
            "Executing standalone yt-dlp (${if (preferNative) "native" else "asset"}): ${command.joinToString(" ")}",
        )

        return try {
            runProcess(
                command = command,
                environment = emptyMap(),
                onStdoutLine = onStdoutLine,
                onStderrLine = onStderrLine,
            )
        } catch (error: IOException) {
            if (!preferNative || !shouldRetryWithAssetBinary(error)) throw error
            logger.w(
                "YtDlpExecutor",
                "Native-library yt-dlp launch failed; retrying with asset-installed binary",
                error,
            )
            runStandaloneCommand(
                args = args,
                preferNative = false,
                onStdoutLine = onStdoutLine,
                onStderrLine = onStderrLine,
            )
        }
    }

    private suspend fun runEmbeddedRuntimeCommand(
        args: List<String>,
        onStdoutLine: ((String) -> Unit)?,
        onStderrLine: ((String) -> Unit)?,
    ): CommandResult {
        ensureRuntimeInitialized()

        val runtime = resolveRuntime()
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
            onStdoutLine = onStdoutLine,
            onStderrLine = onStderrLine,
        )
    }

    private suspend fun runProcess(
        command: List<String>,
        environment: Map<String, String>,
        onStdoutLine: ((String) -> Unit)?,
        onStderrLine: ((String) -> Unit)?,
    ): CommandResult {
        return withContext(Dispatchers.IO) {
            val startMs = System.currentTimeMillis()
            val result = processRunner.runCommand(
                command = command,
                environment = environment,
                onStdoutLine = { line ->
                    logger.d("YtDlpExecutor/stdout", line)
                    onStdoutLine?.invoke(line)
                },
                onStderrLine = { line ->
                    logger.d("YtDlpExecutor/stderr", line)
                    onStderrLine?.invoke(line)
                },
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
            FFmpeg.getInstance().init(context)
            isInitialized = true
            logger.i("YtDlpExecutor", "Embedded yt-dlp runtime initialized")
        }
    }

    private fun resolveRuntime(): YtDlpRuntime {
        val nativeLibraryDir = File(context.applicationInfo.nativeLibraryDir)
        val baseDir = File(context.noBackupFilesDir, YoutubeDL.baseName)
        val packagesDir = File(baseDir, "packages")
        val pythonUsrDir = File(packagesDir, "python/usr")
        val ffmpegUsrLibDir = File(packagesDir, "ffmpeg/usr/lib")
        val aria2cUsrLibDir = File(packagesDir, "aria2c/usr/lib")

        val pythonBinary = File(nativeLibraryDir, "libpython.so")
        val quickJsBinary = File(nativeLibraryDir, "libqjs.so")
        val ffmpegBinary = File(nativeLibraryDir, "libffmpeg.so")
        val ytDlpScript = File(
            File(baseDir, YoutubeDL.ytdlpDirName),
            YoutubeDL.ytdlpBin,
        )

        val ldLibraryPath = listOf(
            File(pythonUsrDir, "lib").absolutePath,
            ffmpegUsrLibDir.absolutePath,
            aria2cUsrLibDir.absolutePath,
        ).joinToString(":")

        val environment = mutableMapOf(
            "LD_LIBRARY_PATH" to ldLibraryPath,
            "SSL_CERT_FILE" to File(pythonUsrDir, "etc/tls/cert.pem").absolutePath,
            "PYTHONHOME" to pythonUsrDir.absolutePath,
            "HOME" to pythonUsrDir.absolutePath,
            "TMPDIR" to context.cacheDir.absolutePath,
            "PATH" to listOfNotNull(
                System.getenv("PATH")?.takeIf { it.isNotBlank() },
                nativeLibraryDir.absolutePath,
            ).joinToString(":"),
        )

        check(pythonBinary.exists()) { "Missing runtime binary: ${pythonBinary.absolutePath}" }
        check(quickJsBinary.exists()) { "Missing runtime binary: ${quickJsBinary.absolutePath}" }
        check(ffmpegBinary.exists()) { "Missing runtime binary: ${ffmpegBinary.absolutePath}" }
        check(ytDlpScript.exists()) { "Missing yt-dlp script: ${ytDlpScript.absolutePath}" }

        return YtDlpRuntime(
            pythonBinary = pythonBinary,
            quickJsBinary = quickJsBinary,
            ffmpegBinary = ffmpegBinary,
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

    private fun normalizeArgsForStandalone(args: List<String>, ffmpegPath: String?): List<String> {
        val normalized = args.toMutableList()
        var insertionIndex = normalized.indexOfFirst {
            it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true)
        }.let { if (it >= 0) it else normalized.size }

        val hasCacheDir = normalized.any { it == "--cache-dir" }
        if (!hasCacheDir) {
            normalized.add(insertionIndex, "--no-cache-dir")
            insertionIndex += 1
        }

        if (!ffmpegPath.isNullOrBlank() && !normalized.contains("--ffmpeg-location")) {
            normalized.addAll(insertionIndex, listOf("--ffmpeg-location", ffmpegPath))
        }

        return normalized
    }

    private fun shouldRetryWithAssetBinary(error: IOException): Boolean {
        val detail = buildString {
            append(error.message.orEmpty())
            val causeMessage = error.cause?.message.orEmpty()
            if (causeMessage.isNotBlank()) {
                append(" ")
                append(causeMessage)
            }
        }.lowercase()

        return detail.contains("no such file") ||
            detail.contains("error=2") ||
            detail.contains("permission denied") ||
            detail.contains("exec format error")
    }

    private data class YtDlpRuntime(
        val pythonBinary: File,
        val quickJsBinary: File,
        val ffmpegBinary: File,
        val ytDlpScript: File,
        val environment: Map<String, String>,
    )
}
