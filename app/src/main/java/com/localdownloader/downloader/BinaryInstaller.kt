package com.localdownloader.downloader

import android.content.Context
import com.localdownloader.utils.Logger
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

data class FfmpegRuntime(
    val executable: File,
    val supportDir: File?,
    val environment: Map<String, String>,
)

/**
 * Resolves packaged native runtimes and cleans up older duplicated runtime artifacts.
 */
@Singleton
class BinaryInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val processRunner: ProcessRunner,
    private val logger: Logger,
) {
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ffmpegVerificationMutex = Mutex()
    @Volatile
    private var verifiedFfmpegRuntimeKey: String? = null

    suspend fun ensureFfmpegBinary(preferNative: Boolean = true): File = withContext(Dispatchers.IO) {
        ensureFfmpegRuntime(preferNative).executable
    }

    suspend fun ensureFfmpegRuntime(preferNative: Boolean = true): FfmpegRuntime = withContext(Dispatchers.IO) {
        if (!preferNative) {
            logger.w("BinaryInstaller", "Asset ffmpeg fallback is disabled; using packaged bundled runtime only")
        }
        val binary = resolveNativeLibraryBinary(listOf("libffmpeg.so", "libffmpeg_exec.so"))
            ?: throw IOException("Missing packaged ffmpeg binary in nativeLibraryDir")
        val supportDir = ensureBundledFfmpegSupportDir()
        val environment = buildFfmpegEnvironment(binary = binary, supportDir = supportDir)
        val runtimeKey = buildString {
            append(binary.absolutePath)
            append('|')
            append(supportDir?.absolutePath.orEmpty())
            append('|')
            append(supportDir?.lastModified() ?: 0L)
        }
        if (verifiedFfmpegRuntimeKey == runtimeKey) {
            return@withContext FfmpegRuntime(binary, supportDir, environment)
        }
        ffmpegVerificationMutex.withLock {
            if (verifiedFfmpegRuntimeKey != runtimeKey) {
                verifyFfmpegBinary(binary, environment)
                verifiedFfmpegRuntimeKey = runtimeKey
            }
        }
        FfmpegRuntime(binary, supportDir, environment)
    }

    fun cleanupRedundantArtifactsAsync() {
        cleanupScope.launch {
            runCatching { cleanupRedundantArtifacts() }
                .onFailure { error ->
                    logger.w("BinaryInstaller", "Failed cleaning redundant runtime artifacts", error)
                }
        }
    }

    private suspend fun cleanupRedundantArtifacts() {
        withContext(Dispatchers.IO) {
            val ffmpegNative = resolveNativeLibraryBinary(listOf("libffmpeg_exec.so"))
            if (ffmpegNative == null) {
                logger.i("BinaryInstaller", "Skipping runtime cleanup because packaged native binaries are unavailable")
                return@withContext
            }

            var freedBytes = 0L

            freedBytes += deleteRecursively(File(File(context.filesDir, "bin"), "yt-dlp"))
            freedBytes += deleteRecursively(File(File(context.filesDir, "bin"), "ffmpeg"))
            val runtimeBaseDir = File(context.noBackupFilesDir, YoutubeDL.baseName)
            val packagesDir = File(runtimeBaseDir, "packages")
            freedBytes += deleteRecursively(File(packagesDir, "ffmpeg"))
            freedBytes += deleteRecursively(File(packagesDir, "aria2c"))

            if (freedBytes > 0L) {
                logger.i(
                    "BinaryInstaller",
                    "Removed redundant runtime artifacts and freed approximately $freedBytes bytes",
                )
            }
        }
    }

    private fun resolveNativeLibraryBinary(candidates: List<String>): File? {
        if (candidates.isEmpty()) return null

        val nativeLibraryDir = context.applicationInfo.nativeLibraryDir?.let(::File)
            ?: return null

        candidates.forEach { candidateName ->
            val candidateFile = File(nativeLibraryDir, candidateName)
            if (candidateFile.exists() && candidateFile.isFile) {
                logger.i(
                    "BinaryInstaller",
                    "Using packaged runtime binary: ${candidateFile.absolutePath}",
                )
                return candidateFile
            }
        }
        return null
    }

    private suspend fun verifyFfmpegBinary(binary: File, environment: Map<String, String>) {
        logger.i(
            "BinaryInstaller",
            "Verifying packaged ffmpeg binary exists=${binary.exists()} canExec=${binary.canExecute()} size=${binary.length()} path=${binary.absolutePath}",
        )
        val result = processRunner.runCommand(
            command = listOf(binary.absolutePath, "-version"),
            environment = environment,
        )
        val versionLine = sequenceOf(result.stdout, result.stderr)
            .flatMap { text -> text.lineSequence() }
            .map(String::trim)
            .firstOrNull { line -> line.contains("ffmpeg version", ignoreCase = true) }

        if (result.exitCode != 0 || versionLine == null) {
            val detail = sequenceOf(result.stderr, result.stdout)
                .map(String::trim)
                .firstOrNull { it.isNotBlank() }
                ?.lineSequence()
                ?.map(String::trim)
                ?.firstOrNull { it.isNotBlank() }
                ?: "unknown ffmpeg launch error"
            throw IOException(
                "Bundled ffmpeg failed health check (exitCode=${result.exitCode}): $detail",
            )
        }

        logger.i("BinaryInstaller", "Bundled ffmpeg verified: $versionLine")
    }

    private fun ensureBundledFfmpegSupportDir(): File? {
        val zipBinary = resolveNativeLibraryBinary(listOf("libffmpeg.zip.so")) ?: return null
        val supportDir = File(context.noBackupFilesDir, "localdownloader_runtime/packages/ffmpeg")
        val versionMarker = File(supportDir, ".bundled-size")
        val expectedMarker = zipBinary.length().toString()

        if (supportDir.exists() && versionMarker.exists() && versionMarker.readText() == expectedMarker) {
            return supportDir
        }

        deleteRecursively(supportDir)
        supportDir.mkdirs()
        unzipSafely(zipBinary, supportDir)
        versionMarker.writeText(expectedMarker)
        logger.i("BinaryInstaller", "Prepared bundled ffmpeg support dir: ${supportDir.absolutePath}")
        return supportDir
    }

    private fun buildFfmpegEnvironment(binary: File, supportDir: File?): Map<String, String> {
        val ldPaths = mutableListOf<String>()
        supportDir?.let { dir ->
            val usrLib = File(dir, "usr/lib")
            when {
                usrLib.exists() -> ldPaths += usrLib.absolutePath
                dir.exists() -> ldPaths += dir.absolutePath
            }
        }
        binary.parentFile?.absolutePath?.let(ldPaths::add)
        val pathEntries = listOfNotNull(
            binary.parentFile?.absolutePath,
            System.getenv("PATH")?.takeIf { it.isNotBlank() },
        )
        return buildMap {
            if (ldPaths.isNotEmpty()) {
                put("LD_LIBRARY_PATH", ldPaths.distinct().joinToString(":"))
            }
            if (pathEntries.isNotEmpty()) {
                put("PATH", pathEntries.distinct().joinToString(":"))
            }
        }
    }

    private fun unzipSafely(sourceZip: File, targetDir: File) {
        val targetRoot = targetDir.canonicalFile
        ZipInputStream(FileInputStream(sourceZip)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val output = File(targetDir, entry.name).canonicalFile
                check(output.path.startsWith(targetRoot.path)) {
                    "Unsafe zip entry path: ${entry.name}"
                }
                if (entry.isDirectory) {
                    output.mkdirs()
                } else {
                    output.parentFile?.mkdirs()
                    output.outputStream().use { zip.copyTo(it) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
    }

    private fun deleteRecursively(target: File): Long {
        if (!target.exists()) return 0L

        val bytes = if (target.isDirectory) {
            target.listFiles()?.sumOf(::deleteRecursively) ?: 0L
        } else {
            target.length()
        }

        return if (target.delete()) bytes else 0L
    }
}
