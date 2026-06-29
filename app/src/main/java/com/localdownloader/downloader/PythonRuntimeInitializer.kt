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
 * Initializes the embedded Python/yt-dlp runtime by preparing bundled
 * native libraries and assets for execution.
 *
 * On Android, the kernel's `execve()` requires the execute permission
 * bit on the binary file.  Some OEMs strip execute bits from `.so`
 * files during APK installation, and `chmod` + `File.canExecute()`
 * can silently lie on certain SELinux configurations.
 *
 * To work around this we use the **system dynamic linker** to invoke
 * our PIE binaries: instead of `execve("/path/to/libpython.so", ...)`
 * we run `/system/bin/linker64 /path/to/libpython.so ...`.  The
 * linker uses `mmap()` (like `dlopen()`) to load the binary, so the
 * file does not need execute permission.
 */
@Singleton
class PythonRuntimeInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: Logger,
) {
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

        private const val YTDLP_ASSET_PATH = "yt-dlp/yt-dlp"
    }

    @Volatile
    private var isExtracted = false
    private val lock = Any()

    /** Returns the app's native library directory (extracted by Android PackageManager). */
    private fun nativeLibraryDir(): File = File(context.applicationInfo.nativeLibraryDir)

    /** Returns the base runtime directory for copied/stdlib files. */
    private fun runtimeBaseDir(): File = File(context.noBackupFilesDir, RUNTIME_BASE_NAME)

    /** Returns the fallback directory for copied Python binaries. */
    private fun pythonBinDir(): File = File(runtimeBaseDir(), PYTHON_BIN_RELATIVE)

    /** Returns the fallback directory for copied QuickJS binaries. */
    private fun quickjsBinDir(): File = File(runtimeBaseDir(), QUICKJS_BIN_RELATIVE)

    /** Returns the directory where Python stdlib is extracted. */
    private fun pythonStdlibDir(): File = File(runtimeBaseDir(), PYTHON_STDLIB_RELATIVE)

    /** Returns the directory where yt-dlp script is placed. */
    private fun ytDlpDir(): File = File(runtimeBaseDir(), YTDLP_DIR_NAME)

    /** Returns the yt-dlp script file. */
    fun ytDlpScript(): File = File(ytDlpDir(), YTDLP_BIN)

    /**
     * Returns the Python interpreter binary (may not have execute perms).
     */
    fun pythonBinary(): File = File(nativeLibraryDir(), PYTHON_INTERPRETER).takeIf { it.exists() }
        ?: throw IOException("Missing libpython.so in nativeLibraryDir")

    /**
     * Returns the QuickJS interpreter binary (may not have execute perms).
     */
    fun quickJsBinary(): File = File(nativeLibraryDir(), QUICKJS_INTERPRETER).takeIf { it.exists() }
        ?: throw IOException("Missing libqjs.so in nativeLibraryDir")

    /**
     * Detects the system dynamic linker path.
     *
     * On 64-bit Android the linker is at `/system/bin/linker64`;
     * on 32-bit it is at `/system/bin/linker`.  The linker can
     * load and run a PIE ELF binary via `mmap()`, bypassing the
     * kernel's `execve()` permission check.
     */
    private val linkerPath: String by lazy {
        val candidates = listOf("/system/bin/linker64", "/system/bin/linker")
        candidates.firstOrNull { File(it).exists() }
            ?: throw IOException("No system linker found (tried: $candidates)")
    }

    /**
     * Returns `true` if the native binary at [path] can be executed
     * directly by the kernel (i.e. has the execute permission bit).
     *
     * On some devices `File.canExecute()` can return `true` even when
     * the kernel will still deny `execve()` (SELinux policy mismatch),
     * so we treat this as advisory only.
     */
    private fun canExecDirectly(path: File): Boolean {
        return path.exists() && path.canExecute()
    }

    /**
     * Returns the command prefix for running a native binary.
     *
     * If the binary has execute permission, runs it directly.
     * Otherwise uses the system linker to bypass the permission check.
     *
     * Example return values:
     * - `["/system/bin/linker64", "/path/to/libpython.so"]` (no exec perms)
     * - `["/path/to/libpython.so"]` (has exec perms)
     */
    fun pythonCommandPrefix(): List<String> {
        val binary = pythonBinary()
        return if (canExecDirectly(binary)) {
            listOf(binary.absolutePath)
        } else {
            listOf(linkerPath, binary.absolutePath)
        }
    }

    /**
     * Returns the `--js-runtimes` argument value for QuickJS.
     *
     * Returns the binary path directly if QuickJS has execute permission.
     * Returns `null` if the binary is inaccessible or not executable,
     * because yt-dlp's `--js-runtimes` option passes the path directly
     * to `subprocess.run()` and does not support a linker prefix.
     *
     * When `null` is returned yt-dlp runs without JavaScript extraction
     * support (core YouTube downloading is unaffected; some niche
     * sites that require JS extraction will fail gracefully).
     */
    fun quickJsRuntimeArg(): String? {
        val binary = quickJsBinary()
        if (!binary.exists()) return null
        return if (canExecDirectly(binary)) {
            "quickjs:${binary.absolutePath}"
        } else {
            logger.w("PythonRuntimeInitializer", "QuickJS not executable; JS extraction disabled")
            null
        }
    }

    /**
     * Returns the directory containing runtime native libraries.
     * Used in `LD_LIBRARY_PATH` for the dynamic linker.
     */
    fun runtimeBinDir(): File = pythonBinDir()

    /**
     * Returns the nativeLibraryDir path for `LD_LIBRARY_PATH` entries.
     */
    fun nativeLibraryPath(): String = nativeLibraryDir().absolutePath

    /**
     * Returns the directory containing the Python user-space files
     * (stdlib, libs, etc.) — used as PYTHONHOME.
     */
    fun pythonUsrDir(): File = File(runtimeBaseDir(), "packages/python/usr")

    /**
     * Returns the SSL certificate file path.
     */
    fun sslCertFile(): File = File(runtimeBaseDir(), CERT_TARGET_RELATIVE)

    /**
     * Ensures the Python stdlib is extracted and yt-dlp is in place.
     * Safe to call multiple times — idempotent after first run.
     */
    fun ensureExtracted() {
        if (isExtracted) return
        synchronized(lock) {
            if (isExtracted) return
            logger.i("PythonRuntimeInitializer", "Extracting runtime assets")
            extractPythonStdlib()
            extractYtDlpScript()
            isExtracted = true
            logger.i("PythonRuntimeInitializer", "Runtime assets extracted successfully")
        }
    }

    /**
     * Extracts `libpython.zip.so` (a zip of Python stdlib) from the
     * native library directory to the runtime packages directory.
     */
    private fun extractPythonStdlib() {
        val nativeLibraryDir = nativeLibraryDir()
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
