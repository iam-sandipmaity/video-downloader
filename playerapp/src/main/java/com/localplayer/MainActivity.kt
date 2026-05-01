package com.localplayer

import android.app.PictureInPictureParams
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.localplayer.model.PlayerMedia
import com.localplayer.ui.StandalonePlayerApp
import com.localplayer.ui.theme.LocalPlayerTheme

class MainActivity : ComponentActivity() {
    private var externalMedia by mutableStateOf<PlayerMedia?>(null)
    private var pictureInPictureAllowed by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        externalMedia = buildExternalMedia(intent)
        setContent {
            LocalPlayerTheme {
                StandalonePlayerApp(
                    externalMedia = externalMedia,
                    onExternalMediaHandled = { externalMedia = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalMedia = buildExternalMedia(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPictureInPictureIfPossible()
    }

    fun updatePictureInPictureAllowed(enabled: Boolean) {
        pictureInPictureAllowed = enabled
    }

    fun enterPictureInPictureIfPossible() {
        if (!pictureInPictureAllowed || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build(),
            )
        }
    }

    private fun buildExternalMedia(intent: Intent?): PlayerMedia? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        val uri = intent.data ?: return null
        return resolvePlayerMedia(
            uri = uri,
            typeHint = intent.type,
            intentFlags = intent.flags,
        )
    }

    private fun resolvePlayerMedia(
        uri: Uri,
        typeHint: String? = null,
        intentFlags: Int = 0,
    ): PlayerMedia {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val takeFlags = intentFlags and
                (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            if (takeFlags != 0) {
                runCatching {
                    contentResolver.takePersistableUriPermission(uri, takeFlags)
                }
            }
        }

        val mimeType = typeHint ?: contentResolver.getType(uri)
        val title = queryDisplayName(contentResolver, uri)
            ?: uri.lastPathSegment?.substringAfterLast('/')
            ?: "Media file"

        return PlayerMedia(
            id = uri.toString(),
            title = title,
            uriString = uri.toString(),
            mimeType = mimeType,
        )
    }

    private fun queryDisplayName(contentResolver: ContentResolver, uri: Uri): String? {
        return runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (columnIndex == -1) null else cursor.getString(columnIndex)
            }
        }.getOrNull()
    }
}
