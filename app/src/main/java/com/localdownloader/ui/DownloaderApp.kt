package com.localdownloader.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.padding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.localdownloader.domain.models.YoutubeAuthBundle
import com.localdownloader.ui.screens.BrowserScreen
import com.localdownloader.ui.screens.CompressScreen
import com.localdownloader.ui.screens.ConvertScreen
import com.localdownloader.ui.screens.DownloadHistoryScreen
import com.localdownloader.ui.screens.HelpScreen
import com.localdownloader.ui.screens.PlayerScreen
import com.localdownloader.ui.screens.ProgressScreen
import com.localdownloader.ui.screens.SettingsScreen
import com.localdownloader.ui.screens.VideoScreen
import com.localdownloader.utils.FileUtils
import com.localdownloader.viewmodel.DownloadViewModel
import com.localdownloader.viewmodel.FormatViewModel
import com.localdownloader.viewmodel.MediaToolsViewModel
import kotlinx.serialization.json.Json

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
    var cacheSize by remember { mutableStateOf(0L) }
    val localJson = remember {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    val convertFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let {
            val path = com.localdownloader.utils.FileUtils.getRealPathFromUri(context, it)
                ?: it.toString()
            mediaToolsViewModel.onConvertInputPathChanged(path)
        }
    }

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
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val currentRoute = currentDestination?.route

    LaunchedEffect(formatState.isDarkTheme) {
        onDarkThemeUpdated?.invoke(formatState.isDarkTheme)
    }

    // Update cache size periodically
    LaunchedEffect(currentRoute) {
        cacheSize = fileUtils.getCacheSize()
    }

    val primaryDestinations = remember {
        listOf(
            PrimaryDestination(
                route = Routes.Browser,
                label = "Browser",
                icon = { Icon(Icons.Outlined.TravelExplore, contentDescription = null) },
            ),
            PrimaryDestination(
                route = Routes.Progress,
                label = "Progress",
                icon = { Icon(Icons.Outlined.CloudDownload, contentDescription = null) },
            ),
            PrimaryDestination(
                route = Routes.Video,
                label = "Video",
                icon = { Icon(Icons.Outlined.PlayCircle, contentDescription = null) },
            ),
        )
    }

    val showBottomBar = currentRoute in primaryDestinations.map { it.route }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    primaryDestinations.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                }
                            },
                            icon = destination.icon,
                            label = { Text(destination.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Browser,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.Browser) {
                BrowserScreen(
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
                    onOpenHistory = { navController.navigate(Routes.History) },
                    onOpenCompress = { navController.navigate(Routes.Compress) },
                    onOpenConvert = { navController.navigate(Routes.Convert) },
                    onOpenSettings = { navController.navigate(Routes.Settings) },
                    onOpenHelp = { navController.navigate(Routes.Help) },
                    onDarkThemeChanged = { enabled ->
                        formatViewModel.toggleDarkTheme(enabled)
                        onDarkThemeChanged?.invoke(enabled)
                    },
                    isDownloadButtonEnabled = formatViewModel.isDownloadButtonEnabled(),
                )
            }
            composable(Routes.Progress) {
                ProgressScreen(
                    uiState = downloadState,
                    onPause = downloadViewModel::pause,
                    onResume = downloadViewModel::resume,
                    onCancel = downloadViewModel::cancel,
                    onToggleDebug = downloadViewModel::toggleDebug,
                )
            }
            composable(Routes.Video) {
                VideoScreen(
                    uiState = downloadState,
                    onOpenPlayer = { taskId -> navController.navigate("${Routes.Player}/$taskId") },
                    onRename = downloadViewModel::renameDownloadedFile,
                    onDelete = downloadViewModel::deleteDownloadedFile,
                    onDismissMessage = downloadViewModel::dismissMessage,
                )
            }
            composable(Routes.History) {
                DownloadHistoryScreen(
                    tasks = downloadState.tasks,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.Convert) {
                ConvertScreen(
                    uiState = mediaToolsState,
                    onInputPathChanged = mediaToolsViewModel::onConvertInputPathChanged,
                    onOutputFormatChanged = mediaToolsViewModel::onConvertOutputFormatChanged,
                    onAudioBitrateChanged = mediaToolsViewModel::onConvertAudioBitrateChanged,
                    onVideoBitrateChanged = mediaToolsViewModel::onConvertVideoBitrateChanged,
                    onConvertClicked = mediaToolsViewModel::startConvert,
                    onBrowseFile = { convertFilePicker.launch(arrayOf("*/*")) },
                    onConversionPresetSelected = mediaToolsViewModel::applyConversionPreset,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.Compress) {
                CompressScreen(
                    uiState = mediaToolsState,
                    onInputPathChanged = mediaToolsViewModel::onCompressInputPathChanged,
                    onResolutionPresetSelected = mediaToolsViewModel::onCompressResolutionPresetSelected,
                    onVideoBitratePresetSelected = mediaToolsViewModel::onCompressVideoBitratePresetSelected,
                    onAudioBitratePresetSelected = mediaToolsViewModel::onCompressAudioBitratePresetSelected,
                    onMaxHeightChanged = mediaToolsViewModel::onCompressMaxHeightChanged,
                    onVideoBitrateChanged = mediaToolsViewModel::onCompressVideoBitrateChanged,
                    onAudioBitrateChanged = mediaToolsViewModel::onCompressAudioBitrateChanged,
                    onCompressClicked = mediaToolsViewModel::startCompress,
                    onBrowseFile = { compressFilePicker.launch(arrayOf("*/*")) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.Settings) {
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
                    onClearCache = {
                        fileUtils.clearCache()
                        cacheSize = 0L
                    },
                    cacheSize = cacheSize,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.Help) {
                HelpScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "${Routes.Player}/{taskId}",
                arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val taskId = backStackEntry.arguments?.getString("taskId")
                val task = downloadState.tasks.firstOrNull { it.id == taskId }
                PlayerScreen(
                    task = task,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private object Routes {
    const val Browser = "browser"
    const val Progress = "progress"
    const val Video = "video"
    const val History = "history"
    const val Convert = "convert"
    const val Compress = "compress"
    const val Settings = "settings"
    const val Help = "help"
    const val Player = "player"
}

private data class PrimaryDestination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
)

