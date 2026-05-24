package com.localdownloader.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
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
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.localdownloader.R
import com.localdownloader.AppLaunchRouter
import com.localdownloader.AppOpenRequest
import com.localdownloader.domain.models.AccentPreset
import com.localdownloader.domain.models.DownloadStatus
import com.localdownloader.domain.models.ContrastMode
import com.localdownloader.domain.models.ThemeMode
import com.localdownloader.ui.screens.CookieCaptureScreen
import com.localdownloader.ui.screens.CookiesScreen
import com.localdownloader.ui.screens.BrowserScreen
import com.localdownloader.ui.screens.CompressScreen
import com.localdownloader.ui.screens.ConvertScreen
import com.localdownloader.ui.screens.DownloadsScreen
import com.localdownloader.ui.screens.DownloadHistoryScreen
import com.localdownloader.ui.screens.ExternalPreviewMode
import com.localdownloader.ui.screens.ExternalPreviewScreen
import com.localdownloader.ui.screens.HelpScreen
import com.localdownloader.ui.screens.MoreScreen
import com.localdownloader.ui.screens.MusicPlayerScreen
import com.localdownloader.ui.screens.PlayerScreen
import com.localdownloader.ui.screens.ProgressScreen
import com.localdownloader.ui.screens.SettingsScreen
import com.localdownloader.ui.screens.UpdateChangelogScreen
import com.localdownloader.ui.screens.UpdateChangelogSections
import com.localdownloader.ui.screens.UpdatesScreen
import com.localdownloader.ui.screens.YoutubeAuthLoginScreen
import com.localdownloader.ui.screens.YoutubeAuthScreen
import com.localdownloader.ui.screens.settings.AboutSettingsScreen
import com.localdownloader.ui.screens.settings.AccessSettingsScreen
import com.localdownloader.ui.screens.settings.AppearanceSettingsScreen
import com.localdownloader.ui.screens.settings.AppLogSettingsScreen
import com.localdownloader.ui.screens.settings.DownloadSettingsScreen
import com.localdownloader.ui.screens.settings.NotificationsSettingsScreen
import com.localdownloader.ui.screens.settings.StorageSettingsScreen
import com.localdownloader.viewmodel.AppLogViewModel
import com.localdownloader.ui.model.ExternalOpenRequest
import com.localdownloader.ui.model.buildVideoLibraryItems
import com.localdownloader.ui.model.toAudioQueueItems
import com.localdownloader.viewmodel.DownloadViewModel
import com.localdownloader.viewmodel.FormatViewModel
import com.localdownloader.viewmodel.MediaToolsViewModel
import com.localdownloader.viewmodel.AudioPlaybackViewModel
import com.localdownloader.viewmodel.PlayerViewModel
import com.localdownloader.viewmodel.UpdatesViewModel

@Composable
fun DownloaderApp(
    externalOpenRequest: ExternalOpenRequest? = null,
    onExternalOpenHandled: (() -> Unit)? = null,
    sharedUrlRequest: String? = null,
    onSharedUrlHandled: (() -> Unit)? = null,
    notificationOpenRequest: AppOpenRequest? = null,
    onNotificationOpenHandled: (() -> Unit)? = null,
    onAppearanceUpdated: ((ThemeMode, AccentPreset, ContrastMode) -> Unit)? = null,
    onLanguageUpdated: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val formatViewModel: FormatViewModel = hiltViewModel()
    val downloadViewModel: DownloadViewModel = hiltViewModel()
    val mediaToolsViewModel: MediaToolsViewModel = hiltViewModel()
    val audioPlaybackViewModel: AudioPlaybackViewModel = hiltViewModel()
    val updatesViewModel: UpdatesViewModel = hiltViewModel()
    val context = LocalContext.current
    // Use the DI-provided FileUtils from mediaToolsViewModel instead of creating a new instance.
    val fileUtils = mediaToolsViewModel.fileUtils
    var cacheSize by remember { mutableStateOf(0L) }
    var activeExternalOpenRequest by remember { mutableStateOf<ExternalOpenRequest?>(externalOpenRequest) }
    var pendingCookieCaptureUrl by remember { mutableStateOf<String?>(null) }
    var pendingCookieCaptureProfileId by remember { mutableStateOf<String?>(null) }
    var pendingFolderBrowseTarget by remember { mutableStateOf<FolderBrowseTarget?>(null) }

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

    val formatState by formatViewModel.uiState.collectAsStateWithLifecycle()
    val downloadState by downloadViewModel.uiState.collectAsStateWithLifecycle()
    val mediaToolsState by mediaToolsViewModel.uiState.collectAsStateWithLifecycle()
    val audioPlaybackState by audioPlaybackViewModel.uiState.collectAsStateWithLifecycle()
    val updatesState by updatesViewModel.uiState.collectAsStateWithLifecycle()
    val folderTreePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        val target = pendingFolderBrowseTarget
        pendingFolderBrowseTarget = null
        if (target == null || uri == null) return@rememberLauncherForActivityResult

        val relativeDownloadsPath = fileUtils.resolveRelativeDownloadsFolderFromTreeUri(uri)
        if (relativeDownloadsPath == null) {
            formatViewModel.showSettingsMessage(
                message = "Pick a folder inside your public Downloads directory.",
                isError = true,
            )
            return@rememberLauncherForActivityResult
        }

        when (target) {
            FolderBrowseTarget.DOWNLOADS_ROOT -> {
                if (relativeDownloadsPath.isBlank()) {
                    formatViewModel.showSettingsMessage(
                        message = "Pick a subfolder inside Downloads for the app root.",
                        isError = true,
                    )
                } else {
                    formatViewModel.onDownloadsRootFolderNameChanged(relativeDownloadsPath)
                    formatViewModel.showSettingsMessage("Downloads root updated.")
                }
            }

            FolderBrowseTarget.VIDEO -> {
                val relativeSubfolder = fileUtils.deriveSubfolderSettingFromRelativeDownloadsPath(
                    rootFolderSetting = formatState.downloadsRootFolderName,
                    selectedRelativeDownloadsPath = relativeDownloadsPath,
                )
                if (relativeSubfolder == null) {
                    formatViewModel.showSettingsMessage(
                        message = "Pick a folder inside the current downloads root for videos.",
                        isError = true,
                    )
                } else {
                    formatViewModel.onVideoSubfolderNameChanged(relativeSubfolder)
                    formatViewModel.showSettingsMessage("Video folder updated.")
                }
            }

            FolderBrowseTarget.AUDIO -> {
                val relativeSubfolder = fileUtils.deriveSubfolderSettingFromRelativeDownloadsPath(
                    rootFolderSetting = formatState.downloadsRootFolderName,
                    selectedRelativeDownloadsPath = relativeDownloadsPath,
                )
                if (relativeSubfolder == null) {
                    formatViewModel.showSettingsMessage(
                        message = "Pick a folder inside the current downloads root for audio.",
                        isError = true,
                    )
                } else {
                    formatViewModel.onAudioSubfolderNameChanged(relativeSubfolder)
                    formatViewModel.showSettingsMessage("Audio folder updated.")
                }
            }

            FolderBrowseTarget.OTHER -> {
                val relativeSubfolder = fileUtils.deriveSubfolderSettingFromRelativeDownloadsPath(
                    rootFolderSetting = formatState.downloadsRootFolderName,
                    selectedRelativeDownloadsPath = relativeDownloadsPath,
                )
                if (relativeSubfolder == null) {
                    formatViewModel.showSettingsMessage(
                        message = "Pick a folder inside the current downloads root for other files.",
                        isError = true,
                    )
                } else {
                    formatViewModel.onOtherSubfolderNameChanged(relativeSubfolder)
                    formatViewModel.showSettingsMessage("Other files folder updated.")
                }
            }
        }
    }
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val currentRoute = currentDestination?.route
    val savedItemsCount = downloadState.tasks.count { it.status == DownloadStatus.COMPLETED }

    LaunchedEffect(formatState.themeMode, formatState.accentPreset, formatState.contrastMode) {
        onAppearanceUpdated?.invoke(
            formatState.themeMode,
            formatState.accentPreset,
            formatState.contrastMode,
        )
    }

    LaunchedEffect(formatState.languageTag) {
        onLanguageUpdated?.invoke(formatState.languageTag)
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

    LaunchedEffect(notificationOpenRequest) {
        val request = notificationOpenRequest ?: return@LaunchedEffect
        when (request.route) {
            Routes.Browser,
            Routes.Downloads,
            Routes.More,
            -> {
                navController.navigate(request.route) {
                    launchSingleTop = true
                    restoreState = true
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                }
            }

            Routes.DownloadQueue -> {
                navController.navigate(Routes.DownloadQueue) {
                    launchSingleTop = true
                }
            }

            Routes.Music -> {
                navController.navigate(Routes.Music) {
                    launchSingleTop = true
                }
            }

            Routes.Player -> {
                val taskId = request.taskId
                if (!taskId.isNullOrBlank()) {
                    navController.navigate("${Routes.Player}/$taskId") {
                        launchSingleTop = true
                    }
                } else {
                    navController.navigate(Routes.Downloads) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                    }
                }
            }

            else -> {
                navController.navigate(Routes.Downloads) {
                    launchSingleTop = true
                    restoreState = true
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                }
            }
        }
        onNotificationOpenHandled?.invoke()
    }

    // Update cache size periodically
    LaunchedEffect(currentRoute) {
        cacheSize = fileUtils.getCacheSize()
    }

    val primaryDestinations = remember {
        listOf(
            PrimaryDestination(
                route = Routes.Browser,
                labelRes = R.string.nav_home,
                icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
            ),
            PrimaryDestination(
                route = Routes.Downloads,
                labelRes = R.string.nav_downloads,
                icon = { Icon(Icons.Outlined.CloudDownload, contentDescription = null) },
            ),
            PrimaryDestination(
                route = Routes.More,
                labelRes = R.string.nav_more,
                icon = { Icon(Icons.Outlined.MoreHoriz, contentDescription = null) },
            ),
        )
    }

    val showBottomBar = currentRoute in primaryDestinations.map { it.route }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
                    slideInVertically(
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                    ) { fullHeight -> fullHeight / 2 },
                exit = fadeOut(animationSpec = tween(durationMillis = 180)) +
                    slideOutVertically(
                        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                    ) { fullHeight -> fullHeight / 2 },
            ) {
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
                            label = { Text(stringResource(destination.labelRes)) },
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
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 220)) +
                    slideIntoContainer(
                        towards = navigationDirection(initialState.destination.route, targetState.destination.route),
                        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180)) +
                    slideOutOfContainer(
                        towards = navigationDirection(initialState.destination.route, targetState.destination.route),
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
                    )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 220)) +
                    slideIntoContainer(
                        towards = navigationDirection(initialState.destination.route, targetState.destination.route),
                        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 160)) +
                    slideOutOfContainer(
                        towards = navigationDirection(initialState.destination.route, targetState.destination.route),
                        animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
                    )
            },
        ) {
            composable(Routes.Browser) {
                BrowserScreen(
                    uiState = formatState,
                    onUrlChanged = formatViewModel::onUrlChanged,
                    onAnalyzeClicked = formatViewModel::analyzeUrl,
                    onQualityChanged = formatViewModel::onQualityChanged,
                    onStreamTypeChanged = formatViewModel::onStreamTypeChanged,
                    onOutputTransformChanged = formatViewModel::onOutputTransformChanged,
                    onFormatSelectorChanged = formatViewModel::onFormatSelectorChanged,
                    onContainerChanged = formatViewModel::onContainerChanged,
                    onAudioFormatChanged = formatViewModel::onAudioFormatChanged,
                    onAudioBitrateChanged = formatViewModel::onAudioBitrateChanged,
                    onDownloadSubtitlesChanged = formatViewModel::onDownloadSubtitlesChanged,
                    onEmbedSubtitlesChanged = formatViewModel::onEmbedSubtitlesChanged,
                    onEmbedMetadataChanged = formatViewModel::onEmbedMetadataChanged,
                    onEmbedThumbnailChanged = formatViewModel::onEmbedThumbnailChanged,
                    onWriteThumbnailChanged = formatViewModel::onWriteThumbnailChanged,
                    onPlaylistEnabledChanged = formatViewModel::onPlaylistEnabledChanged,
                    onPlaylistSelectAllChanged = formatViewModel::onPlaylistSelectAllChanged,
                    onPlaylistItemSelectedChanged = formatViewModel::onPlaylistItemSelectedChanged,
                    onPlaylistItemExpandedChanged = formatViewModel::onPlaylistItemExpandedChanged,
                    onPlaylistItemUseGlobalChanged = formatViewModel::onPlaylistItemUseGlobalChanged,
                    onPlaylistItemStreamTypeChanged = formatViewModel::onPlaylistItemStreamTypeChanged,
                    onPlaylistItemOutputTransformChanged = formatViewModel::onPlaylistItemOutputTransformChanged,
                    onPlaylistItemFormatSelectorChanged = formatViewModel::onPlaylistItemFormatSelectorChanged,
                    onPlaylistItemContainerChanged = formatViewModel::onPlaylistItemContainerChanged,
                    onPlaylistItemAudioFormatChanged = formatViewModel::onPlaylistItemAudioFormatChanged,
                    onPlaylistItemAudioBitrateChanged = formatViewModel::onPlaylistItemAudioBitrateChanged,
                    onCustomFileNameChanged = formatViewModel::onCustomFileNameChanged,
                    onPlaylistItemFileNameChanged = formatViewModel::onPlaylistItemFileNameChanged,
                    onOutputTemplateChanged = formatViewModel::onOutputTemplateChanged,
                    onAudioOutputTemplateChanged = formatViewModel::onAudioOutputTemplateChanged,
                    onClearBrowserState = formatViewModel::clearBrowserState,
                    onClearAnalyzedResult = formatViewModel::clearAnalyzedResult,
                    onOpenReadyItem = formatViewModel::reopenReadyItem,
                    onRemoveReadyItem = formatViewModel::removeReadyItem,
                    onQueueDownloadClicked = formatViewModel::queueDownload,
                    onOpenHistory = { navController.navigate(Routes.History) },
                    onOpenCompress = { navController.navigate(Routes.Compress) },
                    onOpenConvert = { navController.navigate(Routes.Convert) },
                    onOpenYoutubeAccess = { navController.navigate(Routes.YoutubeAuth) },
                    onOpenCookies = { navController.navigate(Routes.Cookies) },
                    onOpenSettings = { navController.navigate(Routes.Settings) },
                    onOpenHelp = { navController.navigate(Routes.Help) },
                    onDismissDownloadSetupNotice = formatViewModel::dismissDownloadSetupNotice,
                    onDismissMessage = formatViewModel::dismissMessage,
                    onDismissMeteredNetworkDialog = formatViewModel::dismissMeteredNetworkDialog,
                    onQueueWhenWifiAvailable = formatViewModel::queueDownloadWhenWifiAvailable,
                    onAllowCellularDownloadsAndQueue = formatViewModel::allowCellularDownloadsAndQueue,
                    onDarkThemeChanged = formatViewModel::toggleDarkTheme,
                    isDownloadButtonEnabled = formatViewModel.isDownloadButtonEnabled(),
                )
            }
            composable(Routes.Downloads) {
                DownloadsScreen(
                    uiState = downloadState,
                    audioPlaybackState = audioPlaybackState,
                    onOpenMusic = {
                        navController.navigate(Routes.Music) {
                            launchSingleTop = true
                        }
                    },
                    onPlayMusic = { taskId, shuffle ->
                        val audioQueue = buildVideoLibraryItems(downloadState.tasks).toAudioQueueItems()
                        if (audioQueue.isNotEmpty()) {
                            audioPlaybackViewModel.playQueue(audioQueue, taskId, shuffle)
                        }
                        navController.navigate(Routes.Music) {
                            launchSingleTop = true
                        }
                    },
                    onDismissAudioError = audioPlaybackViewModel::dismissError,
                    onOpenPlayer = { taskId -> navController.navigate("${Routes.Player}/$taskId") },
                    onRename = downloadViewModel::renameDownloadedFile,
                    onDelete = downloadViewModel::deleteDownloadedFile,
                    onRemoveSelectedFromApp = downloadViewModel::removeDownloadedFilesFromLibrary,
                    onDeleteSelectedFromDevice = downloadViewModel::permanentlyDeleteDownloadedFiles,
                    onRemoveCompletedFromApp = downloadViewModel::clearCompletedLibraryEntries,
                    onDeleteCompletedFromDevice = downloadViewModel::deleteAllCompletedMedia,
                    onDismissMessage = downloadViewModel::dismissMessage,
                    onOpenQueue = { navController.navigate(Routes.DownloadQueue) },
                )
            }
            composable(Routes.DownloadQueue) {
                ProgressScreen(
                    uiState = downloadState,
                    onPause = downloadViewModel::pause,
                    onResume = downloadViewModel::resume,
                    onRetry = downloadViewModel::retry,
                    onCancel = downloadViewModel::cancel,
                    onPauseTasks = downloadViewModel::pauseTasks,
                    onResumeTasks = downloadViewModel::resumeTasks,
                    onRetryTasks = downloadViewModel::retryTasks,
                    onCancelTasks = downloadViewModel::cancelTasks,
                    onOpenCookies = { navController.navigate(Routes.Cookies) },
                    onOpenYoutubeAccess = { navController.navigate(Routes.YoutubeAuth) },
                    onToggleDebug = downloadViewModel::toggleDebug,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.More) {
                MoreScreen(
                    onOpenQueue = { navController.navigate(Routes.DownloadQueue) },
                    onOpenHistory = { navController.navigate(Routes.History) },
                    onOpenCompress = { navController.navigate(Routes.Compress) },
                    onOpenConvert = { navController.navigate(Routes.Convert) },
                    onOpenYoutubeAccess = { navController.navigate(Routes.YoutubeAuth) },
                    onOpenCookies = { navController.navigate(Routes.Cookies) },
                    onOpenUpdates = { navController.navigate(Routes.Updates) },
                    onOpenSettings = { navController.navigate(Routes.Settings) },
                    onOpenHelp = { navController.navigate(Routes.Help) },
                )
            }
            composable(Routes.YoutubeAuth) {
                YoutubeAuthScreen(
                    uiState = formatState,
                    onBack = { navController.popBackStack() },
                    onGenerateAccess = { navController.navigate(Routes.YoutubeAuthLogin) },
                    onEnabledChanged = formatViewModel::setYoutubeAuthEnabled,
                    onClear = formatViewModel::clearYoutubeAuthConfig,
                    onDismissMessage = formatViewModel::dismissMessage,
                )
            }
            composable(Routes.YoutubeAuthLogin) {
                YoutubeAuthLoginScreen(
                    onBack = { navController.popBackStack() },
                    onConfirm = { cookieText, authConfig ->
                        formatViewModel.saveYoutubeAuthSession(cookieText, authConfig)
                        navController.popBackStack(Routes.YoutubeAuth, inclusive = false)
                    },
                )
            }
            composable(Routes.Cookies) {
                CookiesScreen(
                    uiState = formatState,
                    onBack = { navController.popBackStack() },
                    onCookiesEnabledChanged = formatViewModel::onCookiesEnabledChanged,
                    onCookieUserAgentEnabledChanged = formatViewModel::onCookieUserAgentEnabledChanged,
                    onSaveCookie = { profileId, url, cookiesText ->
                        formatViewModel.saveCookieProfile(profileId, url, cookiesText)
                    },
                    onDeleteCookie = formatViewModel::deleteCookieProfile,
                    onDeleteAllCookies = formatViewModel::clearAllCookieProfiles,
                    onImportCookieText = formatViewModel::importCookieText,
                    onOpenCookieCapture = { url, profileId ->
                        pendingCookieCaptureUrl = url
                        pendingCookieCaptureProfileId = profileId
                        navController.navigate(Routes.CookieCapture)
                    },
                    onDismissMessage = formatViewModel::dismissMessage,
                )
            }
            composable(Routes.CookieCapture) {
                val captureUrl = pendingCookieCaptureUrl
                if (captureUrl == null) {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                } else {
                    CookieCaptureScreen(
                        url = captureUrl,
                        onBack = {
                            pendingCookieCaptureUrl = null
                            pendingCookieCaptureProfileId = null
                            navController.popBackStack()
                        },
                        onConfirm = { cookieText ->
                            formatViewModel.replaceCookieFromBrowser(
                                profileId = pendingCookieCaptureProfileId,
                                url = captureUrl,
                                cookieText = cookieText,
                            )
                            pendingCookieCaptureUrl = null
                            pendingCookieCaptureProfileId = null
                            navController.popBackStack()
                        },
                    )
                }
            }
            composable(Routes.History) {
                DownloadHistoryScreen(
                    tasks = downloadState.tasks,
                    retentionDays = downloadState.downloadHistoryRetentionDays,
                    infoMessage = downloadState.infoMessage,
                    errorMessage = downloadState.errorMessage,
                    onDismissMessage = downloadViewModel::dismissMessage,
                    onRetentionDaysChanged = downloadViewModel::setDownloadHistoryRetentionDays,
                    onClearFailedAndCanceledHistory = downloadViewModel::clearFailedAndCanceledHistory,
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
                    onCompressQuickPresetSelected = mediaToolsViewModel::onCompressQuickPresetSelected,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.Settings) {
                SettingsScreen(
                    uiState = formatState,
                    savedItemsCount = savedItemsCount,
                    mediaInfoMessage = downloadState.infoMessage,
                    mediaErrorMessage = downloadState.errorMessage,
                    onDismissMediaLibraryMessage = downloadViewModel::dismissMessage,
                    onOpenAppearance = { navController.navigate(Routes.SettingsAppearance) },
                    onOpenDownloads = { navController.navigate(Routes.SettingsDownloads) },
                    onOpenStorage = { navController.navigate(Routes.SettingsStorage) },
                    onOpenNotifications = { navController.navigate(Routes.SettingsNotifications) },
                    onOpenAccess = { navController.navigate(Routes.SettingsAccess) },
                    onOpenAbout = { navController.navigate(Routes.SettingsAbout) },
                    onOpenAppLog = { navController.navigate(Routes.SettingsAppLog) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SettingsAppearance) {
                AppearanceSettingsScreen(
                    uiState = formatState,
                    onLanguageChanged = formatViewModel::onLanguageChanged,
                    onThemeModeChanged = formatViewModel::onThemeModeChanged,
                    onAccentPresetChanged = formatViewModel::onAccentPresetChanged,
                    onContrastModeChanged = formatViewModel::onContrastModeChanged,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SettingsDownloads) {
                DownloadSettingsScreen(
                    uiState = formatState,
                    onDefaultVideoOutputTemplateChanged = formatViewModel::onDefaultVideoOutputTemplateChanged,
                    onDefaultAudioOutputTemplateChanged = formatViewModel::onDefaultAudioOutputTemplateChanged,
                    onDefaultVideoContainerChanged = formatViewModel::onDefaultVideoContainerChanged,
                    onDefaultAudioContainerChanged = formatViewModel::onDefaultAudioFormatChanged,
                    onDefaultDownloadSubtitlesChanged = formatViewModel::onDefaultDownloadSubtitlesChanged,
                    onDefaultEmbedSubtitlesChanged = formatViewModel::onDefaultEmbedSubtitlesChanged,
                    onDefaultEmbedMetadataChanged = formatViewModel::onDefaultEmbedMetadataChanged,
                    onDefaultEmbedThumbnailChanged = formatViewModel::onDefaultEmbedThumbnailChanged,
                    onMaxConcurrentDownloadsChanged = formatViewModel::onMaxConcurrentDownloadsChanged,
                    onKeepAnalyzedLinkHistoryChanged = formatViewModel::onKeepAnalyzedLinkHistoryChanged,
                    onAnalyzedLinkHistoryRetentionDaysChanged = formatViewModel::onAnalyzedLinkHistoryRetentionDaysChanged,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SettingsStorage) {
                StorageSettingsScreen(
                    uiState = formatState,
                    savedItemsCount = savedItemsCount,
                    mediaInfoMessage = downloadState.infoMessage,
                    mediaErrorMessage = downloadState.errorMessage,
                    onDismissMediaLibraryMessage = downloadViewModel::dismissMessage,
                    onDownloadsRootFolderNameChanged = formatViewModel::onDownloadsRootFolderNameChanged,
                    onVideoSubfolderNameChanged = formatViewModel::onVideoSubfolderNameChanged,
                    onAudioSubfolderNameChanged = formatViewModel::onAudioSubfolderNameChanged,
                    onOtherSubfolderNameChanged = formatViewModel::onOtherSubfolderNameChanged,
                    onBrowseDownloadsRootFolder = {
                        pendingFolderBrowseTarget = FolderBrowseTarget.DOWNLOADS_ROOT
                        folderTreePicker.launch(null)
                    },
                    onBrowseVideoFolder = {
                        pendingFolderBrowseTarget = FolderBrowseTarget.VIDEO
                        folderTreePicker.launch(null)
                    },
                    onBrowseAudioFolder = {
                        pendingFolderBrowseTarget = FolderBrowseTarget.AUDIO
                        folderTreePicker.launch(null)
                    },
                    onBrowseOtherFolder = {
                        pendingFolderBrowseTarget = FolderBrowseTarget.OTHER
                        folderTreePicker.launch(null)
                    },
                    onAutoRemoveMissingFilesFromLibraryChanged = formatViewModel::onAutoRemoveMissingFilesFromLibraryChanged,
                    onDeleteFromStorageWhenRemovedInAppChanged = formatViewModel::onDeleteFromStorageWhenRemovedInAppChanged,
                    onClearVideoTabEntries = downloadViewModel::clearCompletedLibraryEntries,
                    onDeleteAllSavedMedia = downloadViewModel::deleteAllCompletedMedia,
                    onResetSettings = formatViewModel::resetSettingsToDefaults,
                    onClearCache = {
                        fileUtils.clearCache()
                        cacheSize = 0L
                    },
                    cacheSize = cacheSize,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SettingsNotifications) {
                NotificationsSettingsScreen(
                    uiState = formatState,
                    onNotifyCompletedDownloadsChanged = formatViewModel::onNotifyCompletedDownloadsChanged,
                    onNotifyDownloadErrorsChanged = formatViewModel::onNotifyDownloadErrorsChanged,
                    onNotifyCanceledDownloadsChanged = formatViewModel::onNotifyCanceledDownloadsChanged,
                    onNotifyPromotionsChanged = formatViewModel::onNotifyPromotionsChanged,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SettingsAccess) {
                AccessSettingsScreen(
                    uiState = formatState,
                    onAllowMeteredDownloadsChanged = formatViewModel::onAllowMeteredDownloadsChanged,
                    onCookiesEnabledChanged = formatViewModel::onCookiesEnabledChanged,
                    onCookieUserAgentEnabledChanged = formatViewModel::onCookieUserAgentEnabledChanged,
                    onOpenCookies = { navController.navigate(Routes.Cookies) },
                    onOpenYoutubeAccess = { navController.navigate(Routes.YoutubeAuth) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SettingsAbout) {
                AboutSettingsScreen(
                    onOpenUpdates = { navController.navigate(Routes.Updates) },
                    onResetSettings = formatViewModel::resetSettingsToDefaults,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SettingsAppLog) {
                val appLogViewModel: AppLogViewModel = hiltViewModel()
                val appLogState by appLogViewModel.uiState.collectAsStateWithLifecycle()
                AppLogSettingsScreen(
                    uiState = appLogState,
                    onRefresh = appLogViewModel::refresh,
                    onOutcomeFilterChanged = appLogViewModel::setOutcomeFilter,
                    onDayFilterChanged = appLogViewModel::setDayFilter,
                    onBackupLogsToDeviceChanged = appLogViewModel::setBackupLogsToDevice,
                    onAutoDeleteOldAppLogsChanged = appLogViewModel::setAutoDeleteOldAppLogs,
                    onAppLogRetentionDaysChanged = appLogViewModel::setAppLogRetentionDays,
                    onBackupNow = appLogViewModel::backupLogsNow,
                    onDismissFeedback = appLogViewModel::clearFeedback,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.Updates) {
                LaunchedEffect(Unit) {
                    updatesViewModel.initialize()
                }
                UpdatesScreen(
                    uiState = updatesState,
                    onBack = { navController.popBackStack() },
                    onRefreshAll = updatesViewModel::refreshAll,
                    onRefreshApp = updatesViewModel::refreshApp,
                    onRefreshYtDlp = updatesViewModel::refreshYtDlp,
                    onRefreshFfmpeg = updatesViewModel::refreshFfmpeg,
                    onInstallAppUpdate = updatesViewModel::installAppUpdate,
                    onInstallYtDlpUpdate = updatesViewModel::installYtDlpUpdate,
                    onInstallFfmpegUpdate = updatesViewModel::installFfmpegUpdate,
                    onYtDlpChannelChanged = updatesViewModel::setYtDlpChannel,
                    onFfmpegChannelChanged = updatesViewModel::setFfmpegChannel,
                    onAutoUpdateYtDlpChanged = updatesViewModel::setAutoUpdateYtDlp,
                    onIncludePrereleaseAppReleasesChanged = updatesViewModel::setIncludePrereleaseAppReleases,
                    onOpenChangelog = { section ->
                        navController.navigate(Routes.updateChangelog(section))
                    },
                    onConsumePendingAppInstall = updatesViewModel::consumePendingAppInstall,
                    onDismissMessage = updatesViewModel::dismissMessage,
                )
            }
            composable(
                route = Routes.UpdateChangelog,
                arguments = listOf(navArgument("section") { type = NavType.StringType }),
            ) { backStackEntry ->
                val sectionKey = backStackEntry.arguments?.getString("section")
                val sectionState = when (sectionKey) {
                    UpdateChangelogSections.APP -> updatesState.app
                    UpdateChangelogSections.YT_DLP -> updatesState.ytDlp
                    UpdateChangelogSections.FFMPEG -> updatesState.ffmpeg
                    else -> null
                }
                UpdateChangelogScreen(
                    title = sectionState?.title ?: "Changelog",
                    currentVersion = sectionState?.currentVersion,
                    latestVersion = sectionState?.latestVersion,
                    summary = sectionState?.summary ?: "No update details are available.",
                    releaseNotes = sectionState?.releaseNotes,
                    latestDocumentHeading = if (sectionKey == UpdateChangelogSections.APP) {
                        "Latest app release notes"
                    } else {
                        "Recent release notes"
                    },
                    bundledDocumentHeading = if (sectionKey == UpdateChangelogSections.APP) {
                        "Full app changelog"
                    } else {
                        null
                    },
                    overviewText = if (sectionKey == UpdateChangelogSections.APP) {
                        "Read the newest app release notes first, then browse the bundled full changelog below."
                    } else {
                        "Read the latest release notes in a cleaner documentation-style view."
                    },
                    bundledReleaseNotesAssetName = if (sectionKey == UpdateChangelogSections.APP) {
                        "changelog/CHANGELOG.md"
                    } else {
                        null
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.Help) {
                HelpScreen(
                    onBack = { navController.popBackStack() },
                    onOpenCookies = { navController.navigate(Routes.Cookies) },
                    onOpenYoutubeAccess = { navController.navigate(Routes.YoutubeAuth) },
                )
            }
            composable(Routes.Music) {
                MusicPlayerScreen(
                    uiState = downloadState,
                    audioPlaybackState = audioPlaybackState,
                    onPlayAudioQueue = audioPlaybackViewModel::playQueue,
                    onToggleAudioPlayback = audioPlaybackViewModel::togglePlayback,
                    onSeekAudioBy = audioPlaybackViewModel::seekBy,
                    onSeekAudioTo = audioPlaybackViewModel::seekTo,
                    onSkipToPreviousAudio = audioPlaybackViewModel::skipPrevious,
                    onSkipToNextAudio = audioPlaybackViewModel::skipNext,
                    onToggleAudioShuffle = audioPlaybackViewModel::toggleShuffle,
                    onCycleAudioRepeatMode = audioPlaybackViewModel::cycleRepeatMode,
                    onSetAudioSleepTimer = audioPlaybackViewModel::setSleepTimer,
                    onStopAudioPlayback = audioPlaybackViewModel::stopPlayback,
                    onDismissAudioError = audioPlaybackViewModel::dismissError,
                    onBack = { navController.popBackStack() },
                )
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

object Routes {
    const val Browser = AppLaunchRouter.ROUTE_BROWSER
    const val Downloads = AppLaunchRouter.ROUTE_DOWNLOADS
    const val Music = AppLaunchRouter.ROUTE_MUSIC
    const val More = AppLaunchRouter.ROUTE_MORE
    const val YoutubeAuth = "youtube_auth"
    const val YoutubeAuthLogin = "youtube_auth_login"
    const val Cookies = "cookies"
    const val CookieCapture = "cookie_capture"
    const val DownloadQueue = AppLaunchRouter.ROUTE_DOWNLOAD_QUEUE
    const val History = "history"
    const val Convert = "convert"
    const val Compress = "compress"
    const val Settings = "settings"
    const val SettingsAppearance = "settings/appearance"
    const val SettingsDownloads = "settings/downloads"
    const val SettingsStorage = "settings/storage"
    const val SettingsNotifications = "settings/notifications"
    const val SettingsAccess = "settings/access"
    const val SettingsAbout = "settings/about"
    const val SettingsAppLog = "settings/app-log"
    const val Updates = "updates"
    const val UpdateChangelog = "updates/changelog/{section}"
    const val Help = "help"
    const val Player = AppLaunchRouter.ROUTE_PLAYER
    const val ExternalOpen = "external_open"

    fun updateChangelog(section: String): String {
        return "updates/changelog/${android.net.Uri.encode(section)}"
    }
}

private data class PrimaryDestination(
    val route: String,
    val labelRes: Int,
    val icon: @Composable () -> Unit,
)

private enum class FolderBrowseTarget {
    DOWNLOADS_ROOT,
    VIDEO,
    AUDIO,
    OTHER,
}

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

private val primaryRouteOrder = listOf(
    Routes.Browser,
    Routes.Downloads,
    Routes.More,
)

private fun navigationDirection(
    fromRoute: String?,
    toRoute: String?,
): AnimatedContentTransitionScope.SlideDirection {
    val fromIndex = primaryRouteOrder.indexOf(normalizeRoute(fromRoute))
    val toIndex = primaryRouteOrder.indexOf(normalizeRoute(toRoute))
    return if (fromIndex != -1 && toIndex != -1 && toIndex < fromIndex) {
        AnimatedContentTransitionScope.SlideDirection.Right
    } else {
        AnimatedContentTransitionScope.SlideDirection.Left
    }
}

private fun normalizeRoute(route: String?): String? {
    return route?.substringBefore('/')
}
