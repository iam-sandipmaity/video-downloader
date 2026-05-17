package com.localdownloader.downloader

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/** Maximum number of recent lines to keep for stderr error reporting. */
private const val MAX_RECENT_LINES = 500

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
        // Use a circular buffer approach: keep only recent stderr lines to avoid OOM.
        val stderrLines = mutableListOf<String>()
        var stderrTotalSize = 0L
        // Cap stderr buffer to ~1MB of text.
        val maxStderrBytes = 1024L * 1024L

        try {
            coroutineScope {
                val stdoutJob = async {
                    drainStream(
                        stream = process.inputStream,
                        process = process,
                    ) { line ->
                        stdoutBuilder.appendLine(line)
                        onStdoutLine?.invoke(line)
                    }
                }
                val stderrJob = async {
                    drainStream(
                        stream = process.errorStream,
                        process = process,
                    ) { line ->
                        val lineBytes = line.length.toLong() * 2 // rough UTF-16 estimate
                        stderrTotalSize += lineBytes
                        stderrLines.add(line)
                        // Evict oldest lines when buffer gets too large.
                        while (stderrTotalSize > maxStderrBytes && stderrLines.isNotEmpty()) {
                            stderrTotalSize -= stderrLines.removeFirst().length.toLong() * 2
                        }
                        onStderrLine?.invoke(line)
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
                    stderr = stderrLines.joinToString("\n"),
                )
            }
        } catch (e: CancellationException) {
            if (process.isAlive) process.destroyForcibly()
            throw e
        }
    }

    private suspend fun drainStream(
        stream: InputStream,
        process: Process,
        onLine: (String) -> Unit,
    ) {
        try {
            stream.bufferedReader().use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    onLine(line)
                }
            }
        } catch (error: Throwable) {
            if (!shouldIgnoreInterruptedRead(process, error)) throw error
        }
    }

    private suspend fun shouldIgnoreInterruptedRead(process: Process, error: Throwable): Boolean {
        if (error is CancellationException) return true
        if (!error.matchesClosedStreamFailure()) return false
        if (!currentCoroutineContext().isActive || !process.isAlive) return true
        repeat(20) {
            delay(50)
            if (!process.isAlive) return true
        }
        // The embedded runtime can close its pipes slightly before the process fully exits.
        // Preserve the exit code and collected stderr instead of surfacing this internal read race.
        return true
    }

    private fun Throwable.matchesClosedStreamFailure(): Boolean {
        generateSequence(this) { it.cause }.forEach { throwable ->
            val detail = throwable.message.orEmpty().lowercase()
            if (
                detail.contains("interrupted by close") ||
                detail.contains("stream closed") ||
                detail.contains("i/o operation on closed file") ||
                detail.contains("operation on closed file") ||
                detail == "closed"
            ) {
                return true
            }
        }
        return false
    }
}
