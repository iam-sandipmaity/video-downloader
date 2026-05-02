package com.localdownloader.ffmpeg

import com.localdownloader.downloader.BinaryInstaller
import com.localdownloader.downloader.CommandResult
import com.localdownloader.downloader.ProcessRunner
import com.localdownloader.utils.Logger
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FfmpegExecutor @Inject constructor(
    private val binaryInstaller: BinaryInstaller,
    private val processRunner: ProcessRunner,
    private val logger: Logger,
) {
    suspend fun execute(
        args: List<String>,
        onStdoutLine: ((String) -> Unit)? = null,
        onStderrLine: ((String) -> Unit)? = null,
    ): CommandResult {
        return runCommand(
            args = args,
            preferNative = true,
            onStdoutLine = onStdoutLine,
            onStderrLine = onStderrLine,
        )
    }

    private suspend fun runCommand(
        args: List<String>,
        preferNative: Boolean,
        onStdoutLine: ((String) -> Unit)?,
        onStderrLine: ((String) -> Unit)?,
    ): CommandResult {
        val binary = binaryInstaller.ensureFfmpegBinary(preferNative = preferNative)
        val command = listOf(binary.absolutePath) + args
        logger.d(
            "FfmpegExecutor",
            "Executing ${if (preferNative) "native" else "asset"} ffmpeg: ${command.joinToString(" ")}",
        )

        return try {
            processRunner.runCommand(
                command = command,
                onStdoutLine = onStdoutLine,
                onStderrLine = onStderrLine,
            )
        } catch (error: IOException) {
            if (!preferNative || !shouldRetryWithAssetBinary(error)) throw error
            logger.w(
                "FfmpegExecutor",
                "Native-library ffmpeg launch failed; retrying with asset-installed binary",
                error,
            )
            runCommand(
                args = args,
                preferNative = false,
                onStdoutLine = onStdoutLine,
                onStderrLine = onStderrLine,
            )
        }
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
}
