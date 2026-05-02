package com.localdownloader.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.localdownloader.ui.model.ExternalOpenRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileUtils @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val conversionDirName = "converted"
    private val binDirName = "bin"

    /**
     * Returns the downloads directory for yt-dlp output.
     *
     * Android 11+ (API 30+): app-specific external storage (`Android/data/…`).
     * On download completion the Worker copies the file to the public Downloads folder
     * so users can find it in their file manager.
     *
     * Android 10 and below: `/sdcard/Download/LocalDownloader/`
     */
    fun ensureDownloadsDir(): File {
        // Android 10 and below: direct path to public Downloads.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val appDir = File(publicDownloads, "LocalDownloader")
            if ((appDir.exists() || appDir.mkdirs()) && appDir.canWrite()) return appDir
        }

        // Android 11+: app-specific external storage.
        val extDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (extDir != null && (extDir.exists() || extDir.mkdirs()) && extDir.canWrite()) return extDir

        return ensureInternalDir("downloads")
    }

    fun ensureConversionDir(): File = ensureInternalDir(conversionDirName)

    fun ensureBinDir(): File = ensureInternalDir(binDirName)

    fun importDocumentToInternalFile(
        uri: Uri,
        subDirectoryName: String,
        targetFileName: String,
    ): String {
        val targetDir = ensureInternalDir(subDirectoryName)
        val targetFile = File(targetDir, sanitizeFileName(targetFileName))
        context.contentResolver.openInputStream(uri)?.use { input ->
            targetFile.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Unable to open selected file")
        return targetFile.absolutePath
    }

    fun writeTextToInternalFile(
        subDirectoryName: String,
        targetFileName: String,
        content: String,
    ): String {
        val targetDir = ensureInternalDir(subDirectoryName)
        val targetFile = File(targetDir, sanitizeFileName(targetFileName))
        targetFile.writeText(content)
        return targetFile.absolutePath
    }

    fun readTextFromUri(uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalStateException("Unable to read selected file")
    }

    fun createOutputTemplateWithDirectory(template: String): String {
        val outputDir = ensureDownloadsDir().absolutePath
        return "$outputDir/$template"
    }

    fun createUniquePlaylistDirectory(playlistName: String): File {
        val root = ensureDownloadsDir()
        val baseName = sanitizeFileName(playlistName)
        var candidate = File(root, baseName)
        var counter = 2
        while (candidate.exists()) {
            candidate = File(root, "$baseName ($counter)")
            counter += 1
        }
        if (!candidate.mkdirs() && !candidate.isDirectory) {
            throw IllegalStateException("Storage denied: unable to create playlist directory ${candidate.absolutePath}")
        }
        return candidate
    }

    fun buildPlaylistItemOutputTemplate(
        playlistDirectory: File,
        baseTemplate: String,
        playlistItemIndex: Int,
    ): String {
        val fileTemplate = baseTemplate.substringAfterLast('/').substringAfterLast('\\')
        val prefixedFileTemplate = "v$playlistItemIndex - $fileTemplate"
        return File(playlistDirectory, prefixedFileTemplate).absolutePath
    }

    fun appendCounterToTemplate(template: String, n: Int): String {
        if (template.endsWith(".%(ext)s")) {
            return "${template.removeSuffix(".%(ext)s")}($n).%(ext)s"
        }
        val lastDot = template.lastIndexOf('.')
        return if (lastDot >= 0) {
            "${template.substring(0, lastDot)}($n)${template.substring(lastDot)}"
        } else {
            "$template($n)"
        }
    }

    fun buildConvertedOutputPath(baseName: String, ext: String): String {
        val sanitizedName = sanitizeFileName(baseName)
        val fileName = "$sanitizedName.$ext"
        return File(ensureConversionDir(), fileName).absolutePath
    }

    fun deleteDownloadArtifacts(outputTemplate: String): Int {
        val templateFile = File(outputTemplate)
        val parentDir = templateFile.parentFile ?: return 0
        if (!parentDir.exists()) return 0

        val templateName = templateFile.name
        val stem = templateName.substringBefore(".%(ext)s", templateName)
        val deletedFiles = parentDir.listFiles()
            ?.filter { candidate ->
                candidate.isFile && (
                    candidate.name.startsWith(stem) ||
                        candidate.name.startsWith("$stem.")
                    )
            }
            .orEmpty()
            .count { candidate -> deleteManagedFile(candidate.absolutePath) }

        return deletedFiles
    }

    fun sanitizeFileName(value: String): String {
        return value
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .take(160)
            .ifBlank { "media_${System.currentTimeMillis()}" }
    }

    /**
     * On Android 11+ copies a file from the app-specific directory to the public
     * Downloads folder via MediaStore so it becomes visible in file managers.
     * Returns the public path, or null on Android 10 and below (file is already public).
     */
    fun copyToPublicDownloads(
        sourceFile: File,
        playlistFolderName: String? = null,
        targetFileName: String? = null,
    ): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null

        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val relativeParent = playlistFolderName
            ?.trim()
            ?.trim('/', '\\')
            ?.ifBlank { null }
            ?: relativePathWithinDownloadsRoot(sourceFile)
                ?.substringBeforeLast('/', "")
                .orEmpty()
        val publicDir = if (relativeParent.isBlank()) {
            File(publicDownloads, "LocalDownloader")
        } else {
            File(publicDownloads, "LocalDownloader/$relativeParent")
        }
        if (!publicDir.exists()) publicDir.mkdirs()

        val displayName = targetFileName
            ?.takeIf { it.isNotBlank() }
            ?: resolveUniqueFileName(publicDir, sourceFile.name)
        val destFile = File(publicDir, displayName)

        return try {
            val values = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, guessMimeType(sourceFile.name))
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    buildString {
                        append("Download/LocalDownloader/")
                        if (relativeParent.isNotBlank()) {
                            append(relativeParent.trim('/'))
                            append('/')
                        }
                    },
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values,
            ) ?: return null
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            destFile.absolutePath
        } catch (e: Exception) {
            try {
                sourceFile.copyTo(destFile, overwrite = false)
                triggerMediaScan(Uri.fromFile(destFile))
                destFile.absolutePath
            } catch (_: Exception) {
                null
            }
        }
    }

    fun resolveManagedMediaBundle(path: String): List<File> {
        val primaryFile = File(path)
        return resolveManagedMediaBundle(primaryFile)
    }

    fun deleteManagedMediaBundle(path: String): Boolean {
        val bundleFiles = resolveManagedMediaBundle(path)
        if (bundleFiles.isEmpty()) return true
        return bundleFiles.all { candidate -> deleteManagedFile(candidate.absolutePath) }
    }

    fun deleteManagedFile(path: String): Boolean {
        val targetFile = File(path)
        if (!targetFile.exists()) return true
        if (targetFile.delete()) return true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            deleteFromMediaStore(targetFile)?.let { deleted ->
                if (deleted) return true
            }
        }
        return false
    }

    fun renameManagedFile(path: String, targetFileName: String): String? {
        val sourceFile = File(path)
        if (!sourceFile.exists()) return null

        val targetFile = File(sourceFile.parentFile, targetFileName)
        if (sourceFile.absolutePath == targetFile.absolutePath) return sourceFile.absolutePath
        if (targetFile.exists()) return null
        if (sourceFile.renameTo(targetFile)) return targetFile.absolutePath

        return runCatching {
            sourceFile.copyTo(targetFile, overwrite = false)
            if (!deleteManagedFile(sourceFile.absolutePath)) {
                targetFile.delete()
                null
            } else {
                triggerMediaScan(Uri.fromFile(targetFile))
                targetFile.absolutePath
            }
        }.getOrNull()
    }

    fun renameManagedMediaBundle(path: String, targetFileName: String): String? {
        val sourceFile = File(path)
        if (!sourceFile.exists()) return null

        val targetPrimary = File(sourceFile.parentFile, targetFileName)
        if (sourceFile.absolutePath == targetPrimary.absolutePath) return sourceFile.absolutePath

        val bundleFiles = resolveManagedMediaBundle(sourceFile)
        if (bundleFiles.isEmpty()) return null

        val sourceStem = sourceFile.nameWithoutExtension
        val targetStem = targetPrimary.nameWithoutExtension
        val sourcePaths = bundleFiles.map { it.absolutePath }.toSet()
        val renamePlan = bundleFiles.associateWith { currentFile ->
            if (currentFile.absolutePath == sourceFile.absolutePath) {
                targetPrimary
            } else {
                val suffix = currentFile.name.removePrefix(sourceStem)
                File(sourceFile.parentFile, "$targetStem$suffix")
            }
        }

        if (renamePlan.values.any { target ->
                target.exists() && target.absolutePath !in sourcePaths
            }
        ) {
            return null
        }

        val movedPairs = mutableListOf<Pair<File, File>>()
        renamePlan.forEach { (from, to) ->
            if (from.absolutePath == to.absolutePath) return@forEach
            if (!moveManagedFile(from, to)) {
                movedPairs.asReversed().forEach { (movedTo, originalPath) ->
                    moveManagedFile(movedTo, originalPath)
                }
                return null
            }
            movedPairs += to to from
        }
        return targetPrimary.absolutePath
    }

    fun normalizeLibraryOutputPath(path: String): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return path

        val relativePath = relativePathWithinDownloadsRoot(File(path)) ?: return path
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val publicFile = File(publicDownloads, "LocalDownloader/$relativePath")
        return if (publicFile.exists()) publicFile.absolutePath else path
    }

    private fun relativePathWithinDownloadsRoot(sourceFile: File): String? {
        val downloadsRoot = ensureDownloadsDir().absoluteFile.normalize().path
        val sourcePath = sourceFile.absoluteFile.normalize().path
        if (!sourcePath.startsWith(downloadsRoot)) return null
        return sourcePath
            .removePrefix(downloadsRoot)
            .trimStart(File.separatorChar)
            .replace(File.separatorChar, '/')
            .ifBlank { null }
    }

    private fun resolveUniqueFileName(parentDir: File, originalName: String): String {
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

    private fun guessMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4", "mkv", "webm", "mov" -> "video/$ext"
            "mp3" -> "audio/mpeg"
            "aac", "m4a" -> "audio/$ext"
            "opus", "ogg" -> "audio/ogg"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "srt" -> "application/x-subrip"
            "vtt", "webvtt" -> "text/vtt"
            "ass", "ssa" -> "text/x-ssa"
            "ttml", "dfxp", "xml" -> "application/ttml+xml"
            else -> "application/octet-stream"
        }
    }

    private fun resolveManagedMediaBundle(primaryFile: File): List<File> {
        val parentDir = primaryFile.parentFile ?: return listOf(primaryFile).filter { it.exists() }
        val stem = primaryFile.nameWithoutExtension
        val sidecars = parentDir.listFiles()
            ?.filter { candidate ->
                candidate.isFile &&
                    candidate.name != primaryFile.name &&
                    candidate.name.startsWith("$stem.") &&
                    !isTemporaryDownloadArtifact(candidate.name)
            }
            ?.sortedBy { it.name }
            .orEmpty()
        return buildList {
            if (primaryFile.exists()) add(primaryFile)
            addAll(sidecars)
        }
    }

    private fun isTemporaryDownloadArtifact(fileName: String): Boolean {
        val lower = fileName.lowercase()
        return lower.endsWith(".part") ||
            lower.endsWith(".ytdl") ||
            lower.endsWith(".temp")
    }

    private fun triggerMediaScan(uri: Uri) {
        context.sendBroadcast(
            android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri),
        )
    }

    private fun moveManagedFile(sourceFile: File, targetFile: File): Boolean {
        if (sourceFile.absolutePath == targetFile.absolutePath) return true
        if (targetFile.exists()) return false
        if (sourceFile.renameTo(targetFile)) {
            triggerMediaScan(Uri.fromFile(targetFile))
            return true
        }

        return runCatching {
            sourceFile.copyTo(targetFile, overwrite = false)
            if (!deleteManagedFile(sourceFile.absolutePath)) {
                targetFile.delete()
                false
            } else {
                triggerMediaScan(Uri.fromFile(targetFile))
                true
            }
        }.getOrDefault(false)
    }

    private fun deleteFromMediaStore(file: File): Boolean? {
        val downloadsRoot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            .absoluteFile
            .normalize()
        val normalizedFile = file.absoluteFile.normalize()
        val rootPath = downloadsRoot.path
        val filePath = normalizedFile.path
        if (!filePath.startsWith(rootPath)) return null

        val relativePath = file.parentFile
            ?.absoluteFile
            ?.normalize()
            ?.path
            ?.removePrefix(rootPath)
            ?.trimStart(File.separatorChar)
            ?.replace(File.separatorChar, '/')
            .orEmpty()
        val mediaStoreRelativePath = buildString {
            append("Download/")
            if (relativePath.isNotBlank()) {
                append(relativePath.trim('/'))
                append('/')
            }
        }

        val projection = arrayOf(android.provider.BaseColumns._ID)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?"
        val selectionArgs = arrayOf(file.name, mediaStoreRelativePath)
        val contentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val itemId = context.contentResolver.query(
            contentUri,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else null
        } ?: return false

        val itemUri = android.content.ContentUris.withAppendedId(contentUri, itemId)
        return context.contentResolver.delete(itemUri, null, null) > 0
    }

    /**
     * Clears the app's cache directory.
     * Returns the number of bytes freed.
     */
    fun clearCache(): Long {
        val cacheDir = context.cacheDir
        var freedBytes = 0L
        cacheDir.listFiles()?.forEach { file ->
            freedBytes += deleteRecursively(file)
        }
        return freedBytes
    }

    /**
     * Gets the current cache size in bytes.
     */
    fun getCacheSize(): Long {
        return calculateDirSize(context.cacheDir)
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateDirSize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    private fun deleteRecursively(file: File): Long {
        val size = if (file.isDirectory) {
            file.listFiles()?.sumOf { deleteRecursively(it) } ?: 0L
        } else {
            file.length()
        }
        return if (file.delete()) size else 0L
    }

    private fun ensureInternalDir(name: String): File {
        val dir = File(context.filesDir, name)
        if (!dir.exists() && !dir.mkdirs()) {
            throw IllegalStateException("Storage denied: unable to create directory ${dir.absolutePath}")
        }
        if (!dir.isDirectory) {
            throw IllegalStateException("Storage denied: path is not a directory ${dir.absolutePath}")
        }
        if (!dir.canWrite()) {
            throw IllegalStateException("Storage denied: directory is not writable ${dir.absolutePath}")
        }
        return dir
    }

    fun importSharedOpenRequest(uri: Uri, mimeTypeHint: String? = null): ExternalOpenRequest {
        val displayName = queryDisplayName(context, uri)
            ?: buildFallbackSharedName(uri = uri, mimeTypeHint = mimeTypeHint)
        val resolvedMimeType = mimeTypeHint
            ?: context.contentResolver.getType(uri)
            ?: displayName.substringAfterLast('.', "").lowercase().takeIf { it.isNotBlank() }
                ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }

        if (uri.scheme == "file") {
            val directPath = uri.path ?: error("Unable to open shared file path.")
            return ExternalOpenRequest(
                path = directPath,
                displayName = displayName,
                mimeType = resolvedMimeType,
            )
        }

        val targetDir = ensureInternalDir("opened")
        val sanitizedName = sanitizeFileName(displayName)
        val targetFile = File(targetDir, sanitizedName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            targetFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Unable to open shared file.")

        return ExternalOpenRequest(
            path = targetFile.absolutePath,
            displayName = targetFile.name,
            mimeType = resolvedMimeType,
        )
    }

    companion object {
        fun getRealPathFromUri(context: Context, uri: Uri): String? {
            if (uri.scheme == "file") return uri.path
            if (uri.scheme != "content") return null
            return try {
                val displayName = queryDisplayName(context, uri)
                    ?: "media_${System.currentTimeMillis()}"
                val dest = File(context.cacheDir, displayName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }
                if (dest.exists() && dest.length() > 0) {
                    dest.absolutePath
                } else {
                    // Fallback: return the URI string itself for SAF access
                    uri.toString()
                }
            } catch (e: Exception) {
                // Return the URI string as fallback for SAF access
                uri.toString()
            }
        }

        private fun queryDisplayName(context: Context, uri: Uri): String? =
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }

        private fun buildFallbackSharedName(uri: Uri, mimeTypeHint: String?): String {
            val extension = mimeTypeHint
                ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
                ?.takeIf { it.isNotBlank() }
                ?: uri.lastPathSegment?.substringAfterLast('.', "")
                    ?.takeIf { it.isNotBlank() }
            return buildString {
                append("shared_")
                append(System.currentTimeMillis())
                if (!extension.isNullOrBlank()) {
                    append('.')
                    append(extension)
                }
            }
        }
    }
}
