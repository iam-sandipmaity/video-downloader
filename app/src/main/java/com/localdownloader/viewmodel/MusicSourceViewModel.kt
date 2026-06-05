package com.localdownloader.viewmodel

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localdownloader.domain.models.MusicLibraryTrack
import com.localdownloader.domain.models.MusicSourceType
import com.localdownloader.media.isLikelyAudioPath
import com.localdownloader.media.readAudioTrackMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MusicSourceUiState(
    val sourceType: MusicSourceType = MusicSourceType.APP_DOWNLOADS,
    val folderUri: String? = null,
    val folderLabel: String? = null,
    val externalTracks: List<MusicLibraryTrack> = emptyList(),
    val isLoading: Boolean = false,
    val devicePermissionGranted: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class MusicSourceViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(
        MusicSourceUiState(
            sourceType = MusicSourceType.fromStorageKey(prefs.getString(KEY_SOURCE_TYPE, null)),
            folderUri = prefs.getString(KEY_FOLDER_URI, null),
            folderLabel = prefs.getString(KEY_FOLDER_LABEL, null),
            devicePermissionGranted = hasDeviceAudioPermission(),
        ),
    )
    val uiState: StateFlow<MusicSourceUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectSource(sourceType: MusicSourceType) {
        prefs.edit().putString(KEY_SOURCE_TYPE, sourceType.storageKey).apply()
        _uiState.update {
            it.copy(
                sourceType = sourceType,
                message = null,
                errorMessage = null,
            )
        }
        refresh()
    }

    fun setSelectedFolder(uri: Uri) {
        val label = uri.lastPathSegment
            ?.substringAfterLast(':')
            ?.ifBlank { null }
            ?: "Selected folder"
        prefs.edit()
            .putString(KEY_SOURCE_TYPE, MusicSourceType.SELECTED_FOLDER.storageKey)
            .putString(KEY_FOLDER_URI, uri.toString())
            .putString(KEY_FOLDER_LABEL, label)
            .apply()
        _uiState.update {
            it.copy(
                sourceType = MusicSourceType.SELECTED_FOLDER,
                folderUri = uri.toString(),
                folderLabel = label,
                message = "Music folder updated.",
                errorMessage = null,
            )
        }
        refresh()
    }

    fun refresh() {
        val sourceType = _uiState.value.sourceType
        when (sourceType) {
            MusicSourceType.APP_DOWNLOADS -> {
                _uiState.update {
                    it.copy(
                        externalTracks = emptyList(),
                        isLoading = false,
                        devicePermissionGranted = hasDeviceAudioPermission(),
                        errorMessage = null,
                    )
                }
            }

            MusicSourceType.DEVICE_AUDIO -> loadDeviceAudio()
            MusicSourceType.SELECTED_FOLDER -> loadSelectedFolder()
        }
    }

    fun dismissMessage() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }

    private fun loadDeviceAudio() {
        if (!hasDeviceAudioPermission()) {
            _uiState.update {
                it.copy(
                    externalTracks = emptyList(),
                    isLoading = false,
                    devicePermissionGranted = false,
                    errorMessage = "Allow audio permission to show music from your device.",
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    devicePermissionGranted = true,
                    errorMessage = null,
                )
            }
            val result = runCatching { queryDeviceAudio() }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { tracks ->
                        state.copy(
                            externalTracks = tracks,
                            isLoading = false,
                            message = "${tracks.size} device audio tracks loaded.",
                            errorMessage = null,
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            externalTracks = emptyList(),
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to load device audio.",
                        )
                    },
                )
            }
        }
    }

    private fun loadSelectedFolder() {
        val folderUri = _uiState.value.folderUri
        if (folderUri.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    externalTracks = emptyList(),
                    isLoading = false,
                    errorMessage = "Pick a folder to use this music source.",
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = runCatching { queryTreeAudio(Uri.parse(folderUri)) }
            _uiState.update { state ->
                result.fold(
                    onSuccess = { tracks ->
                        state.copy(
                            externalTracks = tracks,
                            isLoading = false,
                            message = "${tracks.size} folder tracks loaded.",
                            errorMessage = null,
                        )
                    },
                    onFailure = { error ->
                        state.copy(
                            externalTracks = emptyList(),
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to load the selected folder.",
                        )
                    },
                )
            }
        }
    }

    private fun queryDeviceAudio(): List<MusicLibraryTrack> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(MediaStore.Audio.Media.SIZE)
            add(MediaStore.Audio.Media.DATE_MODIFIED)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.MIME_TYPE)
            @Suppress("DEPRECATION")
            add(MediaStore.Audio.Media.DATA)
        }.toTypedArray()
        val selection = "${MediaStore.Audio.Media.IS_MUSIC}!=0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"

        return context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val modifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
            val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            @Suppress("DEPRECATION")
            val dataIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val uri = ContentUris.withAppendedId(collection, id)
                    val fileName = cursor.getStringOrNull(nameIndex)
                    val filePath = dataIndex
                        .takeIf { it >= 0 }
                        ?.let { cursor.getStringOrNull(it) }
                        ?.takeIf { File(it).exists() }
                    val title = cursor.getStringOrNull(titleIndex)
                        ?.takeUnless { it.equals("<unknown>", ignoreCase = true) }
                        ?.ifBlank { null }
                        ?: fileName?.substringBeforeLast('.')?.ifBlank { null }
                        ?: "Audio track"
                    val updatedAt = cursor.getLongOrNull(modifiedIndex)
                        ?.times(1_000L)
                        ?: System.currentTimeMillis()
                    add(
                        MusicLibraryTrack(
                            id = "device:$id",
                            title = title,
                            artist = cursor.getStringOrNull(artistIndex)
                                ?.takeUnless { it.equals("<unknown>", ignoreCase = true) },
                            album = cursor.getStringOrNull(albumIndex)
                                ?.takeUnless { it.equals("<unknown>", ignoreCase = true) },
                            playbackUri = uri.toString(),
                            filePath = filePath,
                            fileName = fileName,
                            folderName = filePath?.let { File(it).parentFile?.name },
                            displaySize = cursor.getLongOrNull(sizeIndex)?.toReadableSize().orEmpty(),
                            sizeBytes = cursor.getLongOrNull(sizeIndex),
                            updatedAtEpochMs = updatedAt,
                            durationMs = cursor.getLongOrNull(durationIndex),
                            sourceType = MusicSourceType.DEVICE_AUDIO,
                            sourceUrl = uri.toString(),
                        ),
                    )
                }
            }
        }.orEmpty()
    }

    private fun queryTreeAudio(treeUri: Uri): List<MusicLibraryTrack> {
        val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val tracks = mutableListOf<MusicLibraryTrack>()
        queryTreeChildren(
            treeUri = treeUri,
            documentId = treeDocumentId,
            folderName = _uiState.value.folderLabel ?: "Selected folder",
            tracks = tracks,
        )
        return tracks.sortedByDescending { it.updatedAtEpochMs }
    }

    private fun queryTreeChildren(
        treeUri: Uri,
        documentId: String,
        folderName: String,
        tracks: MutableList<MusicLibraryTrack>,
        depth: Int = 0,
    ) {
        if (tracks.size >= MAX_FOLDER_TRACKS || depth > MAX_FOLDER_DEPTH) return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, documentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )

        context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
            val modifiedIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

            while (cursor.moveToNext() && tracks.size < MAX_FOLDER_TRACKS) {
                val childId = cursor.getString(idIndex)
                val name = cursor.getStringOrNull(nameIndex) ?: continue
                val mimeType = cursor.getStringOrNull(mimeIndex)
                when {
                    mimeType == DocumentsContract.Document.MIME_TYPE_DIR -> {
                        queryTreeChildren(
                            treeUri = treeUri,
                            documentId = childId,
                            folderName = name,
                            tracks = tracks,
                            depth = depth + 1,
                        )
                    }

                    mimeType?.startsWith("audio/") == true || isLikelyAudioPath(name) -> {
                        val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                        val metadata = readAudioTrackMetadata(context, uri.toString())
                        tracks += MusicLibraryTrack(
                            id = "folder:${uri.toString().stableShortHash()}",
                            title = metadata.title ?: name.substringBeforeLast('.').ifBlank { name },
                            artist = metadata.artist,
                            album = metadata.album,
                            playbackUri = uri.toString(),
                            fileName = name,
                            folderName = folderName,
                            displaySize = cursor.getLongOrNull(sizeIndex)?.toReadableSize().orEmpty(),
                            sizeBytes = cursor.getLongOrNull(sizeIndex),
                            updatedAtEpochMs = cursor.getLongOrNull(modifiedIndex)
                                ?: System.currentTimeMillis(),
                            durationMs = metadata.durationMs,
                            sourceType = MusicSourceType.SELECTED_FOLDER,
                            sourceLabel = _uiState.value.folderLabel ?: MusicSourceType.SELECTED_FOLDER.label,
                            sourceUrl = uri.toString(),
                        )
                    }
                }
            }
        }
    }

    private fun hasDeviceAudioPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun String.stableShortHash(): String {
        return Integer.toHexString(hashCode())
    }

    private fun Long.toReadableSize(): String {
        if (this <= 0L) return ""
        val kib = 1024.0
        val mib = kib * 1024.0
        val gib = mib * 1024.0
        return when {
            this >= gib -> "%.1f GB".format(this / gib)
            this >= mib -> "%.1f MB".format(this / mib)
            this >= kib -> "%.1f KB".format(this / kib)
            else -> "$this B"
        }
    }

    private companion object {
        const val PREFS_NAME = "music_source_preferences"
        const val KEY_SOURCE_TYPE = "source_type"
        const val KEY_FOLDER_URI = "folder_uri"
        const val KEY_FOLDER_LABEL = "folder_label"
        const val MAX_FOLDER_DEPTH = 6
        const val MAX_FOLDER_TRACKS = 5_000
    }
}

private fun android.database.Cursor.getStringOrNull(index: Int): String? {
    if (index < 0 || isNull(index)) return null
    return getString(index)
}

private fun android.database.Cursor.getLongOrNull(index: Int): Long? {
    if (index < 0 || isNull(index)) return null
    return getLong(index)
}
