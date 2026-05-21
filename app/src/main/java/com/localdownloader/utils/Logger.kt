package com.localdownloader.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import com.localdownloader.data.SettingsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Logger @Inject constructor(
    @ApplicationContext private val context: Context,
    settingsStore: SettingsStore,
) {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private val archiveFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
    private val fileScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fileMutex = Mutex()

    @Volatile
    private var latestSettings = com.localdownloader.domain.models.AppSettings()

    init {
        settingsStore.observeSettings()
            .onEach { settings ->
                latestSettings = settings
                fileScope.launch {
                    fileMutex.withLock {
                        pruneLogFilesLocked()
                        pruneDeviceLogBackupsLocked()
                    }
                }
            }
            .launchIn(fileScope)
    }

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

    fun appLogFiles(): List<File> {
        val logsDir = File(context.filesDir, LOG_DIR_NAME)
        return logsDir.listFiles()
            ?.filter { file ->
                file.isFile && (
                    file.name == LOG_FILE_NAME ||
                        file.name == "$LOG_FILE_NAME.1" ||
                        (file.name.startsWith("app-") && file.name.endsWith(".log"))
                )
            }
            ?.sortedBy { it.lastModified() }
            .orEmpty()
    }

    fun deviceLogBackupDirPath(): String? {
        return externalLogBackupDir()?.absolutePath
    }

    fun ensureLogFilesExist() {
        runCatching {
            listOf(
                currentLogFile(),
                currentCrashLogFile(),
            ).forEach { file ->
                file.parentFile?.mkdirs()
                if (!file.exists()) {
                    file.createNewFile()
                }
            }
            fileScope.launch {
                fileMutex.withLock {
                    pruneLogFilesLocked()
                    pruneDeviceLogBackupsLocked()
                }
            }
        }.onFailure { error ->
            Log.e("Logger", "Failed creating log files", error)
        }
    }

    suspend fun backupLogsToDevice(): File {
        return fileMutex.withLock {
            ensureActiveFilesExistLocked()
            val backupDir = externalLogBackupDir()
                ?: throw IllegalStateException("Device log backup folder is unavailable on this device.")
            backupDir.mkdirs()
            val backupFile = File(
                backupDir,
                "app-log-backup-${timestampNowForArchive()}.txt",
            )
            backupFile.writeText(buildCombinedLogSnapshot())
            pruneDeviceLogBackupsLocked()
            backupFile
        }
    }

    suspend fun runMaintenance() {
        fileMutex.withLock {
            pruneLogFilesLocked()
            pruneDeviceLogBackupsLocked()
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
        if (level == "W" || level == "E") {
            targets += currentCrashLogFile()
        }
        return targets
    }

    private fun rotateIfNeeded(logFile: File) {
        if (!logFile.exists() || logFile.length() < MAX_LOG_FILE_SIZE_BYTES) return

        val archivedLog = createArchivedLogFile(logFile)
        val rotated = logFile.renameTo(archivedLog)
        if (!rotated) {
            logFile.copyTo(archivedLog, overwrite = true)
            logFile.writeText("")
        }

        if (!logFile.exists()) {
            logFile.parentFile?.mkdirs()
            logFile.createNewFile()
        }

        if (latestSettings.backupLogsToDevice) {
            runCatching { copyArchiveToDeviceLocked(archivedLog) }
                .onFailure { error -> Log.e("Logger", "Failed backing up rotated log", error) }
        }

        pruneLogFilesLocked()
        pruneDeviceLogBackupsLocked()
    }

    private fun buildCombinedLogSnapshot(): String {
        val sections = mutableListOf<String>()

        val appSections = appLogFiles().mapNotNull { file ->
            val content = runCatching { file.readText() }.getOrNull()?.trimEnd()
            content?.takeIf { it.isNotBlank() }?.let { text ->
                "===== ${file.name} =====\n$text"
            }
        }
        sections += appSections

        crashLogFamilyFiles().mapNotNull { file ->
            val content = runCatching { file.readText() }.getOrNull()?.trimEnd()
            content?.takeIf { it.isNotBlank() }?.let { text ->
                "===== ${file.name} =====\n$text"
            }
        }.forEach(sections::add)

        if (sections.isEmpty()) {
            return "No readable app logs were available at ${Instant.now()}."
        }

        return buildString {
            append("Generated ")
            append(
                Instant.now()
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss")),
            )
            append("\n\n")
            append(sections.joinToString("\n\n"))
            append("\n")
        }
    }

    private fun pruneLogFilesLocked() {
        if (!latestSettings.autoDeleteOldAppLogs) return

        val retentionDays = latestSettings.appLogRetentionDays.coerceIn(MIN_RETENTION_DAYS, MAX_RETENTION_DAYS)
        val cutoffMs = System.currentTimeMillis() - retentionDays * DAY_IN_MILLIS
        val archivedFiles = archivedAppLogFiles() + archivedCrashLogFiles()
        archivedFiles
            .filter { it.lastModified() in 1 until cutoffMs }
            .forEach { file ->
                runCatching { file.delete() }
            }
    }

    private fun pruneDeviceLogBackupsLocked() {
        val backupDir = externalLogBackupDir() ?: return
        val backupFiles = backupDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("app-log-backup-") && it.name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()
        backupFiles.drop(MAX_DEVICE_BACKUP_FILES).forEach { file ->
            runCatching { file.delete() }
        }
    }

    private fun copyArchiveToDeviceLocked(archivedLog: File) {
        val backupDir = externalLogBackupDir() ?: return
        backupDir.mkdirs()
        archivedLog.copyTo(File(backupDir, archivedLog.name), overwrite = true)
    }

    private fun externalLogBackupDir(): File? {
        val externalDownloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return null
        val rootSegments = latestSettings.downloadsRootFolderName
            .trim()
            .split('/', '\\')
            .filter { it.isNotBlank() }
        return rootSegments.fold(externalDownloadsDir) { current, segment ->
            File(current, segment)
        }.resolve("Logs")
    }

    private fun crashLogFamilyFiles(): List<File> {
        val logsDir = File(context.filesDir, LOG_DIR_NAME)
        return logsDir.listFiles()
            ?.filter { file ->
                file.isFile && (
                    file.name == CRASH_LOG_FILE_NAME ||
                        file.name == "$CRASH_LOG_FILE_NAME.1" ||
                        (file.name.startsWith("crash-") && file.name.endsWith(".log"))
                )
            }
            ?.sortedBy { it.lastModified() }
            .orEmpty()
    }

    private fun archivedAppLogFiles(): List<File> {
        return appLogFiles().filter { it.name != LOG_FILE_NAME }
    }

    private fun archivedCrashLogFiles(): List<File> {
        return crashLogFamilyFiles().filter { it.name != CRASH_LOG_FILE_NAME }
    }

    private fun createArchivedLogFile(logFile: File): File {
        val prefix = when (logFile.name) {
            LOG_FILE_NAME -> "app"
            CRASH_LOG_FILE_NAME -> "crash"
            else -> logFile.nameWithoutExtension
        }
        return File(logFile.parentFile, "$prefix-${timestampNowForArchive()}.log")
    }

    private fun timestampNowForArchive(): String {
        return LocalDateTime.now().format(archiveFormatter)
    }

    private fun ensureActiveFilesExistLocked() {
        listOf(currentLogFile(), currentCrashLogFile()).forEach { file ->
            file.parentFile?.mkdirs()
            if (!file.exists()) {
                file.createNewFile()
            }
        }
    }

    private fun currentLogFile(): File {
        return File(File(context.filesDir, LOG_DIR_NAME), LOG_FILE_NAME)
    }

    private fun currentCrashLogFile(): File {
        return File(File(context.filesDir, LOG_DIR_NAME), CRASH_LOG_FILE_NAME)
    }

    private companion object {
        private const val LOG_DIR_NAME = "logs"
        private const val LOG_FILE_NAME = "app.log"
        private const val CRASH_LOG_FILE_NAME = "crash.log"
        private const val MAX_LOG_FILE_SIZE_BYTES = 5L * 1024L * 1024L
        private const val MAX_DEVICE_BACKUP_FILES = 20
        private const val MIN_RETENTION_DAYS = 3
        private const val MAX_RETENTION_DAYS = 90
        private const val DAY_IN_MILLIS = 24L * 60L * 60L * 1000L
    }
}
