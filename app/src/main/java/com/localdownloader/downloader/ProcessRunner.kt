package com.localdownloader.downloader

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProcessRunner @Inject constructor() {

    suspend fun runCommand(
        command: List<String>,
        workingDirectory: File? = null,
        environment: Map<String, String> = emptyMap(),
        onStdoutLine: ((String) -> Unit)? = null,
        onStderrLine: ((String) -> Unit)? = null,
    ): CommandResult = withContext(Dispatchers.IO) {
        val processBuilder = ProcessBuilder(command)
        if (workingDirectory != null) {
            processBuilder.directory(workingDirectory)
        }
        if (environment.isNotEmpty()) {
            processBuilder.environment().putAll(environment)
        }

        val process = processBuilder.start()
        val stdoutBuilder = StringBuilder()
        val stderrBuilder = StringBuilder()

        try {
            coroutineScope {
                val stdoutJob = async {
                    try {
                        process.inputStream.bufferedReader().useLines { lines ->
                            lines.forEach { line ->
                                stdoutBuilder.appendLine(line)
                                onStdoutLine?.invoke(line)
                            }
                        }
                    } catch (error: Throwable) {
                        if (!shouldIgnoreInterruptedRead(process, error)) throw error
                    }
                }
                val stderrJob = async {
                    try {
                        process.errorStream.bufferedReader().useLines { lines ->
                            lines.forEach { line ->
                                stderrBuilder.appendLine(line)
                                onStderrLine?.invoke(line)
                            }
                        }
                    } catch (error: Throwable) {
                        if (!shouldIgnoreInterruptedRead(process, error)) throw error
                    }
                }

                val exitCode = try {
                    runInterruptible { process.waitFor() }
                } catch (e: CancellationException) {
                    process.destroyForcibly()
                    throw e
                }
                stdoutJob.await()
                stderrJob.await()

                CommandResult(
                    exitCode = exitCode,
                    stdout = stdoutBuilder.toString(),
                    stderr = stderrBuilder.toString(),
                )
            }
        } catch (e: CancellationException) {
            if (process.isAlive) process.destroyForcibly()
            throw e
        }
    }

    private suspend fun shouldIgnoreInterruptedRead(process: Process, error: Throwable): Boolean {
        if (error is CancellationException) return true
        val detail = error.message.orEmpty().lowercase()
        val looksLikeClosedStream = detail.contains("interrupted by close") ||
            detail.contains("stream closed") ||
            detail.contains("closed")
        return looksLikeClosedStream && (!currentCoroutineContext().isActive || !process.isAlive)
    }
}
