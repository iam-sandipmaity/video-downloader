package com.localdownloader.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
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
     * Returns a writable Downloads directory visible in the system file manager.
     * Primary: /sdcard/Download/LocalDownloader/
     * Fallback: app-specific external storage (Android/data/…), still browseable.
     * Last resort: app internal files dir (original behaviour, invisible to user).
     */
    fun ensureDownloadsDir(): File {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val appDir = File(publicDownloads, "LocalDownloader")
        if ((appDir.exists() || appDir.mkdirs()) && appDir.canWrite()) return appDir

        // External app-specific dir — no permission needed, visible in Files app.
        val extDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (extDir != null && (extDir.exists() || extDir.mkdirs()) && extDir.canWrite()) return extDir

        // Internal fallback.
        return ensureInternalDir("downloads")
    }

    fun ensureConversionDir(): File = ensureInternalDir(conversionDirName)

    fun ensureBinDir(): File = ensureInternalDir(binDirName)

    fun createOutputTemplateWithDirectory(template: String): String {
        val outputDir = ensureDownloadsDir().absolutePath
        return "$outputDir/$template"
    }

    /**
     * Inserts "(n)" before the file extension in a yt-dlp output template so that repeated
     * downloads of the same URL produce distinct filenames instead of overwriting each other.
     *
     * Examples:
     *   appendCounterToTemplate("%(title)s [%(id)s].%(ext)s", 1) → "%(title)s [%(id)s](1).%(ext)s"
     *   appendCounterToTemplate("audio.mp3", 2)                   → "audio(2).mp3"
     *   appendCounterToTemplate("noext", 1)                        → "noext(1)"
     */
    fun appendCounterToTemplate(template: String, n: Int): String {
        // yt-dlp variable extension
        if (template.endsWith(".%(ext)s")) {
            return "${template.removeSuffix(".%(ext)s")}($n).%(ext)s"
        }
        // Literal extension
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
        /**
         * Resolves a SAF / content URI to an actual file path that native tools (FFmpeg) can read.
         * For content:// URIs the file is copied to the app cache dir. Returns null on failure.
         */
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
