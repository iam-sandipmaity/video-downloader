package com.localdownloader.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.BaseColumns
import android.provider.MediaStore
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
        val sanitized = SensitiveDataSanitizer.sanitize(message)
        Log.d(tag, sanitized)
        appendAsync(level = "D", tag = tag, message = sanitized)
    }

    fun i(tag: String, message: String) {
        val sanitized = SensitiveDataSanitizer.sanitize(message)
        Log.i(tag, sanitized)
        appendAsync(level = "I", tag = tag, message = sanitized)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        val sanitized = SensitiveDataSanitizer.sanitize(message)
        Log.w(tag, sanitized, throwable)
        appendAsync(level = "W", tag = tag, message = sanitized, throwable = throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val sanitized = SensitiveDataSanitizer.sanitize(message)
        Log.e(tag, sanitized, throwable)
        appendAsync(level = "E", tag = tag, message = sanitized, throwable = throwable)
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
        return publicLogBackupDir().absolutePath
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
            val backupFileName = "app-log-backup-${timestampNowForArchive()}.txt"
            val backupFile = writeDeviceBackupTextLocked(
                fileName = backupFileName,
                content = buildCombinedLogSnapshot(),
            )
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
                .lineSequence()
                .joinToString("\n") { SensitiveDataSanitizer.sanitize(it) }
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val backupItems = queryPublicLogBackupItems()
                .sortedByDescending { it.lastModifiedMs }
            backupItems.drop(MAX_DEVICE_BACKUP_FILES).forEach { item ->
                runCatching {
                    context.contentResolver.delete(item.uri, null, null)
                }
            }
            return
        }

        val backupDir = publicLogBackupDir()
        val backupFiles = backupDir.listFiles()
            ?.filter { it.isFile && isManagedDeviceBackupFileName(it.name) }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()
        backupFiles.drop(MAX_DEVICE_BACKUP_FILES).forEach { file ->
            runCatching { file.delete() }
        }
    }

    private fun copyArchiveToDeviceLocked(archivedLog: File) {
        writeDeviceBackupFileLocked(archivedLog)
    }

    private fun publicLogBackupDir(): File {
        val publicDownloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val rootSegments = latestSettings.downloadsRootFolderName
            .trim()
            .split('/', '\\')
            .filter { it.isNotBlank() }
        return rootSegments.fold(publicDownloadsDir) { current, segment ->
            File(current, segment)
        }.resolve("Logs")
    }

    private fun publicLogBackupRelativePath(): String {
        val rootSegments = latestSettings.downloadsRootFolderName
            .trim()
            .split('/', '\\')
            .filter { it.isNotBlank() }
        return buildString {
            append("Download/")
            rootSegments.forEach { segment ->
                append(segment)
                append('/')
            }
            append("Logs/")
        }
    }

    private fun writeDeviceBackupTextLocked(
        fileName: String,
        content: String,
    ): File {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return writePublicDownloadEntryLocked(
                fileName = fileName,
                mimeType = "text/plain",
                writeAction = { output -> output.bufferedWriter().use { it.write(content) } },
            )
        }

        val backupDir = publicLogBackupDir()
        backupDir.mkdirs()
        val targetFile = File(backupDir, resolveUniqueBackupFileName(backupDir, fileName))
        targetFile.writeText(content)
        return targetFile
    }

    private fun writeDeviceBackupFileLocked(sourceFile: File): File {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return writePublicDownloadEntryLocked(
                fileName = sourceFile.name,
                mimeType = "text/plain",
                writeAction = { output -> sourceFile.inputStream().use { input -> input.copyTo(output) } },
            )
        }

        val backupDir = publicLogBackupDir()
        backupDir.mkdirs()
        val targetFile = File(backupDir, resolveUniqueBackupFileName(backupDir, sourceFile.name))
        sourceFile.copyTo(targetFile, overwrite = false)
        return targetFile
    }

    private fun writePublicDownloadEntryLocked(
        fileName: String,
        mimeType: String,
        writeAction: (java.io.OutputStream) -> Unit,
    ): File {
        val relativePath = publicLogBackupRelativePath()
        val publicDir = publicLogBackupDir()
        val displayName = resolveUniquePublicBackupFileName(fileName)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(
            externalDownloadsCollectionUri(),
            values,
        ) ?: throw IllegalStateException("Device log backup folder is unavailable on this device.")
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use(writeAction)
                ?: error("Unable to open device log backup output stream.")
            ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }.also { finalized ->
                context.contentResolver.update(uri, finalized, null, null)
            }
        }.getOrElse { error ->
            runCatching { context.contentResolver.delete(uri, null, null) }
            throw error
        }
        return File(publicDir, displayName)
    }

    private fun resolveUniquePublicBackupFileName(originalName: String): String {
        val existingNames = queryPublicLogBackupItems()
            .map { it.displayName }
            .toSet()
        if (originalName !in existingNames) return originalName

        val nameWithoutExt = originalName.substringBeforeLast('.', originalName)
        val extension = originalName.substringAfterLast('.', "")
        var counter = 2
        while (true) {
            val candidate = if (extension.isBlank()) {
                "$nameWithoutExt ($counter)"
            } else {
                "$nameWithoutExt ($counter).$extension"
            }
            if (candidate !in existingNames) return candidate
            counter += 1
        }
    }

    private fun resolveUniqueBackupFileName(
        parentDir: File,
        originalName: String,
    ): String {
        if (!File(parentDir, originalName).exists()) return originalName
        val nameWithoutExt = originalName.substringBeforeLast('.', originalName)
        val extension = originalName.substringAfterLast('.', "")
        var counter = 2
        while (true) {
            val candidate = if (extension.isBlank()) {
                "$nameWithoutExt ($counter)"
            } else {
                "$nameWithoutExt ($counter).$extension"
            }
            if (!File(parentDir, candidate).exists()) return candidate
            counter += 1
        }
    }

    private fun queryPublicLogBackupItems(): List<PublicLogBackupItem> {
        val relativePath = publicLogBackupRelativePath()
        val projection = arrayOf(
            BaseColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_MODIFIED,
        )
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH}=?"
        val selectionArgs = arrayOf(relativePath)
        return context.contentResolver.query(
            externalDownloadsCollectionUri(),
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(BaseColumns._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val modifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            buildList {
                while (cursor.moveToNext()) {
                    val displayName = cursor.getString(nameIndex)
                    if (!isManagedDeviceBackupFileName(displayName)) continue
                    add(
                        PublicLogBackupItem(
                            uri = android.content.ContentUris.withAppendedId(
                                externalDownloadsCollectionUri(),
                                cursor.getLong(idIndex),
                            ),
                            displayName = displayName,
                            lastModifiedMs = cursor.getLong(modifiedIndex) * 1000L,
                        ),
                    )
                }
            }
        }.orEmpty()
    }

    private fun isManagedDeviceBackupFileName(fileName: String): Boolean {
        return (fileName.startsWith("app-log-backup-") && fileName.endsWith(".txt")) ||
            (fileName.startsWith("app-") && fileName.endsWith(".log")) ||
            (fileName.startsWith("crash-") && fileName.endsWith(".log"))
    }

    private fun externalDownloadsCollectionUri(): Uri = Uri.parse(EXTERNAL_DOWNLOADS_CONTENT_URI)

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
        private const val EXTERNAL_DOWNLOADS_CONTENT_URI = "content://media/external/downloads"
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

private data class PublicLogBackupItem(
    val uri: android.net.Uri,
    val displayName: String,
    val lastModifiedMs: Long,
)
