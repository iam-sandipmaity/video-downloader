package com.localdownloader.downloader

import android.content.Context
import com.localdownloader.utils.Logger
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
    private data class FfmpegCandidate(
        val sourceBinary: File,
        val supportDir: File?,
        val label: String,
    )

    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val ffmpegVerificationMutex = Mutex()
    @Volatile
    private var verifiedFfmpegRuntimeKey: String? = null

    suspend fun ensureFfmpegBinary(preferNative: Boolean = true): File = withContext(Dispatchers.IO) {
        ensureFfmpegRuntime(preferNative).executable
    }

    suspend fun ensureFfmpegRuntime(preferNative: Boolean = true): FfmpegRuntime = withContext(Dispatchers.IO) {
        val supportDir = ensureBundledFfmpegSupportDir()
        val overlayCandidate = resolveDownloadedOverlayFfmpegCandidate()
        val bundledLinkedRuntimeHasExplicitSupport = hasExplicitLinkedFfmpegRuntimeSupport(supportDir)
        val bundledLinkedNativeBinary = resolveNativeLibraryBinary(listOf("libffmpeg.so"))
        val bundledFallbackNativeBinary = resolveNativeLibraryBinary(listOf("libffmpeg_exec.so"))
        val candidates = buildList {
            if (overlayCandidate != null) {
                if (!hasExplicitLinkedFfmpegRuntimeSupport(overlayCandidate.supportDir)) {
                    logger.i(
                        "BinaryInstaller",
                        "Downloaded overlay libffmpeg.so has no explicit libc++_shared.so marker; attempting runtime verification before falling back",
                    )
                }
                add(overlayCandidate)
            }
            bundledLinkedNativeBinary?.let { nativeBinary ->
                if (!bundledLinkedRuntimeHasExplicitSupport) {
                    logger.i(
                        "BinaryInstaller",
                        "Bundled linked libffmpeg.so has no explicit libc++_shared.so marker; attempting runtime verification before falling back",
                    )
                }
                add(
                    FfmpegCandidate(
                        sourceBinary = nativeBinary,
                        supportDir = supportDir,
                        label = nativeBinary.name,
                    ),
                )
            }
            if (bundledLinkedNativeBinary == null && !bundledLinkedRuntimeHasExplicitSupport) {
                logger.i(
                    "BinaryInstaller",
                    "No bundled linked libffmpeg.so candidate was found; fallback libffmpeg_exec.so remains required",
                )
            }
            bundledFallbackNativeBinary?.let { nativeBinary ->
                add(
                    FfmpegCandidate(
                        sourceBinary = nativeBinary,
                        supportDir = null,
                        label = nativeBinary.name,
                    ),
                )
            }
        }
        if (candidates.isEmpty()) {
            throw IOException("Missing packaged ffmpeg binary in nativeLibraryDir")
        }

        var lastFailure: IOException? = null

        for (candidate in candidates) {
            val executable = if (preferNative) {
                candidate.sourceBinary
            } else {
                ensureCopiedExecutable(
                    sourceBinary = candidate.sourceBinary,
                    runtimeFolderName = "ffmpeg",
                    targetFileName = "ffmpeg",
                )
            }

            val environment = buildFfmpegEnvironment(
                binary = executable,
                supportDir = candidate.supportDir,
                extraLibraryDir = candidate.sourceBinary.parentFile,
            )
            val runtimeKey = buildFfmpegRuntimeKey(
                executable = executable,
                supportDir = candidate.supportDir,
            )

            if (verifiedFfmpegRuntimeKey == runtimeKey) {
                return@withContext FfmpegRuntime(executable, candidate.supportDir, environment)
            }

            try {
                ffmpegVerificationMutex.withLock {
                    if (verifiedFfmpegRuntimeKey != runtimeKey) {
                        verifyFfmpegBinary(executable, environment)
                        verifiedFfmpegRuntimeKey = runtimeKey
                    }
                }
                return@withContext FfmpegRuntime(executable, candidate.supportDir, environment)
            } catch (error: IOException) {
                lastFailure = error
                logger.w(
                    "BinaryInstaller",
                    "FFmpeg candidate ${candidate.label} failed health check; trying next runtime if available",
                    error,
                )
            }
        }

        throw lastFailure ?: IOException("Missing packaged ffmpeg binary in nativeLibraryDir")
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
            val ffmpegNative = resolveNativeLibraryBinary(listOf("libffmpeg.so", "libffmpeg_exec.so"))
            if (ffmpegNative == null) {
                logger.i("BinaryInstaller", "Skipping runtime cleanup because packaged native binaries are unavailable")
                return@withContext
            }

            var freedBytes = 0L

            freedBytes += deleteRecursively(File(File(context.filesDir, "bin"), "yt-dlp"))
            freedBytes += deleteRecursively(File(File(context.filesDir, "bin"), "ffmpeg"))
            val runtimeBaseDir = File(context.noBackupFilesDir, PythonRuntimeInitializer.RUNTIME_BASE_NAME)
            val packagesDir = File(runtimeBaseDir, "packages")
            freedBytes += deleteRecursively(File(packagesDir, "aria2c"))

            if (freedBytes > 0L) {
                logger.i(
                    "BinaryInstaller",
                    "Removed redundant runtime artifacts and freed approximately $freedBytes bytes",
                )
            }
        }
    }

    fun ffmpegOverlayDir(): File {
        return File(context.noBackupFilesDir, "localdownloader_runtime/downloaded_packages/ffmpeg")
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

    private fun resolveDownloadedOverlayFfmpegCandidate(): FfmpegCandidate? {
        val overlayDir = ffmpegOverlayDir()
        val overlayBinary = File(overlayDir, "libffmpeg.so")
        if (!overlayBinary.exists() || !overlayBinary.isFile) return null
        val overlaySupportDir = ensureSupportDirFromZip(
            zipBinary = File(overlayDir, "libffmpeg.zip.so"),
            supportDir = File(overlayDir, "support/ffmpeg"),
            markerFile = File(overlayDir, ".support-marker"),
        )
        logger.i("BinaryInstaller", "Using downloaded FFmpeg overlay: ${overlayBinary.absolutePath}")
        return FfmpegCandidate(
            sourceBinary = overlayBinary,
            supportDir = overlaySupportDir,
            label = "downloaded overlay",
        )
    }

    private fun hasExplicitLinkedFfmpegRuntimeSupport(supportDir: File?): Boolean {
        val appNativeDir = context.applicationInfo.nativeLibraryDir?.let(::File)
        val appHasSharedCpp = appNativeDir?.let { File(it, "libc++_shared.so").exists() } == true
        val supportHasSharedCpp = supportDir?.let { dir ->
            listOf(
                File(dir, "usr/lib/libc++_shared.so"),
                File(dir, "libc++_shared.so"),
            ).any { it.exists() }
        } == true
        return appHasSharedCpp || supportHasSharedCpp
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
        val embeddedRuntimeDir = File(File(context.noBackupFilesDir, PythonRuntimeInitializer.RUNTIME_BASE_NAME), "packages/ffmpeg")
        val embeddedUsrLib = File(embeddedRuntimeDir, "usr/lib")
        if (embeddedUsrLib.exists()) {
            logger.i("BinaryInstaller", "Using embedded ffmpeg support dir: ${embeddedRuntimeDir.absolutePath}")
            return embeddedRuntimeDir
        }

        val zipBinary = resolveNativeLibraryBinary(listOf("libffmpeg.zip.so")) ?: return null
        return ensureSupportDirFromZip(
            zipBinary = zipBinary,
            supportDir = File(context.noBackupFilesDir, "localdownloader_runtime/packages/ffmpeg"),
            markerFile = File(context.noBackupFilesDir, "localdownloader_runtime/packages/ffmpeg/.bundled-size"),
        )
    }

    private fun buildFfmpegRuntimeKey(executable: File, supportDir: File?): String = buildString {
        append(executable.absolutePath)
        append('|')
        append(supportDir?.absolutePath.orEmpty())
        append('|')
        append(supportDir?.lastModified() ?: 0L)
        append('|')
        append(executable.lastModified())
        append('|')
        append(executable.length())
    }

    private fun ensureCopiedExecutable(
        sourceBinary: File,
        runtimeFolderName: String,
        targetFileName: String,
    ): File {
        val targetDir = File(context.noBackupFilesDir, "localdownloader_runtime/bin/$runtimeFolderName")
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw IOException("Unable to create fallback runtime directory: ${targetDir.absolutePath}")
        }

        val targetBinary = File(targetDir, targetFileName)
        val versionMarker = File(targetDir, ".$targetFileName.source-size")
        val expectedMarker = "${sourceBinary.length()}|${sourceBinary.lastModified()}"
        val needsRefresh = !targetBinary.exists() ||
            !targetBinary.isFile ||
            versionMarker.readTextOrNull() != expectedMarker

        if (needsRefresh) {
            sourceBinary.copyTo(targetBinary, overwrite = true)
            if (!targetBinary.setExecutable(true, false) && !targetBinary.canExecute()) {
                throw IOException("Unable to mark fallback runtime executable: ${targetBinary.absolutePath}")
            }
            versionMarker.writeText(expectedMarker)
            logger.i(
                "BinaryInstaller",
                "Prepared copied ffmpeg fallback executable: ${targetBinary.absolutePath}",
            )
        }

        if (!targetBinary.canExecute() && !targetBinary.setExecutable(true, false)) {
            throw IOException("Fallback runtime is not executable: ${targetBinary.absolutePath}")
        }

        return targetBinary
    }

    private fun buildFfmpegEnvironment(
        binary: File,
        supportDir: File?,
        extraLibraryDir: File? = null,
    ): Map<String, String> {
        val ldPaths = mutableListOf<String>()
        supportDir?.let { dir ->
            val usrLib = File(dir, "usr/lib")
            when {
                usrLib.exists() -> ldPaths += usrLib.absolutePath
                dir.exists() -> ldPaths += dir.absolutePath
            }
        }
        binary.parentFile?.absolutePath?.let(ldPaths::add)
        extraLibraryDir?.absolutePath?.let(ldPaths::add)
        val pathEntries = listOfNotNull(
            binary.parentFile?.absolutePath,
            extraLibraryDir?.absolutePath,
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

    private fun ensureSupportDirFromZip(
        zipBinary: File,
        supportDir: File,
        markerFile: File,
    ): File? {
        if (!zipBinary.exists() || !zipBinary.isFile) return null
        val expectedMarker = "${zipBinary.length()}|${zipBinary.lastModified()}"
        if (supportDir.exists() && markerFile.readTextOrNull() == expectedMarker) {
            return supportDir
        }
        deleteRecursively(supportDir)
        supportDir.mkdirs()
        unzipSafely(zipBinary, supportDir)
        markerFile.parentFile?.mkdirs()
        markerFile.writeText(expectedMarker)
        logger.i("BinaryInstaller", "Prepared ffmpeg support dir: ${supportDir.absolutePath}")
        return supportDir
    }

    private fun unzipSafely(sourceZip: File, targetDir: File) {
        val targetRoot = targetDir.canonicalFile
        val targetRootPath = targetRoot.toPath()
        ZipInputStream(FileInputStream(sourceZip)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val normalizedEntryName = entry.name.replace('\\', '/')
                val unsafeEntry = normalizedEntryName.isBlank() ||
                    normalizedEntryName.startsWith("/") ||
                    normalizedEntryName == "." ||
                    normalizedEntryName == ".." ||
                    normalizedEntryName.contains("/../") ||
                    normalizedEntryName.startsWith("../") ||
                    normalizedEntryName.endsWith("/..")
                if (unsafeEntry) {
                    throw IOException("Unsafe zip entry path: ${entry.name}")
                }

                val outputPath = targetRootPath.resolve(normalizedEntryName).normalize()
                if (!outputPath.startsWith(targetRootPath)) {
                    throw IOException("Unsafe zip entry path: ${entry.name}")
                }
                val output = outputPath.toFile()

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

    private fun File.readTextOrNull(): String? {
        return runCatching { readText() }.getOrNull()
    }
}
