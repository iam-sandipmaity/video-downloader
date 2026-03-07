package com.localdownloader.ffmpeg

import com.localdownloader.downloader.BinaryInstaller
import com.localdownloader.downloader.CommandResult
import com.localdownloader.downloader.ProcessRunner
import com.localdownloader.utils.Logger
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
        val binary = binaryInstaller.ensureFfmpegBinary()
        val command = listOf(binary.absolutePath) + args
        logger.d("FfmpegExecutor", "Executing: ${command.joinToString(" ")}")
        return processRunner.runCommand(
            command = command,
            onStdoutLine = onStdoutLine,
            onStderrLine = onStderrLine,
        )
    }
}
