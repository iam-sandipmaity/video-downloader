package com.localdownloader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Help") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── Downloads Section ───────────────────────────────────────
            HelpSectionHeader("Downloads")
            HelpCard(
                title = "How downloads work",
                body = "Paste a supported video URL (YouTube, Vimeo, Dailymotion, etc.), analyze it, choose your preferred format and quality from the options sheet, then queue the download. The app uses yt-dlp in the background to fetch the media.",
            )
            HelpCard(
                title = "Where downloads are saved",
                body = "All downloaded files are saved to your device's public Downloads folder in the LocalDownloader subfolder. You can access them using any file manager app.",
            )
            HelpCard(
                title = "Supported websites",
                body = "The app supports thousands of websites via yt-dlp including YouTube, Vimeo, Dailymotion, Twitter/X, Facebook, Instagram, TikTok, and many more. Some features may require YouTube authentication for age-restricted content.",
            )

            // ── Converter Section ───────────────────────────────────────
            HelpSectionHeader("Converter")
            HelpCard(
                title = "How to convert files",
                body = "Go to the Converter tab, tap 'Select media file from device' to pick a video or audio file, choose an output format preset (like 'High quality video' or 'Audio only'), and tap 'Convert file'. The conversion runs locally using FFmpeg.",
            )
            HelpCard(
                title = "Supported output formats",
                body = "Video: MP4, MKV, AVI, FLV, MOV\nAudio: MP3, M4A, AAC, WAV, FLAC, Opus, OGG\n\nNote: WebM video output is limited as the bundled FFmpeg doesn't support VP8/VP9 encoders.",
            )
            HelpCard(
                title = "Where converted files are saved",
                body = "Converted files are automatically copied to your public Downloads/LocalDownloader folder so you can find them easily in any file manager.",
            )

            // ── Compressor Section ───────────────────────────────────────
            HelpSectionHeader("Compressor")
            HelpCard(
                title = "How to compress files",
                body = "Go to the Compressor tab, tap 'Select media file from device' to pick a video, choose a resolution preset (like 480p or 720p), select a video bitrate preset, and tap 'Compress file'. You can also adjust audio quality independently.",
            )
            HelpCard(
                title = "Compression settings explained",
                body = "• Resolution: Lower values like 360p or 480p produce smaller files\n• Video bitrate: Lower values = smaller files but may reduce quality\n• Audio bitrate: 128kbps is good for most cases, 320kbps for high quality\n• Presets like 'Small', 'Medium', 'High' combine these settings for you",
            )
            HelpCard(
                title = "Where compressed files are saved",
                body = "Compressed files are automatically copied to your public Downloads/LocalDownloader folder so you can find them easily in any file manager. The filename will have '_compressed' appended.",
            )

            // ── Navigation Section ───────────────────────────────────────
            HelpSectionHeader("Navigation")
            HelpCard(
                title = "Browser tab",
                body = "The main tab for entering URLs and managing downloads. Quick links provide shortcuts to popular video sites.",
            )
            HelpCard(
                title = "Progress tab",
                body = "Shows active, queued, and paused downloads. Failed or cancelled items remain accessible through the History option in the menu.",
            )
            HelpCard(
                title = "Video tab",
                body = "Displays completed downloads with thumbnails. You can play, share, rename, or delete files from here.",
            )

            // ── Settings Section ────────────────────────────────────────
            HelpSectionHeader("Settings")
            HelpCard(
                title = "Dark mode",
                body = "Toggle dark/light theme from either the Browser menu or the Settings screen. Your preference is saved automatically.",
            )
            HelpCard(
                title = "YouTube authentication",
                body = "For age-restricted YouTube videos, you may need to import cookies or a YouTube authentication token. Go to Settings > YouTube Auth for instructions. This is optional and only needed for restricted content.",
            )
            HelpCard(
                title = "Default download location",
                body = "All media (downloads, conversions, compressions) is saved to the public Downloads/LocalDownloader folder. This ensures files remain accessible even if you uninstall the app.",
            )

            // ── Troubleshooting Section ─────────────────────────────────
            HelpSectionHeader("Troubleshooting")
            HelpCard(
                title = "Download failed",
                body = "• Check your internet connection\n• Verify the URL is valid and not blocked\n• Try a different format/quality option\n• For YouTube, try enabling YouTube authentication in settings\n• Check if the video is available in your region",
            )
            HelpCard(
                title = "Conversion/Compression failed",
                body = "• Ensure the input file is not corrupted\n• Check you have enough storage space\n• Try a different output format\n• Large files may take longer to process",
            )
            HelpCard(
                title = "Cannot find output files",
                body = "All processed files are saved to Downloads/LocalDownloader folder. If you can't see them, try refreshing your file manager or restart the app. On Android 11+, you may need to grant file manager permission to see all files.",
            )

            // ── About Section ───────────────────────────────────────────
            HelpSectionHeader("About")
            HelpCard(
                title = "Powered by yt-dlp & FFmpeg",
                body = "This app uses yt-dlp for video downloading and FFmpeg for media conversion and compression. These are powerful open-source tools that support thousands of formats and websites.",
            )

            Text(
                text = "For more help, visit the GitHub repository.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun HelpSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun HelpCard(
    title: String,
    body: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}