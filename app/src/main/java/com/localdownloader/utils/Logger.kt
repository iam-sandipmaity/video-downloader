package com.localdownloader.utils

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Logger @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private val fileScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fileMutex = Mutex()

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        appendAsync(level = "D", tag = tag, message = message)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        appendAsync(level = "I", tag = tag, message = message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        appendAsync(level = "W", tag = tag, message = message, throwable = throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        appendAsync(level = "E", tag = tag, message = message, throwable = throwable)
    }

    fun logFilePath(): String = currentLogFile().absolutePath
    fun crashLogFilePath(): String = currentCrashLogFile().absolutePath
    fun externalLogFilePathOrNull(): String? = externalLogFileOrNull()?.absolutePath
    fun externalCrashLogFilePathOrNull(): String? = externalCrashLogFileOrNull()?.absolutePath

    fun ensureLogFilesExist() {
        runCatching {
            listOfNotNull(
                currentLogFile(),
                currentCrashLogFile(),
                externalLogFileOrNull(),
                externalCrashLogFileOrNull(),
            ).forEach { file ->
                file.parentFile?.mkdirs()
                if (!file.exists()) {
                    file.createNewFile()
                }
            }
        }.onFailure { error ->
            Log.e("Logger", "Failed creating log files", error)
        }
    }

    private fun appendAsync(
        level: String,
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        fileScope.launch {
            fileMutex.withLock {
                runCatching {
                    val stackTrace = throwable?.let(::buildStackTraceText)
                    buildTargetLogFiles(level = level).forEach { logFile ->
                        rotateIfNeeded(logFile)
                        appendLine(logFile, level, tag, message)
                        if (stackTrace != null) {
                            logFile.appendText(stackTrace)
                            if (!stackTrace.endsWith("\n")) {
                                logFile.appendText("\n")
                            }
                        }
                    }
                }.onFailure { error ->
                    Log.e("Logger", "Failed writing log file", error)
                }
            }
        }
    }

    private fun appendLine(logFile: File, level: String, tag: String, message: String) {
        val timestamp = LocalDateTime.now().format(formatter)
        val line = buildString {
            append(timestamp)
            append(" ")
            append(level)
            append("/")
            append(tag)
            append(" [")
            append(Thread.currentThread().name)
            append("] ")
            append(message)
            append("\n")
        }
        logFile.parentFile?.mkdirs()
        logFile.appendText(line)
    }

    private fun buildStackTraceText(throwable: Throwable): String {
        return StringWriter().use { writer ->
            PrintWriter(writer).use { printWriter ->
                throwable.printStackTrace(printWriter)
            }
            writer.toString()
        }
    }

    private fun buildTargetLogFiles(level: String): List<File> {
        val targets = mutableListOf(currentLogFile())
        externalLogFileOrNull()?.let(targets::add)
        if (level == "W" || level == "E") {
            targets += currentCrashLogFile()
            externalCrashLogFileOrNull()?.let(targets::add)
        }
        return targets
    }

    private fun rotateIfNeeded(logFile: File) {
        if (!logFile.exists() || logFile.length() < MAX_LOG_FILE_SIZE_BYTES) return
        val backup = File(logFile.parentFile, "${logFile.name}.1")
        if (backup.exists()) {
            backup.delete()
        }
        logFile.renameTo(backup)
    }

    private fun currentLogFile(): File {
        return File(File(context.filesDir, LOG_DIR_NAME), LOG_FILE_NAME)
    }

    private fun currentCrashLogFile(): File {
        return File(File(context.filesDir, LOG_DIR_NAME), CRASH_LOG_FILE_NAME)
    }

    private fun externalLogFileOrNull(): File? {
        val externalDir = context.getExternalFilesDir(LOG_DIR_NAME) ?: return null
        return File(externalDir, LOG_FILE_NAME)
    }

    private fun externalCrashLogFileOrNull(): File? {
        val externalDir = context.getExternalFilesDir(LOG_DIR_NAME) ?: return null
        return File(externalDir, CRASH_LOG_FILE_NAME)
    }

    private companion object {
        private const val LOG_DIR_NAME = "logs"
        private const val LOG_FILE_NAME = "app.log"
        private const val CRASH_LOG_FILE_NAME = "crash.log"
        private const val MAX_LOG_FILE_SIZE_BYTES = 5L * 1024L * 1024L
    }
}
