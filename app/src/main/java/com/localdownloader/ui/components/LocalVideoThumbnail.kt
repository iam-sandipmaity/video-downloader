package com.localdownloader.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LocalVideoThumbnail(
    filePath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallbackContent: (@Composable BoxScope.() -> Unit)? = null,
) {
    val context = LocalContext.current
    val bitmap = produceState<Bitmap?>(initialValue = null, key1 = filePath) {
        value = withContext(Dispatchers.IO) {
            if (filePath.isNullOrBlank()) {
                null
            } else {
                // 1. Check for companion sidecar thumbnail image (.png, .jpg, .jpeg, .webp)
                val sidecarBitmap = runCatching {
                    val primaryFile = java.io.File(filePath)
                    if (primaryFile.exists()) {
                        val parent = primaryFile.parentFile
                        val stem = primaryFile.nameWithoutExtension
                        val sidecarImage = parent?.listFiles()?.firstOrNull { candidate ->
                            candidate.isFile &&
                                candidate.nameWithoutExtension == stem &&
                                candidate.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp") &&
                                candidate.length() > 0L
                        }
                        if (sidecarImage != null) {
                            val options = BitmapFactory.Options().apply {
                                inSampleSize = 1
                            }
                            BitmapFactory.decodeFile(sidecarImage.absolutePath, options)
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }.getOrNull()

                if (sidecarBitmap != null) {
                    sidecarBitmap
                } else {
                    // 2. Fall back to embedded picture or video frame extraction
                    runCatching {
                        val retriever = MediaMetadataRetriever()
                        try {
                            if (filePath.startsWith("content://", ignoreCase = true)) {
                                retriever.setDataSource(context, filePath.toUri())
                            } else {
                                retriever.setDataSource(filePath)
                            }
                            retriever.embeddedPicture?.let { bytes ->
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } ?: retriever.frameAtTime
                        } finally {
                            retriever.release()
                        }
                    }.getOrNull()
                }
            }
        }
    }.value

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            if (fallbackContent != null) {
                fallbackContent()
            } else {
                Icon(
                    imageVector = Icons.Outlined.PlayCircle,
                    contentDescription = contentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                )
            }
        }
    }
}
