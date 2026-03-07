package com.localdownloader.downloader

import android.content.Context
import android.os.Build
import com.localdownloader.utils.FileUtils
import com.localdownloader.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Prefers packaged native binaries and falls back to asset copy + chmod in app storage.
 */
@Singleton
class BinaryInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileUtils: FileUtils,
    private val logger: Logger,
) {
    suspend fun ensureYtDlpBinary(): File = ensureBinary(
        toolFolder = "yt-dlp",
        binaryName = "yt-dlp",
        nativeLibraryCandidates = listOf("libyt_dlp.so", "libyt-dlp.so"),
    )

    suspend fun ensureFfmpegBinary(): File = ensureBinary(
        toolFolder = "ffmpeg",
        binaryName = "ffmpeg",
        nativeLibraryCandidates = listOf("libffmpeg_exec.so", "libffmpeg.so"),
    )

    private suspend fun ensureBinary(
        toolFolder: String,
        binaryName: String,
        nativeLibraryCandidates: List<String>,
    ): File {
        return withContext(Dispatchers.IO) {
            val nativeLibraryBinary = resolveNativeLibraryBinary(nativeLibraryCandidates)
            if (nativeLibraryBinary != null) {
                logger.i(
                    "BinaryInstaller",
                    "Using $toolFolder binary from nativeLibraryDir: ${nativeLibraryBinary.absolutePath}",
                )
                return@withContext nativeLibraryBinary
            }

            val targetFile = File(fileUtils.ensureBinDir(), binaryName)
            if (!targetFile.exists() || targetFile.length() == 0L) {
                val sourceAssetPath = resolveAssetPath(toolFolder = toolFolder, binaryName = binaryName)
                logger.i("BinaryInstaller", "Installing $toolFolder binary from $sourceAssetPath")
                context.assets.open(sourceAssetPath).use { input ->
                    targetFile.outputStream().use { output -> input.copyTo(output) }
                }
            }

            targetFile.setReadable(true, false)
            targetFile.setWritable(true, true)
            val executableSet = targetFile.setExecutable(true, false)
            if (!executableSet) {
                logger.w(
                    "BinaryInstaller",
                    "Unable to mark ${targetFile.absolutePath} as executable; this device may block execution from app files dir.",
                )
            }

            targetFile
        }
    }

    private fun resolveAssetPath(toolFolder: String, binaryName: String): String {
        val discoveredCandidates = mutableListOf<String>()

        Build.SUPPORTED_ABIS.forEach { abi ->
            val candidate = "$toolFolder/$abi/$binaryName"
            if (assetExists(candidate)) return candidate

            val abiFolder = "$toolFolder/$abi"
            val fallbackInAbi = pickFirstBinaryAssetInFolder(abiFolder)
            if (fallbackInAbi != null) {
                return "$abiFolder/$fallbackInAbi"
            }
            discoveredCandidates += listAssets(abiFolder).map { "$abiFolder/$it" }
        }

        val fallback = "$toolFolder/$binaryName"
        if (assetExists(fallback)) return fallback

        val rootFallback = pickFirstBinaryAssetInFolder(toolFolder)
        if (rootFallback != null) return "$toolFolder/$rootFallback"
        discoveredCandidates += listAssets(toolFolder).map { "$toolFolder/$it" }

        throw IOException(
            "No bundled binary found for $toolFolder. Expected one of supported ABIs: ${
                Build.SUPPORTED_ABIS.joinToString()
            }. Found assets: ${
                discoveredCandidates
                    .filterNot { it.endsWith(".md", ignoreCase = true) }
                    .ifEmpty { listOf("none") }
                    .joinToString()
            }",
        )
    }

    private fun pickFirstBinaryAssetInFolder(folder: String): String? {
        val assets = listAssets(folder)
            .filterNot { it.endsWith(".md", ignoreCase = true) }
            .filterNot { it.endsWith(".txt", ignoreCase = true) }
            .filterNot { it.startsWith(".") }
            .filter { assetExists("$folder/$it") }

        if (assets.isEmpty()) return null

        // Prefer conventional binary name when present.
        val preferred = assets.firstOrNull { it == "yt-dlp" || it == "ffmpeg" }
        return preferred ?: assets.first()
    }

    private fun listAssets(folder: String): List<String> {
        return try {
            context.assets.list(folder)?.toList().orEmpty()
        } catch (_: IOException) {
            emptyList()
        }
    }

    private fun assetExists(path: String): Boolean {
        return try {
            context.assets.open(path).close()
            true
        } catch (_: IOException) {
            false
        }
    }

    private fun resolveNativeLibraryBinary(candidates: List<String>): File? {
        if (candidates.isEmpty()) return null

        val nativeLibraryDir = context.applicationInfo.nativeLibraryDir?.let(::File)
            ?: return null

        candidates.forEach { candidateName ->
            val candidateFile = File(nativeLibraryDir, candidateName)
            if (candidateFile.exists() && candidateFile.isFile) {
                return candidateFile
            }
        }
        return null
    }
}
