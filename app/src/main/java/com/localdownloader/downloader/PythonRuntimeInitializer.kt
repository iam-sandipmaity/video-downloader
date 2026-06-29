package com.localdownloader.downloader

import android.content.Context
import com.localdownloader.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Initializes the embedded Python/yt-dlp runtime by extracting bundled
 * native libraries and assets to the app's internal storage.
 *
 * Replaces the former `YoutubeDL.init()` that was provided by the
 * junkfood02/youtubedl-android library.
 */
@Singleton
class PythonRuntimeInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger,
) {
    /** Base directory name for all runtime files. */
    companion object {
        const val RUNTIME_BASE_NAME = "localdownloader_runtime"
        const val YTDLP_DIR_NAME = "yt-dlp"
        const val YTDLP_BIN = "yt-dlp"

        private const val PYTHON_ZIP_SO = "libpython.zip.so"
        private const val PYTHON_STDLIB_RELATIVE = "packages/python/usr/lib/python3.11"
        private const val CERT_TARGET_RELATIVE = "packages/python/usr/etc/tls/cert.pem"
        private const val MARKER_FILE_NAME = ".python-stdlib-extracted"
        private const val PYTHON_BIN_RELATIVE = "bin/python"
        private const val QUICKJS_BIN_RELATIVE = "bin/quickjs"
        private const val PYTHON_INTERPRETER = "libpython.so"
        private const val PYTHON_SHARED_LIB = "libpython3.11.so"
        private const val QUICKJS_INTERPRETER = "libqjs.so"

        /** Path to the yt-dlp script asset in the APK. */
        private const val YTDLP_ASSET_PATH = "yt-dlp/yt-dlp"
    }

    @Volatile
    private var isExtracted = false
    private val lock = Any()

    /**
     * Returns the base runtime directory.
     */
    private fun runtimeBaseDir(): File = File(context.noBackupFilesDir, RUNTIME_BASE_NAME)

    /**
     * Returns the directory where Python interpreter & shared lib are placed.
     */
    private fun pythonBinDir(): File = File(runtimeBaseDir(), PYTHON_BIN_RELATIVE)

    /**
     * Returns the directory where QuickJS interpreter is placed.
     */
    private fun quickjsBinDir(): File = File(runtimeBaseDir(), QUICKJS_BIN_RELATIVE)

    /**
     * Returns the directory where Python stdlib is extracted.
     */
    private fun pythonStdlibDir(): File = File(runtimeBaseDir(), PYTHON_STDLIB_RELATIVE)

    /**
     * Returns the directory where yt-dlp script is placed.
     */
    private fun ytDlpDir(): File = File(runtimeBaseDir(), YTDLP_DIR_NAME)

    /**
     * Returns the yt-dlp script file.
     */
    fun ytDlpScript(): File = File(ytDlpDir(), YTDLP_BIN)

    /**
     * Returns the executable Python interpreter (copied to a writable location
     * with execute permission).
     */
    fun pythonBinary(): File = File(pythonBinDir(), PYTHON_INTERPRETER)

    /**
     * Returns the QuickJS interpreter binary (copied to a writable location
     * with execute permission).
     */
    fun quickJsBinary(): File = File(quickjsBinDir(), QUICKJS_INTERPRETER)

    /**
     * Returns the directory containing all runtime binaries and shared libraries.
     * Used as a search path for the dynamic linker.
     */
    fun runtimeBinDir(): File = pythonBinDir()

    /**
     * Returns the directory containing the Python user-space files
     * (stdlib, libs, etc.) - used as PYTHONHOME.
     */
    fun pythonUsrDir(): File = File(runtimeBaseDir(), "packages/python/usr")

    /**
     * Returns the SSL certificate file path.
     */
    fun sslCertFile(): File = File(runtimeBaseDir(), CERT_TARGET_RELATIVE)

    /**
     * Ensures the Python stdlib is extracted and yt-dlp is in place.
     * Safe to call multiple times - idempotent after first run.
     */
    fun ensureExtracted() {
        if (isExtracted) return
        synchronized(lock) {
            if (isExtracted) return
            logger.i("PythonRuntimeInitializer", "Extracting runtime assets")
            ensureRuntimeBinaries()
            extractPythonStdlib()
            extractYtDlpScript()
            isExtracted = true
            logger.i("PythonRuntimeInitializer", "Runtime assets extracted successfully")
        }
    }

    /**
     * Copies Python and QuickJS native binaries from the APK's native library
     * directory to a writable location and ensures they have execute permission.
     *
     * Also copies the Python shared library (`libpython3.11.so`) alongside
     * the interpreter so that `$ORIGIN` RPATH resolves correctly.
     */
    private fun ensureRuntimeBinaries() {
        val nativeLibraryDir = File(context.applicationInfo.nativeLibraryDir)

        // Copy Python interpreter + shared lib
        val pythonBinDir = pythonBinDir()
        copyBinaryWithExec(
            source = File(nativeLibraryDir, PYTHON_INTERPRETER),
            targetDir = pythonBinDir,
            targetName = PYTHON_INTERPRETER,
        )
        copyBinaryWithExec(
            source = File(nativeLibraryDir, PYTHON_SHARED_LIB),
            targetDir = pythonBinDir,
            targetName = PYTHON_SHARED_LIB,
            executable = false,
        )

        // Copy QuickJS interpreter
        copyBinaryWithExec(
            source = File(nativeLibraryDir, QUICKJS_INTERPRETER),
            targetDir = quickjsBinDir(),
            targetName = QUICKJS_INTERPRETER,
        )
    }

    /**
     * Copies a file from the native library directory to a writable target
     * directory and optionally marks it as executable.
     *
     * On some Android devices `File.setExecutable()` can return `true` without
     * actually setting the kernel execute bit.  We therefore also try a `chmod`
     * shell fallback, and we verify the result with `canExecute()` every time
     * this method is called (not just on first copy).
     */
    private fun copyBinaryWithExec(
        source: File,
        targetDir: File,
        targetName: String,
        executable: Boolean = true,
    ) {
        if (!source.exists()) {
            throw IOException("Missing native binary: ${source.absolutePath}")
        }

        val targetFile = File(targetDir, targetName)
        val versionMarker = File(targetDir, ".$targetName.source-size")
        val expectedMarker = "${source.length()}|${source.lastModified()}"

        val needsRefresh = !targetFile.exists() ||
            !targetFile.isFile ||
            versionMarker.readTextOrNull() != expectedMarker

        if (needsRefresh) {
            targetDir.mkdirs()
            source.copyTo(targetFile, overwrite = true)

            if (executable) {
                ensureIsExecutable(targetFile)
            }

            versionMarker.writeText(expectedMarker)
            logger.i("PythonRuntimeInitializer", "Prepared ${targetFile.absolutePath}")
        }

        // Safety re-check every call (not just on copy) in case execute bit
        // was lost due to cache eviction, remount, or other platform quirk.
        if (executable && !targetFile.canExecute()) {
            logger.w("PythonRuntimeInitializer", "${targetFile.absolutePath} lost execute bit, re-applying")
            ensureIsExecutable(targetFile)
        }
    }

    /**
     * Ensures a file has the owner-execute bit set.
     *
     * Uses `File.setExecutable()` first (fast path).  On Android this can
     * silently fail on certain kernel/SELinux configurations, so we fall
     * back to `chmod` via the shell when the Java API does not take effect.
     */
    private fun ensureIsExecutable(file: File) {
        // Fast path: Java API
        if (file.setExecutable(true, false) && file.canExecute()) {
            return
        }

        // Slow path: shell chmod (works on all Android versions)
        try {
            val process = Runtime.getRuntime().exec(
                arrayOf("/system/bin/chmod", "0700", file.absolutePath)
            )
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw IOException(
                    "chmod exited with code $exitCode for ${file.absolutePath}"
                )
            }
            if (!file.canExecute()) {
                throw IOException(
                    "File is still not executable after chmod: ${file.absolutePath}"
                )
            }
            logger.i("PythonRuntimeInitializer", "Used chmod fallback for ${file.absolutePath}")
        } catch (e: IOException) {
            throw IOException(
                "Unable to mark ${file.absolutePath} as executable (Java API + chmod both failed)",
                e
            )
        }
    }

    /**
     * Extracts `libpython.zip.so` (a zip of Python stdlib) from the
     * native library directory to the runtime packages directory.
     */
    private fun extractPythonStdlib() {
        val nativeLibraryDir = File(context.applicationInfo.nativeLibraryDir)
        val zipSo = File(nativeLibraryDir, PYTHON_ZIP_SO)
        if (!zipSo.exists() || !zipSo.isFile) {
            throw IOException("Missing Python stdlib archive: ${zipSo.absolutePath}")
        }

        val stdlibDir = pythonStdlibDir()
        val markerFile = File(runtimeBaseDir(), MARKER_FILE_NAME)
        val expectedMarker = "${zipSo.length()}|${zipSo.lastModified()}"

        // Skip if already extracted and up to date
        if (stdlibDir.exists() && markerFile.readTextOrNull() == expectedMarker) {
            logger.i("PythonRuntimeInitializer", "Python stdlib already extracted and up to date")
            return
        }

        // Extract the zip contents
        logger.i("PythonRuntimeInitializer", "Extracting Python stdlib from ${zipSo.absolutePath}")
        stdlibDir.mkdirs()

        ZipInputStream(FileInputStream(zipSo)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val normalizedName = entry.name.replace('\\', '/')
                val target = File(stdlibDir, normalizedName).canonicalFile

                // Security check: prevent zip slip
                if (!target.canonicalPath.startsWith(stdlibDir.canonicalPath + File.separator) &&
                    target.canonicalPath != stdlibDir.canonicalPath
                ) {
                    throw IOException("Unsafe zip entry: ${entry.name}")
                }

                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { zip.copyTo(it) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        // Ensure SSL cert directory exists and bundle a default cert.pem if available
        ensureSslCert()

        // Write marker
        markerFile.parentFile?.mkdirs()
        markerFile.writeText(expectedMarker)

        logger.i("PythonRuntimeInitializer", "Python stdlib extracted to ${stdlibDir.absolutePath}")
    }

    /**
     * Copies yt-dlp from app assets to the runtime directory.
     */
    private fun extractYtDlpScript() {
        val targetDir = ytDlpDir()
        val targetFile = File(targetDir, YTDLP_BIN)

        if (targetFile.exists() && targetFile.isFile) {
            logger.i("PythonRuntimeInitializer", "yt-dlp script already in place")
            return
        }

        logger.i("PythonRuntimeInitializer", "Extracting yt-dlp from assets")
        targetDir.mkdirs()

        try {
            context.assets.open(YTDLP_ASSET_PATH).use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: IOException) {
            throw IOException("Failed to extract yt-dlp from assets (path: $YTDLP_ASSET_PATH)", e)
        }

        if (!targetFile.setExecutable(true, false) && !targetFile.canExecute()) {
            logger.w("PythonRuntimeInitializer", "Could not set yt-dlp as executable (not required for Python invocation)")
        }

        logger.i("PythonRuntimeInitializer", "yt-dlp script placed at ${targetFile.absolutePath}")
    }

    /**
     * Ensures an SSL/TLS CA certificate bundle exists at the expected
     * path. If the Python stdlib zip contains one, it's already in place
     * after extraction. Otherwise, attempts to locate a bundled asset.
     */
    private fun ensureSslCert() {
        val certFile = sslCertFile()
        if (certFile.exists()) {
            return
        }

        // Try to extract from assets if bundled
        try {
            context.assets.open("ssl/cert.pem").use { input ->
                certFile.parentFile?.mkdirs()
                certFile.outputStream().use { output ->
                    input.copyTo(output)
                }
                logger.i("PythonRuntimeInitializer", "SSL cert bundle extracted from assets")
            }
        } catch (e: IOException) {
            logger.w("PythonRuntimeInitializer", "No SSL cert bundle found in assets; HTTPS may fail for some sites")
        }
    }

    private fun File.readTextOrNull(): String? {
        return runCatching { readText() }.getOrNull()
    }
}
