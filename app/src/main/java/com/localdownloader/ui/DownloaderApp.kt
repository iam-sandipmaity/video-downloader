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
import com.localdownloader.domain.models.DownloadStatus
import com.localdownloader.domain.models.YoutubeAuthBundle
import com.localdownloader.ui.screens.BrowserScreen
import com.localdownloader.ui.screens.CompressScreen
import com.localdownloader.ui.screens.ConvertScreen
import com.localdownloader.ui.screens.DownloadHistoryScreen
import com.localdownloader.ui.screens.ExternalPreviewMode
import com.localdownloader.ui.screens.ExternalPreviewScreen
import com.localdownloader.ui.screens.HelpScreen
import com.localdownloader.ui.screens.PlayerScreen
import com.localdownloader.ui.screens.ProgressScreen
import com.localdownloader.ui.screens.SettingsScreen
import com.localdownloader.ui.screens.VideoScreen
import com.localdownloader.ui.model.ExternalOpenRequest
import com.localdownloader.utils.FileUtils
import com.localdownloader.viewmodel.DownloadViewModel
import com.localdownloader.viewmodel.FormatViewModel
import com.localdownloader.viewmodel.MediaToolsViewModel
import com.localdownloader.viewmodel.PlayerViewModel
import kotlinx.serialization.json.Json

@Composable
fun DownloaderApp(
    externalOpenRequest: ExternalOpenRequest? = null,
    onExternalOpenHandled: (() -> Unit)? = null,
    sharedUrlRequest: String? = null,
    onSharedUrlHandled: (() -> Unit)? = null,
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
    var activeExternalOpenRequest by remember { mutableStateOf<ExternalOpenRequest?>(externalOpenRequest) }
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

    LaunchedEffect(externalOpenRequest) {
        if (externalOpenRequest != null) {
            activeExternalOpenRequest = externalOpenRequest
            navController.navigate(Routes.ExternalOpen) {
                launchSingleTop = true
            }
            onExternalOpenHandled?.invoke()
        }
    }

    LaunchedEffect(sharedUrlRequest) {
        val sharedUrl = sharedUrlRequest?.trim().orEmpty()
        if (sharedUrl.isNotBlank()) {
            formatViewModel.onUrlChanged(sharedUrl)
            navController.navigate(Routes.Browser) {
                launchSingleTop = true
            }
            formatViewModel.analyzeUrl()
            onSharedUrlHandled?.invoke()
        }
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
                    onClearBrowserState = formatViewModel::clearBrowserState,
                    onClearAnalyzedResult = formatViewModel::clearAnalyzedResult,
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
                    savedItemsCount = downloadState.tasks.count { it.status == com.localdownloader.domain.models.DownloadStatus.COMPLETED },
                    mediaInfoMessage = downloadState.infoMessage,
                    mediaErrorMessage = downloadState.errorMessage,
                    onDismissMediaLibraryMessage = downloadViewModel::dismissMessage,
                    onDarkThemeChanged = { enabled ->
                        formatViewModel.toggleDarkTheme(enabled)
                        onDarkThemeChanged?.invoke(enabled)
                    },
                    onOutputTemplateChanged = formatViewModel::onOutputTemplateChanged,
                    onContainerChanged = formatViewModel::onContainerChanged,
                    onEmbedMetadataChanged = formatViewModel::onEmbedMetadataChanged,
                    onEmbedThumbnailChanged = formatViewModel::onEmbedThumbnailChanged,
                    onAutoRemoveMissingFilesFromLibraryChanged = formatViewModel::onAutoRemoveMissingFilesFromLibraryChanged,
                    onDeleteFromStorageWhenRemovedInAppChanged = formatViewModel::onDeleteFromStorageWhenRemovedInAppChanged,
                    onClearVideoTabEntries = downloadViewModel::clearCompletedLibraryEntries,
                    onDeleteAllSavedMedia = downloadViewModel::deleteAllCompletedMedia,
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
                val playerViewModel: PlayerViewModel = hiltViewModel(backStackEntry)
                PlayerScreen(
                    task = task,
                    playerViewModel = playerViewModel,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ExternalOpen) { backStackEntry ->
                val request = activeExternalOpenRequest
                if (request == null) {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                } else if (isPlayableMediaRequest(request)) {
                    val playerViewModel: PlayerViewModel = hiltViewModel(backStackEntry)
                    PlayerScreen(
                        task = com.localdownloader.domain.models.DownloadTask(
                            id = "external:${request.path}",
                            url = request.path,
                            title = request.displayName,
                            status = DownloadStatus.COMPLETED,
                            outputPath = request.path,
                        ),
                        playerViewModel = playerViewModel,
                        onBack = {
                            activeExternalOpenRequest = null
                            navController.popBackStack()
                        },
                    )
                } else {
                    ExternalPreviewScreen(
                        request = request,
                        mode = when {
                            isWebPreviewRequest(request) -> ExternalPreviewMode.WEB
                            else -> ExternalPreviewMode.IMAGE
                        },
                        onBack = {
                            activeExternalOpenRequest = null
                            navController.popBackStack()
                        },
                    )
                }
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
    const val ExternalOpen = "external_open"
}

private data class PrimaryDestination(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit,
)

private fun isPlayableMediaRequest(request: ExternalOpenRequest): Boolean {
    val mime = request.mimeType?.lowercase().orEmpty()
    if (mime.startsWith("video/") || mime.startsWith("audio/")) return true
    val extension = request.path.substringAfterLast('.', "").lowercase()
    return extension in PLAYABLE_MEDIA_EXTENSIONS
}

private fun isWebPreviewRequest(request: ExternalOpenRequest): Boolean {
    val mime = request.mimeType?.lowercase().orEmpty()
    val extension = request.path.substringAfterLast('.', "").lowercase()
    return mime.contains("html") || mime.contains("multipart/related") || extension in setOf("html", "htm", "mhtml", "mht")
}

private val PLAYABLE_MEDIA_EXTENSIONS = setOf(
    "mp4", "mkv", "webm", "mov", "avi", "m4v", "3gp", "ts", "m2ts", "mpeg", "mpg",
    "mp3", "m4a", "aac", "opus", "ogg", "wav", "flac", "amr",
)

