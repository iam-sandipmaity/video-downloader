package com.localplayer.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localplayer.model.PlayerMedia
import com.localplayer.ui.screens.PlayerScreen
import com.localplayer.viewmodel.PlayerViewModel

@Composable
fun StandalonePlayerApp(
    externalMedia: PlayerMedia?,
    onExternalMediaHandled: () -> Unit,
) {
    val context = LocalContext.current
    val playerViewModel: PlayerViewModel = viewModel()
    var selectedMedia by rememberSaveable(stateSaver = PlayerMediaSaver) { mutableStateOf<PlayerMedia?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
        selectedMedia = resolvePickedMedia(uri = uri, mimeType = context.contentResolver.getType(uri))
    }

    LaunchedEffect(externalMedia) {
        if (externalMedia != null) {
            selectedMedia = externalMedia
            onExternalMediaHandled()
        }
    }

    if (selectedMedia == null) {
        MediaPickerHome(
            onOpenMedia = {
                pickerLauncher.launch(arrayOf("video/*", "audio/*"))
            },
        )
    } else {
        PlayerScreen(
            media = selectedMedia,
            playerViewModel = playerViewModel,
            onBack = {
                playerViewModel.bindMedia(null)
                selectedMedia = null
            },
        )
    }
}

@Composable
private fun MediaPickerHome(
    onOpenMedia: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF120F17),
                        Color(0xFF1D1624),
                        Color(0xFF2E211A),
                    ),
                ),
            ),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            shape = RoundedCornerShape(32.dp),
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Standalone Player",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Open a local audio or video file and use the same swipe gestures, PiP, subtitle controls, and track switching from the downloader app player.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FeatureRow(Icons.Outlined.VideoFile, "Video playback with fullscreen and Picture-in-Picture")
                        FeatureRow(Icons.Outlined.AudioFile, "Background audio support for music and audio-first files")
                        FeatureRow(Icons.Outlined.PlayCircle, "Swipe seek, brightness, volume, and double-tap controls")
                    }
                }
                Button(onClick = onOpenMedia) {
                    Text("Open media")
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun resolvePickedMedia(uri: Uri, mimeType: String?): PlayerMedia {
    val title = uri.lastPathSegment?.substringAfterLast('/') ?: "Media file"
    return PlayerMedia(
        id = uri.toString(),
        title = title,
        uriString = uri.toString(),
        mimeType = mimeType,
    )
}

private val PlayerMediaSaver = androidx.compose.runtime.saveable.Saver<PlayerMedia?, List<String?>>(
    save = { media ->
        media?.let { listOf(it.id, it.title, it.uriString, it.mimeType) }
    },
    restore = { restored ->
        if (restored.size < 3) {
            null
        } else {
            PlayerMedia(
                id = restored[0].orEmpty(),
                title = restored[1].orEmpty(),
                uriString = restored[2].orEmpty(),
                mimeType = restored.getOrNull(3),
            )
        }
    },
)
