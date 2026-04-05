package com.localdownloader.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
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

    fun createOutputTemplateWithDirectory(template: String): String {
        val outputDir = ensureDownloadsDir().absolutePath
        return "$outputDir/$template"
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
    fun copyToPublicDownloads(sourceFile: File): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null

        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val publicDir = File(publicDownloads, "LocalDownloader")
        if (!publicDir.exists()) publicDir.mkdirs()

        val destFile = File(publicDir, sourceFile.name)
        if (destFile.exists()) {
            var i = 1
            val nameWithoutExt = sourceFile.nameWithoutExtension
            val ext = sourceFile.extension
            do {
                destFile.parentFile?.let { parent ->
                    // destFile = File(parent, "${nameWithoutExt}($i).$ext")
                }
            } while (File(publicDir, "${nameWithoutExt}($i).$ext").exists())
            return null // Let the worker handle naming; don't overwrite
        }

        return try {
            val values = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, sourceFile.name)
                put(MediaStore.MediaColumns.MIME_TYPE, guessMimeType(sourceFile.name))
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/LocalDownloader/")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Files.getContentUri("external"),
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
            // Resolve the real path
            sourceFile.absolutePath
        } catch (e: Exception) {
            // Fallback: direct copy
            try {
                sourceFile.copyTo(destFile, overwrite = false)
                triggerMediaScan(Uri.fromFile(destFile))
                destFile.absolutePath
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun guessMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "mp4", "mkv", "webm", "mov" -> "video/$ext"
            "mp3" -> "audio/mpeg"
            "aac", "m4a" -> "audio/$ext"
            "opus", "ogg", "ogg" -> "audio/ogg"
            "wav" -> "audio/wav"
            "flac" -> "audio/flac"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "application/octet-stream"
        }
    }

    private fun triggerMediaScan(uri: Uri) {
        context.sendBroadcast(
            android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri),
        )
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
                dest.absolutePath
            } catch (_: Exception) {
                null
            }
        }

        private fun queryDisplayName(context: Context, uri: Uri): String? =
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
    }
}
