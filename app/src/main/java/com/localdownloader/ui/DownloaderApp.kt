package com.localdownloader.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.localdownloader.domain.models.YoutubeAuthBundle
import com.localdownloader.ui.screens.CompressScreen
import com.localdownloader.ui.screens.ConvertScreen
import com.localdownloader.ui.screens.DownloadHistoryScreen
import com.localdownloader.ui.screens.DownloadManagerScreen
import com.localdownloader.ui.screens.HomeScreen
import com.localdownloader.ui.screens.SettingsScreen
import com.localdownloader.utils.FileUtils
import com.localdownloader.viewmodel.DownloadViewModel
import com.localdownloader.viewmodel.FormatViewModel
import com.localdownloader.viewmodel.MediaToolsViewModel
import kotlinx.serialization.json.Json

// SVG path data (24×24 viewBox)
private object NavIcons {
    const val HOME = "M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z"
    const val QUEUE = "M4 6h16v2H4zm0 5h16v2H4zm0 5h16v2H4z"
    const val HISTORY = "M13 3a9 9 0 0 0-9 9H1l3.89 3.89.07.14L9 12H6a7 7 0 1 1 2.05 4.95L6.62 18.38A9 9 0 1 0 13 3zm-1 5v5l4.28 2.54.72-1.21L13 12V8z"
    const val SETTINGS = "M19.14 12.94c.04-.3.06-.61.06-.94s-.02-.64-.07-.94l2.03-1.58a.49.49 0 0 0 .12-.61l-1.92-3.32a.49.49 0 0 0-.59-.22l-2.39.96a7.07 7.07 0 0 0-1.62-.94l-.36-2.54a.484.484 0 0 0-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96a.48.48 0 0 0-.59.22L2.74 8.87a.47.47 0 0 0 .12.61l2.03 1.58c-.05.3-.07.62-.07.94s.02.64.07.94l-2.03 1.58a.47.47 0 0 0-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.37 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.57 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32a.47.47 0 0 0-.12-.61l-2.01-1.58zM12 15.6a3.6 3.6 0 1 1 0-7.2 3.6 3.6 0 0 1 0 7.2z"
    const val CONVERT = "M6.99 11L3 15l3.99 4v-3H14v-2H6.99v-3zM21 9l-3.99-4v3H10v2h7.01v3L21 9z"
    const val COMPRESS = "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 14l-5-5h3V8h4v4h3l-5 5z"
}

@Composable
fun DownloaderApp(
    onDarkThemeChanged: ((Boolean) -> Unit)? = null,
    onDarkThemeUpdated: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val formatViewModel: FormatViewModel = hiltViewModel()
    val downloadViewModel: DownloadViewModel = hiltViewModel()
    val mediaToolsViewModel: MediaToolsViewModel = hiltViewModel()
    val context = LocalContext.current
    val fileUtils = remember(context) { FileUtils(context) }
    val localJson = remember {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    // File picker for Convert screen
    val convertFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            val path = com.localdownloader.utils.FileUtils.getRealPathFromUri(context, it)
                ?: it.toString()
            mediaToolsViewModel.onConvertInputPathChanged(path)
        }
    }

    // File picker for Compress screen
    val compressFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            val path = com.localdownloader.utils.FileUtils.getRealPathFromUri(context, it)
                ?: it.toString()
            mediaToolsViewModel.onCompressInputPathChanged(path)
        }
    }

    val youtubeCookiesPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            runCatching {
                fileUtils.importDocumentToInternalFile(
                    uri = it,
                    subDirectoryName = "auth",
                    targetFileName = "youtube-cookies.txt",
                )
            }.onSuccess { path ->
                formatViewModel.onYoutubeCookiesImported(path)
            }.onFailure { error ->
                formatViewModel.onYoutubeAuthImportFailed(
                    error.message ?: "Unable to import cookies file.",
                )
            }
        }
    }

    val youtubeAuthBundlePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            runCatching {
                val bundleText = fileUtils.readTextFromUri(it)
                val bundle = localJson.decodeFromString<YoutubeAuthBundle>(bundleText)
                val cookiesContent = bundle.cookiesContent?.takeIf { content -> content.isNotBlank() }
                    ?: throw IllegalStateException(
                        "This auth bundle is missing inline cookies. Regenerate it with the latest desktop helper or import cookies.txt manually.",
                    )
                val poToken = bundle.poToken?.trim().orEmpty()
                if (poToken.isBlank()) {
                    throw IllegalStateException(
                        "This auth bundle is missing the PO token. Regenerate it with the latest desktop helper or paste the token manually.",
                    )
                }
                val cookiesPath = fileUtils.writeTextToInternalFile(
                    subDirectoryName = "auth",
                    targetFileName = "youtube-cookies.txt",
                    content = cookiesContent,
                )
                bundle to cookiesPath
            }.onSuccess { (bundle, cookiesPath) ->
                formatViewModel.applyYoutubeAuthBundle(bundle, cookiesPath)
            }.onFailure { error ->
                formatViewModel.onYoutubeAuthImportFailed(
                    error.message ?: "Unable to import YouTube auth bundle.",
                )
            }
        }
    }

    val formatState by formatViewModel.uiState.collectAsStateWithLifecycle()
    val downloadState by downloadViewModel.uiState.collectAsStateWithLifecycle()
    val mediaToolsState by mediaToolsViewModel.uiState.collectAsStateWithLifecycle()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    LaunchedEffect(formatState.isDarkTheme) {
        onDarkThemeUpdated?.invoke(formatState.isDarkTheme)
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            val activeColor = MaterialTheme.colorScheme.primary
            val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

            NavigationBar {
                AppDestination.entries.forEach { destination ->
                    val selected = currentRoute == destination.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                            }
                        },
                        icon = {
                            NavIcon(
                                svgPath = destination.svgPath,
                                tint = if (selected) activeColor else inactiveColor,
                            )
                        },
                        label = null,
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppDestination.Home.route) {
                HomeScreen(
                    uiState = formatState,
                    onUrlChanged = formatViewModel::onUrlChanged,
                    onAnalyzeClicked = formatViewModel::analyzeUrl,
                    onQualityChanged = formatViewModel::onQualityChanged,
                    onStreamTypeChanged = formatViewModel::onStreamTypeChanged,
                    onFormatSelectorChanged = formatViewModel::onFormatSelectorChanged,
                    onContainerChanged = formatViewModel::onContainerChanged,
                    onAudioFormatChanged = formatViewModel::onAudioFormatChanged,
                    onAudioBitrateChanged = formatViewModel::onAudioBitrateChanged,
                    onDownloadSubtitlesChanged = formatViewModel::onDownloadSubtitlesChanged,
                    onEmbedMetadataChanged = formatViewModel::onEmbedMetadataChanged,
                    onEmbedThumbnailChanged = formatViewModel::onEmbedThumbnailChanged,
                    onWriteThumbnailChanged = formatViewModel::onWriteThumbnailChanged,
                    onPlaylistEnabledChanged = formatViewModel::onPlaylistEnabledChanged,
                    onOutputTemplateChanged = formatViewModel::onOutputTemplateChanged,
                    onQueueDownloadClicked = formatViewModel::queueDownload,
                )
            }
            composable(AppDestination.Queue.route) {
                DownloadManagerScreen(
                    uiState = downloadState,
                    onPause = downloadViewModel::pause,
                    onResume = downloadViewModel::resume,
                    onCancel = downloadViewModel::cancel,
                    onToggleDebug = downloadViewModel::toggleDebug,
                )
            }
            composable(AppDestination.History.route) {
                DownloadHistoryScreen(tasks = downloadState.tasks)
            }
            composable(AppDestination.Convert.route) {
                ConvertScreen(
                    uiState = mediaToolsState,
                    onInputPathChanged = mediaToolsViewModel::onConvertInputPathChanged,
                    onOutputFormatChanged = mediaToolsViewModel::onConvertOutputFormatChanged,
                    onAudioBitrateChanged = mediaToolsViewModel::onConvertAudioBitrateChanged,
                    onVideoBitrateChanged = mediaToolsViewModel::onConvertVideoBitrateChanged,
                    onConvertClicked = mediaToolsViewModel::startConvert,
                    onBrowseFile = { convertFilePicker.launch(arrayOf("*/*")) },
                )
            }
            composable(AppDestination.Compress.route) {
                CompressScreen(
                    uiState = mediaToolsState,
                    onInputPathChanged = mediaToolsViewModel::onCompressInputPathChanged,
                    onMaxHeightChanged = mediaToolsViewModel::onCompressMaxHeightChanged,
                    onVideoBitrateChanged = mediaToolsViewModel::onCompressVideoBitrateChanged,
                    onAudioBitrateChanged = mediaToolsViewModel::onCompressAudioBitrateChanged,
                    onCompressClicked = mediaToolsViewModel::startCompress,
                    onBrowseFile = { compressFilePicker.launch(arrayOf("*/*")) },
                )
            }
            composable(AppDestination.Settings.route) {
                SettingsScreen(
                    uiState = formatState,
                    onDarkThemeChanged = { enabled ->
                        formatViewModel.toggleDarkTheme(enabled)
                        onDarkThemeChanged?.invoke(enabled)
                    },
                    onOutputTemplateChanged = formatViewModel::onOutputTemplateChanged,
                    onContainerChanged = formatViewModel::onContainerChanged,
                    onEmbedMetadataChanged = formatViewModel::onEmbedMetadataChanged,
                    onEmbedThumbnailChanged = formatViewModel::onEmbedThumbnailChanged,
                    onSaveClicked = formatViewModel::saveSettings,
                    onYoutubeAuthEnabledChanged = formatViewModel::onYoutubeAuthEnabledChanged,
                    onYoutubePoTokenChanged = formatViewModel::onYoutubePoTokenChanged,
                    onYoutubePoTokenClientHintChanged = formatViewModel::onYoutubePoTokenClientHintChanged,
                    onYoutubeCookiesPathChanged = formatViewModel::onYoutubeCookiesPathChanged,
                    onPickYoutubeCookies = { youtubeCookiesPicker.launch(arrayOf("text/plain", "*/*")) },
                    onPickYoutubeAuthBundle = { youtubeAuthBundlePicker.launch(arrayOf("application/json", "text/plain", "*/*")) },
                )
            }
        }
    }
}

@Composable
private fun NavIcon(svgPath: String, tint: Color, modifier: Modifier = Modifier) {
    val path = remember(svgPath) { PathParser().parsePathString(svgPath).toPath() }
    Canvas(modifier = modifier.size(24.dp)) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            drawPath(path = path, color = tint)
        }
    }
}

private enum class AppDestination(
    val route: String,
    val svgPath: String,
) {
    Home(route = "home", svgPath = NavIcons.HOME),
    Queue(route = "queue", svgPath = NavIcons.QUEUE),
    History(route = "history", svgPath = NavIcons.HISTORY),
    Convert(route = "convert", svgPath = NavIcons.CONVERT),
    Compress(route = "compress", svgPath = NavIcons.COMPRESS),
    Settings(route = "settings", svgPath = NavIcons.SETTINGS),
}
