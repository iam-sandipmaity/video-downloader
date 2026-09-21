package com.localdownloader.ui.screens.settings

import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.localdownloader.ui.components.PreferenceItem
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceSubtitle

data class OpenSourceCredit(
    val title: String,
    val version: String,
    val description: String,
    val url: String,
)

@Composable
fun CreditsSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val openSourceCredits = listOf(
        OpenSourceCredit(
            title = "Android Jetpack Compose",
            version = "v2026.05.00",
            description = "Declarative UI toolkit for building native Android interfaces.",
            url = "https://developer.android.com/jetpack/compose",
        ),
        OpenSourceCredit(
            title = "Kotlin & Coroutines",
            version = "v2.3.21 / v1.11.0",
            description = "Modern application language and asynchronous flow coordination.",
            url = "https://kotlinlang.org/",
        ),
        OpenSourceCredit(
            title = "yt-dlp",
            version = "Managed Runtime",
            description = "Powerful open-source media extractor and downloader engine.",
            url = "https://github.com/yt-dlp/yt-dlp",
        ),
        OpenSourceCredit(
            title = "FFmpeg",
            version = "Managed Runtime",
            description = "Comprehensive multimedia framework for transcoding, merging, and compression.",
            url = "https://ffmpeg.org/",
        ),
        OpenSourceCredit(
            title = "youtubedl-android",
            version = "v0.18.1",
            description = "Android wrapper runtime for yt-dlp and FFmpeg execution.",
            url = "https://github.com/yausername/youtubedl-android",
        ),
        OpenSourceCredit(
            title = "Media3 ExoPlayer",
            version = "v1.10.1",
            description = "High-performance video and audio playback engine.",
            url = "https://developer.android.com/media/media3",
        ),
        OpenSourceCredit(
            title = "Coil",
            version = "v2.7.0",
            description = "Fast, lightweight image and SVG loading pipeline for Android.",
            url = "https://coil-kt.github.io/coil/",
        ),
        OpenSourceCredit(
            title = "Hilt Dependency Injection",
            version = "v2.59.2",
            description = "Standard dependency injection framework for Android.",
            url = "https://dagger.dev/hilt/",
        ),
        OpenSourceCredit(
            title = "Room Database",
            version = "v2.8.4",
            description = "Robust SQLite object mapping for queue and history persistence.",
            url = "https://developer.android.com/jetpack/androidx/releases/room",
        ),
        OpenSourceCredit(
            title = "AndroidX WorkManager",
            version = "v2.11.2",
            description = "Reliable background scheduling and download task management.",
            url = "https://developer.android.com/topic/libraries/architecture/workmanager",
        ),
        OpenSourceCredit(
            title = "Kotlin Serialization",
            version = "v1.11.0",
            description = "Cross-platform JSON serialization for yt-dlp metadata.",
            url = "https://github.com/Kotlin/kotlinx.serialization",
        ),
        OpenSourceCredit(
            title = "Material 3 Components",
            version = "v1.14.0",
            description = "Google Material 3 design system components, color schemes, and motion.",
            url = "https://m3.material.io/",
        ),
    )

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    PreferencePageScaffold(
        title = "Open Source Credits",
        onBack = onBack,
        modifier = modifier,
    ) {
        item {
            PreferenceSubtitle(text = "CORE ENGINES & RUNTIMES")
        }
        item {
            val credit = openSourceCredits[2] // yt-dlp
            PreferenceItem(
                icon = Icons.Rounded.Code,
                title = credit.title,
                description = "${credit.version} • ${credit.description}",
                onClick = { openUrl(credit.url) },
            )
        }
        item {
            val credit = openSourceCredits[3] // FFmpeg
            PreferenceItem(
                icon = Icons.Rounded.Code,
                title = credit.title,
                description = "${credit.version} • ${credit.description}",
                onClick = { openUrl(credit.url) },
            )
        }
        item {
            val credit = openSourceCredits[4] // youtubedl-android
            PreferenceItem(
                icon = Icons.Rounded.Code,
                title = credit.title,
                description = "${credit.version} • ${credit.description}",
                onClick = { openUrl(credit.url) },
            )
        }

        item {
            PreferenceSubtitle(text = "UI & MEDIA LIBRARIES")
        }
        item {
            val credit = openSourceCredits[0] // Jetpack Compose
            PreferenceItem(
                icon = Icons.Rounded.Favorite,
                title = credit.title,
                description = "${credit.version} • ${credit.description}",
                onClick = { openUrl(credit.url) },
            )
        }
        item {
            val credit = openSourceCredits[5] // Media3 ExoPlayer
            PreferenceItem(
                icon = Icons.Rounded.Favorite,
                title = credit.title,
                description = "${credit.version} • ${credit.description}",
                onClick = { openUrl(credit.url) },
            )
        }
        item {
            val credit = openSourceCredits[6] // Coil
            PreferenceItem(
                icon = Icons.Rounded.Favorite,
                title = credit.title,
                description = "${credit.version} • ${credit.description}",
                onClick = { openUrl(credit.url) },
            )
        }
        item {
            val credit = openSourceCredits[11] // Material 3
            PreferenceItem(
                icon = Icons.Rounded.Favorite,
                title = credit.title,
                description = "${credit.version} • ${credit.description}",
                onClick = { openUrl(credit.url) },
            )
        }

        item {
            PreferenceSubtitle(text = "ARCHITECTURE & INFRASTRUCTURE")
        }
        item {
            val credit = openSourceCredits[1] // Kotlin & Coroutines
            PreferenceItem(
                icon = Icons.Rounded.Description,
                title = credit.title,
                description = "${credit.version} • ${credit.description}",
                onClick = { openUrl(credit.url) },
            )
        }
        item {
            val credit = openSourceCredits[7] // Hilt
            PreferenceItem(
                icon = Icons.Rounded.Description,
                title = credit.title,
                description = "${credit.version} • ${credit.description}",
                onClick = { openUrl(credit.url) },
            )
        }
        item {
            val credit = openSourceCredits[8] // Room Database
            PreferenceItem(
                icon = Icons.Rounded.Description,
                title = credit.title,
                description = "${credit.version} • ${credit.description}",
                onClick = { openUrl(credit.url) },
            )
        }
        item {
            val credit = openSourceCredits[9] // WorkManager
            PreferenceItem(
                icon = Icons.Rounded.Description,
                title = credit.title,
                description = "${credit.version} • ${credit.description}",
                onClick = { openUrl(credit.url) },
            )
        }
        item {
            val credit = openSourceCredits[10] // Kotlin Serialization
            PreferenceItem(
                icon = Icons.Rounded.Description,
                title = credit.title,
                description = "${credit.version} • ${credit.description}",
                onClick = { openUrl(credit.url) },
            )
        }
    }
}
