package com.localdownloader.utils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.localdownloader.data.SettingsStore
import com.localdownloader.domain.models.AppSettings
import com.localdownloader.media.resolvePreferredMediaMimeTypeForExtension
import com.localdownloader.ui.model.ExternalOpenRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileUtils @Inject constructor(
    @ApplicationContext val context: Context,
    settingsStore: SettingsStore,
) {
    private val conversionDirName = "converted"
    private val binDirName = "bin"
    private val settingsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var latestSettings: AppSettings = AppSettings()

    init {
        settingsStore.observeSettings()
            .onEach { latestSettings = it }
            .launchIn(settingsScope)
    }

    enum class MediaFolderCategory {
        VIDEO,
        AUDIO,
        OTHER,
        PLAYLIST,
    }

    fun ensureVaultDir(): File {
        val vaultDir = File(context.noBackupFilesDir, "vault")
        if (!vaultDir.exists()) vaultDir.mkdirs()
        return vaultDir
    }

    fun moveToVault(path: String): String {
        val sourceFile = File(path)
        if (!sourceFile.exists()) return path

        val vaultDir = ensureVaultDir()
        val targetFile = File(vaultDir, sourceFile.name)
        if (sourceFile.renameTo(targetFile)) {
            return targetFile.absolutePath
        }

        return runCatching {
            sourceFile.copyTo(targetFile, overwrite = false)
            sourceFile.delete()
            targetFile.absolutePath
        }.getOrNull() ?: path
    }

    fun moveFromVault(path: String): String {
        val vaultFile = File(path)
        if (vaultFile.parentFile?.name?.equals("vault", ignoreCase = true) != true) return path

        val libraryDir = ensureDownloadsDir()
        val targetFile = File(libraryDir, vaultFile.name)
        if (vaultFile.renameTo(targetFile)) {
            return targetFile.absolutePath
        }

        return runCatching {
            vaultFile.copyTo(targetFile, overwrite = false)
            vaultFile.delete()
            targetFile.absolutePath
        }.getOrNull() ?: path
    }

    /**
     * Returns the downloads directory for yt-dlp output.
     *
     * Android 10+ (API 29+): app-specific external storage (`Android/data/...`).
     * On download completion the Worker copies the file to the public Downloads folder
     * so users can find it in their file manager.
     *
     * Android 9 and below: `/sdcard/Download/LocalDownloader/`
     */
    fun ensureDownloadsDir(subDirectoryName: String? = null): File {
        val rootFolderName = configuredRootFolderName()
        val normalizedSubdirectory = normalizeSubfolderSetting(subDirectoryName.orEmpty())
            .ifBlank { null }
        // Android 9 and below: direct path to the configured public storage root.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            val publicStorage = Environment.getExternalStorageDirectory()
            val appDir = buildPath(publicStorage, configuredPublicRootRelativePath(), normalizedSubdirectory)
            if ((appDir.exists() || appDir.mkdirs()) && appDir.canWrite()) return appDir
        }

        // Android 10+: app-specific external storage.
        val extDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val appSpecificDir = extDir?.let { buildPath(it, rootFolderName, normalizedSubdirectory) }
        if (appSpecificDir != null && (appSpecificDir.exists() || appSpecificDir.mkdirs()) && appSpecificDir.canWrite()) {
            return appSpecificDir
        }

        val internalKey = listOfNotNull("downloads", rootFolderName, normalizedSubdirectory).joinToString(File.separator)
        return ensureInternalDir(internalKey)
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
        copyUriToManagedFile(
            context = context,
            uri = uri,
            targetFile = targetFile,
            maxBytes = MAX_CACHE_FILE_SIZE,
        )
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
        val readableUri = requireReadableContentUri(uri)
        return context.contentResolver.openInputStream(readableUri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalStateException("Unable to read selected file")
    }

    fun createOutputTemplateWithDirectory(
        template: String,
        category: MediaFolderCategory = MediaFolderCategory.VIDEO,
    ): String {
        val outputDir = ensureDownloadsDir(resolveSubfolderForCategory(category)).absolutePath
        return "$outputDir/$template"
    }

    fun createUniquePlaylistDirectory(playlistName: String): File {
        val root = ensureDownloadsDir(resolveSubfolderForCategory(MediaFolderCategory.PLAYLIST))
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
        val deletedFiles = parentDir.listFiles()
            ?.filter { candidate ->
                candidate.isFile && matchesManagedArtifactForTemplate(
                    templateName = templateName,
                    candidateName = candidate.name,
                )
            }
            .orEmpty()
            .count { candidate -> deleteManagedFile(candidate.absolutePath) }

        return deletedFiles
    }

    fun sanitizeFileName(value: String): String {
        return sanitizeFileNameStatic(value)
    }

    fun normalizeDownloadsRootSetting(value: String): String {
        return normalizeRelativeDownloadsPath(
            raw = value,
            fallback = "LocalDownloader",
            allowBlank = false,
        )
    }

    fun normalizeSubfolderSetting(value: String): String {
        return normalizeRelativeDownloadsPath(
            raw = value,
            fallback = null,
            allowBlank = true,
        )
    }

    fun normalizeExternalStoragePath(value: String): String {
        return normalizeRelativeDownloadsPath(
            raw = value,
            fallback = null,
            allowBlank = true,
        )
    }

    fun resolveRelativeExternalStorageFolderFromTreeUri(uri: Uri): String? {
        val documentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
            ?: runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
            ?: return null
        return extractRelativeExternalStoragePath(documentId)
    }

    fun resolveRelativeDownloadsFolderFromTreeUri(uri: Uri): String? {
        val documentId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
            ?: runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
            ?: return null
        return extractRelativeDownloadsPath(documentId)
    }

    fun deriveSubfolderSettingFromRelativeDownloadsPath(
        rootFolderSetting: String,
        selectedRelativeDownloadsPath: String,
    ): String? {
        val normalizedRoot = normalizeDownloadsRootSetting(rootFolderSetting)
        val normalizedSelected = normalizeSubfolderSetting(selectedRelativeDownloadsPath)
        if (normalizedSelected.isBlank()) return null
        if (normalizedSelected == normalizedRoot) return ""
        val rootedPrefix = "$normalizedRoot/"
        if (!normalizedSelected.startsWith(rootedPrefix)) return null
        return normalizedSelected.removePrefix(rootedPrefix)
    }

    fun deriveSubfolderSettingFromRelativePublicPath(
        rootPublicPath: String,
        rootFolderSetting: String,
        selectedRelativePublicPath: String,
    ): String? {
        val normalizedRoot = normalizeExternalStoragePath(rootPublicPath).ifBlank {
            buildDefaultPublicRootPath(rootFolderSetting)
        }
        val normalizedSelected = normalizeExternalStoragePath(selectedRelativePublicPath)
        if (normalizedSelected.isBlank()) return null
        if (normalizedSelected == normalizedRoot) return ""
        val rootedPrefix = "$normalizedRoot/"
        if (!normalizedSelected.startsWith(rootedPrefix)) return null
        return normalizedSelected.removePrefix(rootedPrefix)
    }

    /**
     * On Android 10+ copies a file from the app-specific directory to the configured
     * public storage root via MediaStore so it becomes visible in file managers.
     * Returns the public path, or null on Android 9 and below (file is already public).
     */
    fun copyToPublicDownloads(
        sourceFile: File,
        playlistFolderName: String? = null,
        targetFileName: String? = null,
    ): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return copyToPublicDownloadsApi29(
            sourceFile = sourceFile,
            playlistFolderName = playlistFolderName,
            targetFileName = targetFileName,
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun copyToPublicDownloadsApi29(
        sourceFile: File,
        playlistFolderName: String?,
        targetFileName: String?,
    ): String? {
        val publicStorage = Environment.getExternalStorageDirectory()
        val rootRelativePath = configuredPublicRootRelativePath()
        val relativeParent = playlistFolderName
            ?.trim()
            ?.trim('/', '\\')
            ?.ifBlank { null }
            ?: relativePathWithinDownloadsRoot(sourceFile)
                ?.substringBeforeLast('/', "")
                .orEmpty()
        val publicDir = if (relativeParent.isBlank()) {
            File(publicStorage, rootRelativePath)
        } else {
            File(publicStorage, "$rootRelativePath/$relativeParent")
        }
        if (!publicDir.exists()) publicDir.mkdirs()

        val requestedFileName = targetFileName
            ?.takeIf { it.isNotBlank() }
            ?: sourceFile.name
        val displayName = resolveUniqueFileName(publicDir, requestedFileName)
        val destFile = File(publicDir, displayName)
        var insertedUri: Uri? = null

        return try {
            val values = android.content.ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, guessMimeType(sourceFile.name))
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    buildString {
                        append(rootRelativePath.trim('/'))
                        append('/')
                        if (relativeParent.isNotBlank()) {
                            append(relativeParent.trim('/'))
                            append('/')
                        }
                    },
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val targetUri = context.contentResolver.insert(
                collectionUriForRelativePath(rootRelativePath),
                values,
            ) ?: throw IllegalStateException("Unable to create public storage item.")
            insertedUri = targetUri
            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw IllegalStateException("Unable to open public Downloads output stream.")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(targetUri, values, null, null)
            destFile.absolutePath
        } catch (e: Exception) {
            insertedUri?.let { uri ->
                runCatching { context.contentResolver.delete(uri, null, null) }
            }
            copyToPersistedTreeRoot(
                sourceFile = sourceFile,
                relativeParent = relativeParent,
                requestedFileName = requestedFileName,
            )?.let { return it }
            try {
                sourceFile.copyTo(destFile, overwrite = false)
                triggerMediaScan(Uri.fromFile(destFile))
                destFile.absolutePath
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun copyToPersistedTreeRoot(
        sourceFile: File,
        relativeParent: String,
        requestedFileName: String,
    ): String? {
        val treeUri = latestSettings.downloadsRootTreeUri
            .takeIf { it.isNotBlank() }
            ?.let { runCatching { it.toUri() }.getOrNull() }
            ?: return null
        val rootRelativePath = configuredPublicRootRelativePath()
        return runCatching {
            val targetDirUri = ensureTreeSubdirectory(
                treeUri = treeUri,
                relativePath = relativeParent,
            )
            val displayName = resolveUniqueTreeFileName(
                parentDocumentUri = targetDirUri,
                originalName = requestedFileName,
            )
            val targetUri = DocumentsContract.createDocument(
                context.contentResolver,
                targetDirUri,
                guessMimeType(displayName),
                displayName,
            ) ?: return@runCatching null
            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                sourceFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return@runCatching null
            buildPublicFilePath(
                rootRelativePath = rootRelativePath,
                relativeParent = relativeParent,
                displayName = displayName,
            )
        }.getOrNull()
    }

    private fun ensureTreeSubdirectory(
        treeUri: Uri,
        relativePath: String,
    ): Uri {
        var parentUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        relativePath
            .split('/', '\\')
            .map(::sanitizeFolderSegment)
            .filter { it.isNotBlank() }
            .forEach { segment ->
                parentUri = findTreeChild(
                    treeUri = treeUri,
                    parentDocumentUri = parentUri,
                    displayName = segment,
                    mimeType = DocumentsContract.Document.MIME_TYPE_DIR,
                ) ?: DocumentsContract.createDocument(
                    context.contentResolver,
                    parentUri,
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    segment,
                ) ?: throw IllegalStateException("Unable to create storage folder $segment")
            }
        return parentUri
    }

    private fun resolveUniqueTreeFileName(parentDocumentUri: Uri, originalName: String): String {
        val existingNames = queryTreeChildNames(parentDocumentUri)
        if (originalName !in existingNames) return originalName

        val dotIndex = originalName.lastIndexOf('.')
        val baseName = if (dotIndex > 0) originalName.substring(0, dotIndex) else originalName
        val extension = if (dotIndex > 0) originalName.substring(dotIndex) else ""
        var counter = 2
        while (true) {
            val candidate = "$baseName ($counter)$extension"
            if (candidate !in existingNames) return candidate
            counter += 1
        }
    }

    private fun findTreeChild(
        treeUri: Uri,
        parentDocumentUri: Uri,
        displayName: String,
        mimeType: String?,
    ): Uri? {
        val parentDocumentId = DocumentsContract.getDocumentId(parentDocumentUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        return context.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext()) {
                if (
                    cursor.getString(nameIndex) == displayName &&
                    (mimeType == null || cursor.getString(mimeIndex) == mimeType)
                ) {
                    return@use DocumentsContract.buildDocumentUriUsingTree(treeUri, cursor.getString(idIndex))
                }
            }
            null
        }
    }

    private fun queryTreeChildNames(parentDocumentUri: Uri): Set<String> {
        val treeUri = latestSettings.downloadsRootTreeUri
            .takeIf { it.isNotBlank() }
            ?.let { runCatching { it.toUri() }.getOrNull() }
            ?: return emptySet()
        val parentDocumentId = DocumentsContract.getDocumentId(parentDocumentUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        return context.contentResolver.query(
            childrenUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            buildSet {
                while (cursor.moveToNext()) {
                    cursor.getString(nameIndex)?.let(::add)
                }
            }
        }.orEmpty()
    }

    private fun persistedTreeFileExists(file: File): Boolean {
        val treeUri = latestSettings.downloadsRootTreeUri
            .takeIf { it.isNotBlank() }
            ?.let { runCatching { it.toUri() }.getOrNull() }
            ?: return false
        val relativeToRoot = relativePathWithinConfiguredPublicRoot(file) ?: return false
        return runCatching {
            var parentUri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri),
            )
            val segments = relativeToRoot.split('/').filter { it.isNotBlank() }
            if (segments.isEmpty()) return@runCatching false
            segments.dropLast(1).forEach { segment ->
                parentUri = findTreeChild(
                    treeUri = treeUri,
                    parentDocumentUri = parentUri,
                    displayName = segment,
                    mimeType = DocumentsContract.Document.MIME_TYPE_DIR,
                ) ?: return@runCatching false
            }
            findTreeChild(
                treeUri = treeUri,
                parentDocumentUri = parentUri,
                displayName = segments.last(),
                mimeType = null,
            ) != null
        }.getOrDefault(false)
    }

    private fun relativePathWithinConfiguredPublicRoot(file: File): String? {
        val relativePublicPath = relativePathWithinPublicStorage(file) ?: return null
        val rootRelativePath = configuredPublicRootRelativePath().trim('/')
        if (relativePublicPath == rootRelativePath) return ""
        val rootedPrefix = "$rootRelativePath/"
        if (!relativePublicPath.startsWith(rootedPrefix)) return null
        return relativePublicPath.removePrefix(rootedPrefix).ifBlank { null }
    }

    private fun buildPublicFilePath(
        rootRelativePath: String,
        relativeParent: String,
        displayName: String,
    ): String {
        val publicStorage = Environment.getExternalStorageDirectory()
        val relativeFilePath = listOf(rootRelativePath, relativeParent, displayName)
            .filter { it.isNotBlank() }
            .joinToString("/")
        return File(publicStorage, relativeFilePath).absolutePath
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

    fun pruneEmptyPrivateDownloadDirectories() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val downloadsRoot = ensureDownloadsDir().absoluteFile.normalize()
        downloadsRoot
            .walkBottomUp()
            .filter { candidate ->
                candidate.isDirectory &&
                    candidate.absoluteFile.normalize() != downloadsRoot &&
                    candidate.listFiles()?.isEmpty() == true
            }
            .forEach { candidate ->
                runCatching { candidate.delete() }
            }
    }

    fun deleteManagedFile(path: String): Boolean {
        val targetFile = File(path)
        if (!targetFile.exists()) return true
        if (targetFile.delete()) return true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return path

        val relativePath = relativePathWithinDownloadsRoot(File(path)) ?: return path
        val publicStorage = Environment.getExternalStorageDirectory()
        val publicFile = File(publicStorage, "${configuredPublicRootRelativePath()}/$relativePath")
        return if (publicFile.exists()) publicFile.absolutePath else path
    }

    fun managedFileExists(path: String): Boolean {
        val targetFile = File(path)
        if (targetFile.exists()) return true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return publicMediaStoreFileExists(targetFile) || persistedTreeFileExists(targetFile)
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

    private fun relativePathWithinPublicStorage(sourceFile: File): String? {
        val publicStorage = Environment.getExternalStorageDirectory()
            .absoluteFile
            .normalize()
            .path
        val sourcePath = sourceFile.absoluteFile.normalize().path
        if (!sourcePath.startsWith(publicStorage)) return null
        return sourcePath
            .removePrefix(publicStorage)
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
        return resolvePreferredMediaMimeTypeForExtension(ext) ?: when (ext) {
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
                    isManagedSidecarArtifact(candidate.name) &&
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

    private fun isManagedSidecarArtifact(fileName: String): Boolean {
        val lower = fileName.lowercase()
        if (Regex(""".*\.f\d+\..+""").matches(lower)) return false
        if (lower.contains(".video.") || lower.contains(".audio.")) return false

        return when {
            lower.endsWith(".srt") -> true
            lower.endsWith(".vtt") -> true
            lower.endsWith(".webvtt") -> true
            lower.endsWith(".ass") -> true
            lower.endsWith(".ssa") -> true
            lower.endsWith(".ttml") -> true
            lower.endsWith(".dfxp") -> true
            lower.endsWith(".xml") -> true
            lower.endsWith(".jpg") -> true
            lower.endsWith(".jpeg") -> true
            lower.endsWith(".png") -> true
            lower.endsWith(".webp") -> true
            lower.endsWith(".gif") -> true
            lower.endsWith(".bmp") -> true
            lower.endsWith(".info.json") -> true
            lower.endsWith(".description") -> true
            else -> false
        }
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return deleteFromMediaStoreApi29(file)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun publicMediaStoreFileExists(file: File): Boolean {
        val relativePublicPath = relativePathWithinPublicStorage(file) ?: return false
        val relativeParent = relativePublicPath
            .substringBeforeLast('/', "")
            .takeIf { it.isNotBlank() }
            ?.let { "$it/" }
            ?: ""
        val projection = arrayOf(android.provider.BaseColumns._ID)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?"
        val selectionArgs = arrayOf(file.name, relativeParent)
        return context.contentResolver.query(
            collectionUriForRelativePath(relativePublicPath),
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor -> cursor.moveToFirst() } == true
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deleteFromMediaStoreApi29(file: File): Boolean? {
        val downloadsRoot = Environment.getExternalStorageDirectory()
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
            if (relativePath.isNotBlank()) {
                append(relativePath.trim('/'))
                append('/')
            }
        }

        val projection = arrayOf(android.provider.BaseColumns._ID)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?"
        val selectionArgs = arrayOf(file.name, mediaStoreRelativePath)
        val contentUri = collectionUriForRelativePath(relativePath)
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

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun externalDownloadsCollectionUri(): Uri = EXTERNAL_DOWNLOADS_CONTENT_URI.toUri()

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun collectionUriForRelativePath(relativePath: String): Uri {
        val topLevel = relativePath
            .trim('/')
            .substringBefore('/')
            .lowercase()
        return when (topLevel) {
            "download", "downloads" -> externalDownloadsCollectionUri()
            "movies" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            "music", "audiobooks", "podcasts", "ringtones" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            "pictures", "dcim" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
    }

    private fun configuredRootFolderName(): String {
        return normalizeDownloadsRootSetting(latestSettings.downloadsRootFolderName)
    }

    private fun configuredPublicRootRelativePath(): String {
        return normalizeExternalStoragePath(latestSettings.downloadsRootPublicPath).ifBlank {
            buildDefaultPublicRootPath(latestSettings.downloadsRootFolderName)
        }
    }

    private fun resolveSubfolderForCategory(category: MediaFolderCategory): String? {
        val raw = when (category) {
            MediaFolderCategory.VIDEO -> latestSettings.videoSubfolderName
            MediaFolderCategory.AUDIO -> latestSettings.audioSubfolderName
            MediaFolderCategory.OTHER -> latestSettings.otherSubfolderName
            MediaFolderCategory.PLAYLIST -> latestSettings.videoSubfolderName
        }
        return normalizeSubfolderSetting(raw).ifBlank { null }
    }

    private fun buildPath(
        root: File,
        rootFolderName: String,
        subDirectoryName: String?,
    ): File {
        val rootDir = File(root, rootFolderName)
        return if (subDirectoryName.isNullOrBlank()) {
            rootDir
        } else {
            File(rootDir, subDirectoryName)
        }
    }

    private fun buildDefaultPublicRootPath(rootFolderName: String): String {
        return "Download/${normalizeDownloadsRootSetting(rootFolderName)}"
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
        val root = when (name.substringBefore(File.separator).substringBefore('/').substringBefore('\\')) {
            "cookies" -> context.noBackupFilesDir
            "opened" -> context.cacheDir
            else -> context.filesDir
        }
        val dir = File(root, name)
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
        copyUriToManagedFile(
            context = context,
            uri = uri,
            targetFile = targetFile,
            maxBytes = MAX_CACHE_FILE_SIZE,
        )

        return ExternalOpenRequest(
            path = targetFile.absolutePath,
            displayName = targetFile.name,
            mimeType = resolvedMimeType,
        )
    }

    companion object {
        /** Maximum size for files copied to cache (500MB). */
        private const val MAX_CACHE_FILE_SIZE = 500L * 1024 * 1024
        private const val EXTERNAL_DOWNLOADS_CONTENT_URI = "content://media/external/downloads"

        private fun sanitizeFileNameStatic(value: String): String {
            return value
                .replace(Regex("""[\\/:*?"<>|]"""), "_")
                .take(160)
                .ifBlank { "media_${System.currentTimeMillis()}" }
        }

private fun normalizeRelativeDownloadsPath(
    raw: String,
    fallback: String?,
    allowBlank: Boolean,
): String {
            val normalized = raw
                .replace('\\', '/')
                .split('/')
                .map(::sanitizeFolderSegment)
                .filter { it.isNotBlank() }
                .joinToString("/")

            if (normalized.isNotBlank()) return normalized
            if (allowBlank) return ""

            val fallbackValue = fallback?.takeIf { it.isNotBlank() } ?: return ""
            return normalizeRelativeDownloadsPath(
                raw = fallbackValue,
                fallback = null,
                allowBlank = allowBlank,
            )
        }

        private fun sanitizeFolderSegment(value: String): String {
            return value
                .trim()
                .replace(Regex("""[:*?"<>|]"""), "_")
                .trim('.', ' ')
        }

        private fun extractRelativeExternalStoragePath(documentId: String): String? {
            val normalized = documentId
                .removePrefix("raw:")
                .substringAfter(':', documentId.removePrefix("raw:"))
                .replace('\\', '/')
                .trim('/')
            if (normalized.isBlank()) return null

            val segments = normalized.split('/').filter { it.isNotBlank() }
            val publicSegments = segments.dropWhile { segment ->
                segment.equals("storage", ignoreCase = true) ||
                    segment.equals("emulated", ignoreCase = true) ||
                    segment == "0" ||
                    segment.equals("primary", ignoreCase = true)
            }
            return normalizeRelativeDownloadsPath(
                raw = publicSegments.joinToString("/"),
                fallback = null,
                allowBlank = false,
            ).ifBlank { null }
        }

        private fun extractRelativeDownloadsPath(documentId: String): String? {
            val normalized = documentId
                .removePrefix("raw:")
                .substringAfter(':', documentId.removePrefix("raw:"))
                .replace('\\', '/')
                .trim('/')
            if (normalized.isBlank()) return null

            val segments = normalized.split('/').filter { it.isNotBlank() }
            val downloadsIndex = segments.indexOfFirst {
                it.equals("Download", ignoreCase = true) || it.equals("Downloads", ignoreCase = true)
            }
            if (downloadsIndex == -1) return null

            val relative = segments.drop(downloadsIndex + 1).joinToString("/")
            return normalizeRelativeDownloadsPath(
                raw = relative,
                fallback = null,
                allowBlank = true,
            )
        }

        fun getRealPathFromUri(context: Context, uri: Uri): String? {
            if (uri.scheme == "file") return uri.path
            if (uri.scheme != "content") return null
            return try {
                val displayName = queryDisplayName(context, uri)
                    ?: "media_${System.currentTimeMillis()}"
                val dest = File(context.cacheDir, sanitizeFileNameStatic(displayName))

                // Check available cache space before copying.
                val cacheDir = context.cacheDir
                val availableSpace = cacheDir.freeSpace
                val contentLength = getContentLength(context, uri)

                if (contentLength > MAX_CACHE_FILE_SIZE) {
                    throw IllegalStateException("File is too large to cache (${contentLength / (1024 * 1024)}MB). Maximum allowed: ${MAX_CACHE_FILE_SIZE / (1024 * 1024)}MB")
                }
                if (contentLength > 0 && contentLength > availableSpace) {
                    throw IllegalStateException("Not enough cache space. Required: ${contentLength / (1024 * 1024)}MB, Available: ${availableSpace / (1024 * 1024)}MB")
                }

                copyUriToManagedFile(
                    context = context,
                    uri = uri,
                    targetFile = dest,
                    maxBytes = MAX_CACHE_FILE_SIZE,
                )
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

        private fun getContentLength(context: Context, uri: Uri): Long {
            val readableUri = requireReadableContentUri(uri)
            return try {
                context.contentResolver.query(
                    readableUri,
                    arrayOf(OpenableColumns.SIZE),
                    null,
                    null,
                    null,
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getLong(0).takeIf { it > 0L }
                    } else {
                        null
                    }
                } ?: context.contentResolver.openAssetFileDescriptor(readableUri, "r")?.use { descriptor ->
                    descriptor.length.takeIf { it > 0L }
                } ?: 0L
            } catch (_: Exception) {
                0L
            }
        }

        private fun copyUriToManagedFile(
            context: Context,
            uri: Uri,
            targetFile: File,
            maxBytes: Long,
        ) {
            val readableUri = requireReadableContentUri(uri)
            val managedTargetFile = requireManagedTargetFile(context, targetFile)
            val targetDir = managedTargetFile.parentFile
                ?: throw IllegalStateException("Unable to resolve target directory.")
            targetDir.mkdirs()
            val availableSpace = allocatableSpaceBytes(context, targetDir)
            val contentLength = getContentLength(context, readableUri)

            if (contentLength > maxBytes) {
                throw IllegalStateException("File is too large to cache (${contentLength / (1024 * 1024)}MB). Maximum allowed: ${maxBytes / (1024 * 1024)}MB")
            }
            if (contentLength > 0 && contentLength > availableSpace) {
                throw IllegalStateException("Not enough cache space. Required: ${contentLength / (1024 * 1024)}MB, Available: ${availableSpace / (1024 * 1024)}MB")
            }

            try {
                context.contentResolver.openInputStream(readableUri)?.use { input ->
                    managedTargetFile.outputStream().use { output ->
                        copyStreamWithLimit(
                            input = input,
                            output = output,
                            maxBytes = maxBytes,
                            availableSpace = availableSpace,
                        )
                    }
                } ?: error("Unable to open selected file")
            } catch (error: Throwable) {
                runCatching { managedTargetFile.delete() }
                throw error
            }
        }

        private fun requireReadableContentUri(uri: Uri): Uri {
            val normalizedUri = uri.normalizeScheme()
            require(normalizedUri.scheme == ContentResolver.SCHEME_CONTENT) {
                "Only content URIs can be imported through this path."
            }
            require(!normalizedUri.authority.isNullOrBlank()) {
                "Content URI authority is required."
            }
            return normalizedUri
        }

        private fun requireManagedTargetFile(context: Context, targetFile: File): File {
            val managedTargetFile = targetFile.canonicalFile
            val managedRoots = listOf(
                context.filesDir,
                context.cacheDir,
                context.noBackupFilesDir,
            ).map { it.canonicalFile }

            val insideManagedStorage = managedRoots.any { root ->
                managedTargetFile == root ||
                    managedTargetFile.path.startsWith(root.path + File.separator)
            }
            require(insideManagedStorage) {
                "Target file must stay inside app-managed storage."
            }
            return managedTargetFile
        }

        private fun allocatableSpaceBytes(context: Context, directory: File): Long {
            val storageManager = context.getSystemService(StorageManager::class.java)
            return runCatching {
                storageManager.getAllocatableBytes(storageManager.getUuidForPath(directory))
            }.getOrElse {
                directory.usableSpace
            }
        }

        private fun copyStreamWithLimit(
            input: InputStream,
            output: OutputStream,
            maxBytes: Long,
            availableSpace: Long,
        ) {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var copiedBytes = 0L
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break

                copiedBytes += read
                if (copiedBytes > maxBytes) {
                    throw IllegalStateException("File is too large to cache (${copiedBytes / (1024 * 1024)}MB). Maximum allowed: ${maxBytes / (1024 * 1024)}MB")
                }
                if (copiedBytes > availableSpace) {
                    throw IllegalStateException("Not enough cache space. Required: ${copiedBytes / (1024 * 1024)}MB, Available: ${availableSpace / (1024 * 1024)}MB")
                }

                output.write(buffer, 0, read)
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

internal fun managedArtifactStem(templateName: String): String {
    return templateName.substringBefore(".%(ext)s", templateName)
}

internal fun matchesManagedArtifactForTemplate(
    templateName: String,
    candidateName: String,
): Boolean {
    val stem = managedArtifactStem(templateName).trim()
    if (stem.isBlank()) return false
    return candidateName == stem || candidateName.startsWith("$stem.")
}
