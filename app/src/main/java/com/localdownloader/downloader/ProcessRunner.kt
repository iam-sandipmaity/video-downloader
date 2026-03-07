package com.localdownloader.downloader

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
                    process.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            stdoutBuilder.appendLine(line)
                            onStdoutLine?.invoke(line)
                        }
                    }
                }
                val stderrJob = async {
                    process.errorStream.bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            stderrBuilder.appendLine(line)
                            onStderrLine?.invoke(line)
                        }
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
}
