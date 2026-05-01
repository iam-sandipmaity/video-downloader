package com.localplayer.ui.screens

import android.app.Activity
import android.media.AudioManager
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CropFree
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.localplayer.MainActivity
import com.localplayer.model.PlayerMedia
import com.localplayer.viewmodel.PlayerTrackOption
import com.localplayer.viewmodel.PlayerUiState
import com.localplayer.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun PlayerScreen(
    media: PlayerMedia?,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = context.findActivity()
    val playablePath = media?.uriString?.takeIf { path -> path.isNotBlank() }
    val allowBackgroundPlayback = remember(playablePath, media?.mimeType) {
        isLikelyAudioFile(playablePath, media?.mimeType)
    }
    val uiState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val selectedAudioTrack = uiState.audioTracks.firstOrNull { it.isSelected }
    val selectedSubtitleTrack = uiState.subtitleTracks.firstOrNull { it.isSelected }

    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var controlsVisible by rememberSaveable { mutableStateOf(true) }
    var gestureFeedback by rememberSaveable { mutableStateOf<String?>(null) }
    var swipeHintVisible by rememberSaveable { mutableStateOf(true) }
    var swipeAdjustmentOverlay by remember { mutableStateOf<SwipeAdjustmentOverlay?>(null) }
    var swipeSeekOverlay by remember { mutableStateOf<SwipeSeekOverlay?>(null) }
    var playerWidthPx by rememberSaveable { mutableStateOf(0) }
    var playerHeightPx by rememberSaveable { mutableStateOf(0) }
    var isScrubbing by rememberSaveable { mutableStateOf(false) }
    var scrubPositionMs by rememberSaveable { mutableStateOf(0f) }
    var activePanelName by rememberSaveable { mutableStateOf(PlayerPanel.NONE.name) }
    var zoomScale by rememberSaveable { mutableFloatStateOf(1f) }
    var panOffsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var panOffsetY by rememberSaveable { mutableFloatStateOf(0f) }
    var swipeSeekStartPositionMs by rememberSaveable { mutableStateOf(0L) }
    val swipeAdjustmentController = remember(activity, context) {
        PlayerSwipeAdjustmentController(
            context = context,
            activity = activity,
        )
    }
    val activePanel = remember(activePanelName) {
        runCatching { PlayerPanel.valueOf(activePanelName) }.getOrDefault(PlayerPanel.NONE)
    }

    LaunchedEffect(media?.id, playablePath) {
        playerViewModel.bindMedia(media)
    }

    DisposableEffect(lifecycleOwner, playerViewModel, allowBackgroundPlayback, activity) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> playerViewModel.onAppForegrounded()
                Lifecycle.Event.ON_STOP -> playerViewModel.onAppBackgrounded(
                    allowBackgroundPlayback = allowBackgroundPlayback || (activity as? MainActivity)?.isInPictureInPictureMode == true,
                )
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            playerViewModel.persistPlaybackState()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(
        controlsVisible,
        uiState.isBuffering,
        isScrubbing,
        activePanel,
    ) {
        if (
            controlsVisible &&
            !uiState.isBuffering &&
            !isScrubbing &&
            activePanel == PlayerPanel.NONE
        ) {
            delay(CONTROLS_AUTO_HIDE_MS)
            controlsVisible = false
        }
    }

    DisposableEffect(activity, playablePath, media?.mimeType, uiState.isAvailable) {
        val mainActivity = activity as? MainActivity
        mainActivity?.updatePictureInPictureAllowed(
            enabled = uiState.isAvailable && isLikelyVideoFile(playablePath, media?.mimeType),
        )
        onDispose {
            mainActivity?.updatePictureInPictureAllowed(false)
        }
    }

    LaunchedEffect(gestureFeedback) {
        if (gestureFeedback != null) {
            delay(GESTURE_FEEDBACK_MS)
            gestureFeedback = null
        }
    }

    LaunchedEffect(playablePath) {
        swipeHintVisible = playablePath != null
        if (playablePath != null) {
            delay(SWIPE_HINT_MS)
            swipeHintVisible = false
        }
    }

    LaunchedEffect(swipeAdjustmentOverlay) {
        val overlay = swipeAdjustmentOverlay ?: return@LaunchedEffect
        if (!overlay.isActive) {
            delay(GESTURE_FEEDBACK_MS)
            if (swipeAdjustmentOverlay == overlay) {
                swipeAdjustmentOverlay = null
            }
        }
    }

    LaunchedEffect(swipeSeekOverlay) {
        val overlay = swipeSeekOverlay ?: return@LaunchedEffect
        if (!overlay.isActive) {
            delay(GESTURE_FEEDBACK_MS)
            if (swipeSeekOverlay == overlay) {
                swipeSeekOverlay = null
            }
        }
    }

    LaunchedEffect(media?.id, uiState.resizeMode) {
        zoomScale = 1f
        panOffsetX = 0f
        panOffsetY = 0f
    }

    DisposableEffect(activity, view, isFullscreen, uiState.isLocked) {
        val window = activity?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, view) }
        if (window != null && controller != null) {
            WindowCompat.setDecorFitsSystemWindows(window, !isFullscreen)
            if (isFullscreen) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                activity.requestedOrientation = if (uiState.isLocked) {
                    ActivityInfo.SCREEN_ORIENTATION_LOCKED
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
                activity.requestedOrientation = if (uiState.isLocked) {
                    ActivityInfo.SCREEN_ORIENTATION_LOCKED
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        }

        onDispose {
            if (window != null && controller != null) {
                WindowCompat.setDecorFitsSystemWindows(window, true)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (playablePath != null) {
            AndroidView(
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        player = playerViewModel.player
                        useController = false
                        keepScreenOn = true
                        resizeMode = uiState.resizeMode
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        playerWidthPx = it.width
                        playerHeightPx = it.height
                    }
                    .pointerInput(playerWidthPx, playerHeightPx) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val nextScale = (zoomScale * zoom).coerceIn(MIN_PINCH_SCALE, MAX_PINCH_SCALE)
                            val maxPanX = ((playerWidthPx * (nextScale - 1f)) / 2f).coerceAtLeast(0f)
                            val maxPanY = ((playerHeightPx * (nextScale - 1f)) / 2f).coerceAtLeast(0f)
                            zoomScale = nextScale
                            if (nextScale <= 1.02f) {
                                panOffsetX = 0f
                                panOffsetY = 0f
                            } else {
                                panOffsetX = (panOffsetX + pan.x).coerceIn(-maxPanX, maxPanX)
                                panOffsetY = (panOffsetY + pan.y).coerceIn(-maxPanY, maxPanY)
                            }
                            controlsVisible = true
                        }
                    }
                    .graphicsLayer {
                        scaleX = zoomScale
                        scaleY = zoomScale
                        translationX = panOffsetX
                        translationY = panOffsetY
                    },
                update = { playerView ->
                    playerView.player = playerViewModel.player
                    playerView.resizeMode = uiState.resizeMode
                },
            )

            GestureLayer(
                onToggleControls = {
                    controlsVisible = !controlsVisible
                    if (!controlsVisible) {
                        activePanelName = PlayerPanel.NONE.name
                    }
                },
                    onSeekBack = {
                        val appliedMs = playerViewModel.seekBy(-SEEK_INCREMENT_MS)
                        if (appliedMs != 0L) {
                            gestureFeedback = "-${abs(appliedMs / 1000)}s"
                        }
                        activePanelName = PlayerPanel.NONE.name
                    },
                    onSeekForward = {
                        val appliedMs = playerViewModel.seekBy(SEEK_INCREMENT_MS)
                        if (appliedMs != 0L) {
                            gestureFeedback = "+${abs(appliedMs / 1000)}s"
                        }
                        activePanelName = PlayerPanel.NONE.name
                    },
                onDoubleTapCenter = {
                    val wasPlaying = uiState.isPlaying
                    playerViewModel.togglePlayback()
                    gestureFeedback = if (wasPlaying) "Paused" else "Playing"
                    activePanelName = PlayerPanel.NONE.name
                },
                playerWidthPx = playerWidthPx,
                playerHeightPx = playerHeightPx,
                onAdjustmentStart = { side ->
                    swipeHintVisible = false
                    gestureFeedback = null
                    swipeSeekOverlay = null
                    controlsVisible = false
                    activePanelName = PlayerPanel.NONE.name
                    swipeAdjustmentOverlay = swipeAdjustmentController.start(side)
                },
                onAdjustmentChange = { side, fractionDelta ->
                    swipeAdjustmentOverlay = swipeAdjustmentController.adjust(
                        side = side,
                        deltaFraction = fractionDelta,
                    )
                },
                onAdjustmentEnd = {
                    swipeAdjustmentOverlay = swipeAdjustmentOverlay?.copy(isActive = false)
                },
                onSeekSwipeStart = {
                    swipeHintVisible = false
                    gestureFeedback = null
                    swipeAdjustmentOverlay = null
                    controlsVisible = false
                    activePanelName = PlayerPanel.NONE.name
                    swipeSeekStartPositionMs = uiState.positionMs
                    swipeSeekOverlay = SwipeSeekOverlay(
                        deltaMs = 0L,
                        targetPositionMs = uiState.positionMs,
                        isActive = true,
                    )
                },
                onSeekSwipeChange = { distanceFraction ->
                    val deltaMs = calculateSwipeSeekDeltaMs(
                        distanceFraction = distanceFraction,
                        durationMs = uiState.durationMs,
                    )
                    swipeSeekOverlay = SwipeSeekOverlay(
                        deltaMs = deltaMs,
                        targetPositionMs = (swipeSeekStartPositionMs + deltaMs)
                            .coerceIn(0L, uiState.durationMs.takeIf { it > 0 } ?: Long.MAX_VALUE),
                        isActive = true,
                    )
                },
                onSeekSwipeEnd = {
                    val pendingOverlay = swipeSeekOverlay
                    val requestedDeltaMs = pendingOverlay?.deltaMs ?: 0L
                    if (requestedDeltaMs != 0L) {
                        val appliedDeltaMs = playerViewModel.seekBy(requestedDeltaMs)
                        swipeSeekOverlay = pendingOverlay?.copy(
                            deltaMs = appliedDeltaMs,
                            targetPositionMs = (swipeSeekStartPositionMs + appliedDeltaMs)
                                .coerceIn(0L, uiState.durationMs.takeIf { it > 0 } ?: Long.MAX_VALUE),
                            isActive = false,
                        )
                    } else {
                        swipeSeekOverlay = null
                    }
                },
                onSeekSwipeCancel = {
                    swipeSeekOverlay = null
                },
            )

            if (uiState.isBuffering) {
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp,
                            color = Color.White,
                        )
                        Text(
                            text = "Buffering",
                            modifier = Modifier.padding(start = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = gestureFeedback != null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = (-86).dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    shape = CircleShape,
                ) {
                    Text(
                        text = gestureFeedback.orEmpty(),
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
            }

            swipeAdjustmentOverlay?.let { overlay ->
                SwipeAdjustmentHud(
                    overlay = overlay,
                )
            }

            swipeSeekOverlay?.let { overlay ->
                SwipeSeekHud(
                    overlay = overlay,
                )
            }

            AnimatedVisibility(
                visible = swipeHintVisible && swipeAdjustmentOverlay == null && swipeSeekOverlay == null,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                SwipeGestureHint(
                    controlsVisible = controlsVisible,
                )
            }

            PlayerChrome(
                uiState = uiState,
                title = uiState.title.ifBlank { media?.title.orEmpty() },
                isFullscreen = isFullscreen,
                controlsVisible = controlsVisible || uiState.isBuffering,
                activePanel = activePanel,
                currentPositionMs = if (isScrubbing) scrubPositionMs.toLong() else uiState.positionMs,
                selectedAudioLabel = when {
                    uiState.audioDisabled -> "None"
                    selectedAudioTrack != null -> selectedAudioTrack.title
                    else -> "Auto"
                },
                selectedSubtitleLabel = when {
                    uiState.subtitlesDisabled -> "None"
                    selectedSubtitleTrack != null -> selectedSubtitleTrack.title
                    else -> "Auto"
                },
                canEnterPictureInPicture = uiState.isAvailable && isLikelyVideoFile(playablePath, media?.mimeType),
                isRotationLocked = uiState.isLocked,
                onBack = {
                    playerViewModel.persistPlaybackState()
                    onBack()
                },
                onPlayPause = {
                    playerViewModel.togglePlayback()
                    controlsVisible = true
                },
                onFullscreenToggle = {
                    isFullscreen = !isFullscreen
                    controlsVisible = true
                },
                onEnterPictureInPicture = {
                    (activity as? MainActivity)?.enterPictureInPictureIfPossible()
                },
                onSeekChanged = { value ->
                    isScrubbing = true
                    scrubPositionMs = value
                    controlsVisible = true
                },
                onSeekFinished = {
                    playerViewModel.seekTo(scrubPositionMs.toLong())
                    isScrubbing = false
                    controlsVisible = true
                },
                onTogglePanel = { panel ->
                    controlsVisible = true
                    activePanelName = if (activePanel == panel) PlayerPanel.NONE.name else panel.name
                },
                isZoomed = zoomScale > 1.02f,
                onLockClick = {
                    val willLockRotation = !uiState.isLocked
                    playerViewModel.toggleLock()
                    activePanelName = PlayerPanel.NONE.name
                    controlsVisible = true
                    gestureFeedback = if (willLockRotation) "Rotation locked" else "Rotation unlocked"
                },
                onResetZoom = {
                    zoomScale = 1f
                    panOffsetX = 0f
                    panOffsetY = 0f
                    controlsVisible = true
                },
            )

            AnimatedVisibility(
                visible = (controlsVisible || activePanel != PlayerPanel.NONE) && activePanel != PlayerPanel.NONE,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 18.dp, bottom = 108.dp),
            ) {
                PlayerOptionPanel(
                    panel = activePanel,
                    uiState = uiState,
                    selectedSubtitleTrack = selectedSubtitleTrack,
                    onDismiss = { activePanelName = PlayerPanel.NONE.name },
                    onSelectSpeed = {
                        playerViewModel.setPlaybackSpeed(it)
                        activePanelName = PlayerPanel.NONE.name
                    },
                    onSelectAudioTrack = {
                        playerViewModel.selectAudioTrack(it)
                        activePanelName = PlayerPanel.NONE.name
                    },
                    onDisableAudio = {
                        playerViewModel.disableAudio()
                        activePanelName = PlayerPanel.NONE.name
                    },
                    onEnableAudioAuto = {
                        playerViewModel.selectAudioTrack(null)
                        activePanelName = PlayerPanel.NONE.name
                    },
                    onDisableSubtitles = {
                        playerViewModel.disableSubtitles()
                        activePanelName = PlayerPanel.NONE.name
                    },
                    onEnableSubtitlesAuto = {
                        playerViewModel.enableSubtitlesAuto()
                        activePanelName = PlayerPanel.NONE.name
                    },
                    onSelectSubtitleTrack = {
                        playerViewModel.selectSubtitleTrack(it)
                        activePanelName = PlayerPanel.NONE.name
                    },
                    onSelectResizeMode = {
                        playerViewModel.setResizeMode(it)
                        activePanelName = PlayerPanel.NONE.name
                    },
                    isZoomed = zoomScale > 1.02f,
                    onResetZoom = {
                        zoomScale = 1f
                        panOffsetX = 0f
                        panOffsetY = 0f
                        activePanelName = PlayerPanel.NONE.name
                    },
                )
            }

            uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 84.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(28.dp),
            ) {
                Text(
                    text = "This file is not available for playback.",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun BoxScope.GestureLayer(
    onToggleControls: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onDoubleTapCenter: () -> Unit,
    playerWidthPx: Int,
    playerHeightPx: Int,
    onAdjustmentStart: (SwipeAdjustmentSide) -> Unit,
    onAdjustmentChange: (SwipeAdjustmentSide, Float) -> Unit,
    onAdjustmentEnd: () -> Unit,
    onSeekSwipeStart: () -> Unit,
    onSeekSwipeChange: (Float) -> Unit,
    onSeekSwipeEnd: () -> Unit,
    onSeekSwipeCancel: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(playerWidthPx, playerHeightPx) {
                if (playerWidthPx == 0 || playerHeightPx == 0) return@pointerInput
                var activeMode = SwipeGestureMode.NONE
                var activeSide: SwipeAdjustmentSide? = null
                var totalHorizontalDrag = 0f
                var totalVerticalDrag = 0f
                detectDragGestures(
                    onDragStart = { offset ->
                        activeMode = SwipeGestureMode.PENDING
                        activeSide = if (offset.x < playerWidthPx / 2f) {
                            SwipeAdjustmentSide.LEFT
                        } else {
                            SwipeAdjustmentSide.RIGHT
                        }
                        totalHorizontalDrag = 0f
                        totalVerticalDrag = 0f
                    },
                    onDrag = { _, dragAmount ->
                        totalHorizontalDrag += dragAmount.x
                        totalVerticalDrag += dragAmount.y

                        if (activeMode == SwipeGestureMode.PENDING) {
                            val absHorizontalDrag = abs(totalHorizontalDrag)
                            val absVerticalDrag = abs(totalVerticalDrag)
                            if (
                                absHorizontalDrag < viewConfiguration.touchSlop &&
                                absVerticalDrag < viewConfiguration.touchSlop
                            ) {
                                return@detectDragGestures
                            }

                            activeMode = when {
                                absHorizontalDrag > absVerticalDrag * SWIPE_DIRECTION_LOCK_RATIO -> {
                                    onSeekSwipeStart()
                                    SwipeGestureMode.HORIZONTAL
                                }

                                absVerticalDrag > absHorizontalDrag * SWIPE_DIRECTION_LOCK_RATIO -> {
                                    activeSide?.let(onAdjustmentStart)
                                    SwipeGestureMode.VERTICAL
                                }

                                else -> return@detectDragGestures
                            }
                        }

                        when (activeMode) {
                            SwipeGestureMode.HORIZONTAL -> {
                                onSeekSwipeChange(
                                    (totalHorizontalDrag / playerWidthPx.toFloat()).coerceIn(-1f, 1f),
                                )
                            }

                            SwipeGestureMode.VERTICAL -> {
                                val side = activeSide ?: return@detectDragGestures
                                onAdjustmentChange(
                                    side,
                                    (-totalVerticalDrag / playerHeightPx.toFloat()).coerceIn(-1f, 1f),
                                )
                            }

                            SwipeGestureMode.NONE,
                            SwipeGestureMode.PENDING,
                            -> Unit
                        }
                    },
                    onDragEnd = {
                        when (activeMode) {
                            SwipeGestureMode.HORIZONTAL -> onSeekSwipeEnd()
                            SwipeGestureMode.VERTICAL -> onAdjustmentEnd()
                            SwipeGestureMode.NONE,
                            SwipeGestureMode.PENDING,
                            -> Unit
                        }
                        activeMode = SwipeGestureMode.NONE
                        activeSide = null
                        totalHorizontalDrag = 0f
                        totalVerticalDrag = 0f
                    },
                    onDragCancel = {
                        when (activeMode) {
                            SwipeGestureMode.HORIZONTAL -> onSeekSwipeCancel()
                            SwipeGestureMode.VERTICAL -> onAdjustmentEnd()
                            SwipeGestureMode.NONE,
                            SwipeGestureMode.PENDING,
                            -> Unit
                        }
                        activeMode = SwipeGestureMode.NONE
                        activeSide = null
                        totalHorizontalDrag = 0f
                        totalVerticalDrag = 0f
                    },
                )
            }
            .pointerInput(playerWidthPx) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { offset ->
                        if (playerWidthPx == 0) return@detectTapGestures
                        val leftZoneEnd = playerWidthPx * DOUBLE_TAP_LEFT_ZONE_FRACTION
                        val rightZoneStart = playerWidthPx * DOUBLE_TAP_RIGHT_ZONE_FRACTION
                        when {
                            offset.x < leftZoneEnd -> {
                            onSeekBack()
                            }

                            offset.x > rightZoneStart -> {
                                onSeekForward()
                            }

                            else -> {
                                onDoubleTapCenter()
                            }
                        }
                    },
                )
            },
    )
}

@Composable
private fun BoxScope.SwipeAdjustmentHud(
    overlay: SwipeAdjustmentOverlay,
) {
    val alignment = if (overlay.type == SwipeAdjustmentType.BRIGHTNESS) {
        Alignment.CenterStart
    } else {
        Alignment.CenterEnd
    }
    Surface(
        modifier = Modifier
            .align(alignment)
            .padding(horizontal = 20.dp),
        color = Color.Black.copy(alpha = 0.72f),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = overlay.type.label,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.82f),
            )
            Box(
                modifier = Modifier
                    .size(width = 16.dp, height = 132.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f)),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(fraction = overlay.level.coerceIn(0f, 1f))
                        .background(Color.White),
                )
            }
            Text(
                text = "${overlay.percentText}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun BoxScope.SwipeGestureHint(
    controlsVisible: Boolean,
) {
    Surface(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = if (controlsVisible) 120.dp else 28.dp),
        color = Color.Black.copy(alpha = 0.68f),
        shape = RoundedCornerShape(22.dp),
    ) {
        Text(
            text = "Swipe left or right to seek. Swipe up or down: left brightness, right volume.",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
        )
    }
}

@Composable
private fun BoxScope.SwipeSeekHud(
    overlay: SwipeSeekOverlay,
) {
    Surface(
        modifier = Modifier
            .align(Alignment.Center)
            .offset(y = (-86).dp),
        color = Color.Black.copy(alpha = 0.74f),
        shape = RoundedCornerShape(28.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = if (overlay.isActive) "Seek" else "Jumped",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.78f),
            )
            Text(
                text = formatSignedSeekDelta(overlay.deltaMs),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Text(
                text = "To ${formatPlaybackTime(overlay.targetPositionMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.84f),
            )
        }
    }
}

@Composable
private fun BoxScope.PlayerChrome(
    uiState: PlayerUiState,
    title: String,
    isFullscreen: Boolean,
    controlsVisible: Boolean,
    activePanel: PlayerPanel,
    currentPositionMs: Long,
    selectedAudioLabel: String,
    selectedSubtitleLabel: String,
    canEnterPictureInPicture: Boolean,
    isRotationLocked: Boolean,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onFullscreenToggle: () -> Unit,
    onEnterPictureInPicture: () -> Unit,
    onSeekChanged: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onTogglePanel: (PlayerPanel) -> Unit,
    isZoomed: Boolean,
    onLockClick: () -> Unit,
    onResetZoom: () -> Unit,
) {
    AnimatedVisibility(
        visible = controlsVisible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.46f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.82f),
                        ),
                    ),
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.34f),
            ) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 18.dp, top = 14.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                        )
                    }
                    Text(
                        text = title,
                        modifier = Modifier.padding(start = 6.dp),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White.copy(alpha = 0.94f),
                shape = CircleShape,
                shadowElevation = 10.dp,
            ) {
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(132.dp),
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle,
                        contentDescription = if (uiState.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(84.dp),
                        tint = Color(0xCC111111),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
            ) {
                PlayerTimeline(
                    currentPositionMs = currentPositionMs,
                    durationMs = uiState.durationMs,
                    bufferedPositionMs = uiState.bufferedPositionMs,
                    onSeekChanged = onSeekChanged,
                    onSeekFinished = onSeekFinished,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DockStrip(
                        activePanel = activePanel,
                        playbackSpeed = uiState.playbackSpeed,
                        subtitleLabel = selectedSubtitleLabel,
                        audioLabel = selectedAudioLabel,
                        isFullscreen = isFullscreen,
                        canEnterPictureInPicture = canEnterPictureInPicture,
                        isRotationLocked = isRotationLocked,
                        isZoomed = isZoomed,
                        onResizeClick = { onTogglePanel(PlayerPanel.RESIZE) },
                        onSubtitleClick = { onTogglePanel(PlayerPanel.SUBTITLES) },
                        onAudioClick = { onTogglePanel(PlayerPanel.AUDIO) },
                        onSpeedClick = { onTogglePanel(PlayerPanel.SPEED) },
                        onPictureInPictureClick = onEnterPictureInPicture,
                        onResetZoom = onResetZoom,
                        onLockClick = onLockClick,
                        onFullscreenToggle = onFullscreenToggle,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerTimeline(
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long,
    onSeekChanged: (Float) -> Unit,
    onSeekFinished: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Slider(
                value = currentPositionMs.coerceAtLeast(0L).toFloat(),
                onValueChange = onSeekChanged,
                onValueChangeFinished = onSeekFinished,
                valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.24f),
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth(
                        fraction = if (durationMs > 0L) {
                            (bufferedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        },
                    )
                    .padding(top = 18.dp, start = 12.dp, end = 12.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
            )
        }

        Text(
            text = "${formatPlaybackTime(currentPositionMs)}  •  ${formatPlaybackTime(durationMs)}",
            modifier = Modifier.padding(start = 12.dp, top = 2.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}

@Composable
private fun DockStrip(
    activePanel: PlayerPanel,
    playbackSpeed: Float,
    subtitleLabel: String,
    audioLabel: String,
    isFullscreen: Boolean,
    canEnterPictureInPicture: Boolean,
    isRotationLocked: Boolean,
    isZoomed: Boolean,
    onResizeClick: () -> Unit,
    onSubtitleClick: () -> Unit,
    onAudioClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onPictureInPictureClick: () -> Unit,
    onResetZoom: () -> Unit,
    onLockClick: () -> Unit,
    onFullscreenToggle: () -> Unit,
) {
    Surface(
        color = Color.Black.copy(alpha = 0.44f),
        shape = RoundedCornerShape(26.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DockButton(
                icon = Icons.Outlined.CropFree,
                contentDescription = "Video size",
                selected = activePanel == PlayerPanel.RESIZE,
                onClick = onResizeClick,
            )
            DockButton(
                icon = Icons.Outlined.Subtitles,
                contentDescription = "Subtitles",
                selected = activePanel == PlayerPanel.SUBTITLES || subtitleLabel.equals("None", ignoreCase = false).not(),
                onClick = onSubtitleClick,
            )
            DockButton(
                icon = Icons.Outlined.GraphicEq,
                contentDescription = "Audio tracks",
                selected = activePanel == PlayerPanel.AUDIO || !audioLabel.equals("Auto", ignoreCase = true),
                onClick = onAudioClick,
            )
            DockButton(
                icon = Icons.Outlined.Speed,
                contentDescription = "Playback speed",
                selected = activePanel == PlayerPanel.SPEED || abs(playbackSpeed - 1f) > 0.01f,
                onClick = onSpeedClick,
            )
            if (isZoomed) {
                DockButton(
                    icon = Icons.Outlined.Check,
                    contentDescription = "Reset zoom",
                    selected = true,
                    onClick = onResetZoom,
                )
            }
            if (canEnterPictureInPicture) {
                DockButton(
                    icon = Icons.Outlined.PictureInPictureAlt,
                    contentDescription = "Picture in picture",
                    selected = false,
                    onClick = onPictureInPictureClick,
                )
            }
            DockButton(
                icon = Icons.Outlined.Lock,
                contentDescription = if (isRotationLocked) "Unlock rotation" else "Lock rotation",
                selected = isRotationLocked,
                onClick = onLockClick,
            )
            DockButton(
                icon = if (isFullscreen) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                contentDescription = if (isFullscreen) "Exit fullscreen" else "Enter fullscreen",
                selected = isFullscreen,
                onClick = onFullscreenToggle,
            )
        }
    }
}

@Composable
private fun DockButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) Color.White.copy(alpha = 0.18f) else Color.Transparent,
        shape = RoundedCornerShape(18.dp),
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun PlayerOptionPanel(
    panel: PlayerPanel,
    uiState: PlayerUiState,
    selectedSubtitleTrack: PlayerTrackOption?,
    onDismiss: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectAudioTrack: (PlayerTrackOption?) -> Unit,
    onDisableAudio: () -> Unit,
    onEnableAudioAuto: () -> Unit,
    onDisableSubtitles: () -> Unit,
    onEnableSubtitlesAuto: () -> Unit,
    onSelectSubtitleTrack: (PlayerTrackOption?) -> Unit,
    onSelectResizeMode: (Int) -> Unit,
    isZoomed: Boolean,
    onResetZoom: () -> Unit,
) {
    Surface(
        color = Color.Black.copy(alpha = 0.78f),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 8.dp,
        modifier = Modifier.widthIn(min = 240.dp, max = 320.dp),
    ) {
        Column(
            modifier = Modifier
                .heightIn(max = 360.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = panel.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                TextButton(onClick = onDismiss) {
                    Text("Close", color = Color.White.copy(alpha = 0.86f))
                }
            }

            when (panel) {
                PlayerPanel.SPEED -> {
                    SPEED_OPTIONS.forEach { speed ->
                        PanelOptionRow(
                            title = formatSpeed(speed),
                            subtitle = if (speed == 1f) "Normal" else null,
                            selected = abs(uiState.playbackSpeed - speed) < 0.01f,
                            onClick = { onSelectSpeed(speed) },
                        )
                    }
                }

                PlayerPanel.AUDIO -> {
                    PanelOptionRow(
                        title = "None",
                        subtitle = "Mute this file in the player",
                        selected = uiState.audioDisabled,
                        onClick = onDisableAudio,
                    )
                    PanelOptionRow(
                        title = "Auto",
                        subtitle = "Let player choose",
                        selected = !uiState.audioDisabled && uiState.audioTracks.none { it.isSelected },
                        onClick = onEnableAudioAuto,
                    )
                    if (uiState.audioTracks.isEmpty()) {
                        EmptyPanelMessage("No alternate audio tracks detected.")
                    } else {
                        uiState.audioTracks.forEach { track ->
                            PanelOptionRow(
                                title = track.title,
                                subtitle = track.subtitle,
                                selected = !uiState.audioDisabled && track.isSelected,
                                onClick = { onSelectAudioTrack(track) },
                            )
                        }
                    }
                }

                PlayerPanel.SUBTITLES -> {
                    PanelOptionRow(
                        title = "None",
                        subtitle = "Disable subtitles",
                        selected = uiState.subtitlesDisabled,
                        onClick = onDisableSubtitles,
                    )
                    PanelOptionRow(
                        title = "Auto",
                        subtitle = "Let player choose",
                        selected = !uiState.subtitlesDisabled && selectedSubtitleTrack == null,
                        onClick = onEnableSubtitlesAuto,
                    )
                    uiState.subtitleTracks.forEach { track ->
                        PanelOptionRow(
                            title = track.title,
                            subtitle = track.subtitle,
                            selected = !uiState.subtitlesDisabled && track.isSelected,
                            onClick = { onSelectSubtitleTrack(track) },
                        )
                    }
                }

                PlayerPanel.RESIZE -> {
                    RESIZE_OPTIONS.forEach { option ->
                        PanelOptionRow(
                            title = option.label,
                            subtitle = option.subtitle,
                            selected = uiState.resizeMode == option.resizeMode,
                            onClick = { onSelectResizeMode(option.resizeMode) },
                        )
                    }
                    if (isZoomed) {
                        PanelOptionRow(
                            title = "Reset pinch zoom",
                            subtitle = "Return to the default framing",
                            selected = false,
                            onClick = onResetZoom,
                        )
                    }
                }

                PlayerPanel.NONE -> Unit
            }
        }
    }
}

@Composable
private fun PanelOptionRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        color = if (selected) Color.White.copy(alpha = 0.10f) else Color.Transparent,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    subtitle?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.68f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyPanelMessage(message: String) {
    Text(
        text = message,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.76f),
    )
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun formatPlaybackTime(timeMs: Long): String {
    val totalSeconds = (timeMs.coerceAtLeast(0L) / 1000L).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private fun isLikelyVideoFile(path: String?, mimeType: String? = null): Boolean {
    val normalizedMimeType = mimeType?.lowercase().orEmpty()
    if (normalizedMimeType.startsWith("video/")) return true
    val extension = path?.substringAfterLast('.', "")?.lowercase().orEmpty()
    return extension in setOf("mp4", "mkv", "webm", "mov", "avi", "m4v", "3gp", "ts", "m2ts", "mpeg", "mpg")
}

private fun isLikelyAudioFile(path: String?, mimeType: String? = null): Boolean {
    val normalizedMimeType = mimeType?.lowercase().orEmpty()
    if (normalizedMimeType.startsWith("audio/")) return true
    val extension = path?.substringAfterLast('.', "")?.lowercase().orEmpty()
    return extension in setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "amr", "3ga", "wma")
}

private fun formatSpeed(speed: Float): String {
    return if (abs(speed - speed.toInt().toFloat()) < 0.01f) {
        "${speed.toInt()}x"
    } else {
        "${speed}x"
    }
}

private fun formatSignedSeekDelta(deltaMs: Long): String {
    val sign = when {
        deltaMs > 0L -> "+"
        deltaMs < 0L -> "-"
        else -> ""
    }
    val seconds = (abs(deltaMs) / 1000L).coerceAtLeast(0L)
    return "$sign${seconds}s"
}

private fun calculateSwipeSeekDeltaMs(
    distanceFraction: Float,
    durationMs: Long,
): Long {
    if (distanceFraction == 0f) return 0L
    val maxSeekSeconds = if (durationMs > 0L) {
        (durationMs / 1000f * SEEK_SWIPE_DURATION_FRACTION)
            .coerceIn(MIN_SWIPE_SEEK_SECONDS, MAX_SWIPE_SEEK_SECONDS)
    } else {
        DEFAULT_MAX_SWIPE_SEEK_SECONDS
    }
    val scaledSeconds = maxSeekSeconds * abs(distanceFraction).coerceIn(0f, 1f).pow(SEEK_SWIPE_CURVE_POWER)
    val roundedSeconds = scaledSeconds.roundToInt().coerceAtLeast(1)
    return roundedSeconds * 1000L * distanceFraction.signAsLong()
}

private fun Float.signAsLong(): Long {
    return when {
        this > 0f -> 1L
        this < 0f -> -1L
        else -> 0L
    }
}

private enum class SwipeAdjustmentSide {
    LEFT,
    RIGHT,
}

private enum class SwipeGestureMode {
    NONE,
    PENDING,
    HORIZONTAL,
    VERTICAL,
}

private enum class SwipeAdjustmentType(val label: String) {
    BRIGHTNESS("Brightness"),
    VOLUME("Volume"),
}

private data class SwipeAdjustmentOverlay(
    val type: SwipeAdjustmentType,
    val level: Float,
    val isActive: Boolean,
) {
    val percentText: Int
        get() = (level.coerceIn(0f, 1f) * 100f).roundToInt()
}

private data class SwipeSeekOverlay(
    val deltaMs: Long,
    val targetPositionMs: Long,
    val isActive: Boolean,
)

private data class ResizeOption(
    val resizeMode: Int,
    val label: String,
    val subtitle: String,
)

private class PlayerSwipeAdjustmentController(
    private val context: Context,
    private val activity: Activity?,
) {
    private val audioManager =
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var startingBrightness = DEFAULT_GESTURE_LEVEL
    private var startingVolume = DEFAULT_GESTURE_LEVEL

    fun start(side: SwipeAdjustmentSide): SwipeAdjustmentOverlay {
        return when (side) {
            SwipeAdjustmentSide.LEFT -> {
                startingBrightness = currentBrightness()
                SwipeAdjustmentOverlay(
                    type = SwipeAdjustmentType.BRIGHTNESS,
                    level = startingBrightness,
                    isActive = true,
                )
            }

            SwipeAdjustmentSide.RIGHT -> {
                startingVolume = currentVolume()
                SwipeAdjustmentOverlay(
                    type = SwipeAdjustmentType.VOLUME,
                    level = startingVolume,
                    isActive = true,
                )
            }
        }
    }

    fun adjust(
        side: SwipeAdjustmentSide,
        deltaFraction: Float,
    ): SwipeAdjustmentOverlay {
        return when (side) {
            SwipeAdjustmentSide.LEFT -> {
                val updatedBrightness = (startingBrightness + deltaFraction)
                    .coerceIn(MIN_BRIGHTNESS_LEVEL, 1f)
                applyBrightness(updatedBrightness)
                SwipeAdjustmentOverlay(
                    type = SwipeAdjustmentType.BRIGHTNESS,
                    level = updatedBrightness,
                    isActive = true,
                )
            }

            SwipeAdjustmentSide.RIGHT -> {
                val updatedVolume = (startingVolume + deltaFraction).coerceIn(0f, 1f)
                applyVolume(updatedVolume)
                SwipeAdjustmentOverlay(
                    type = SwipeAdjustmentType.VOLUME,
                    level = updatedVolume,
                    isActive = true,
                )
            }
        }
    }

    private fun currentBrightness(): Float {
        val windowBrightness = activity?.window?.attributes?.screenBrightness
        if (windowBrightness != null && windowBrightness in 0f..1f) {
            return windowBrightness.coerceIn(MIN_BRIGHTNESS_LEVEL, 1f)
        }
        val systemBrightness = runCatching {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
            ) / 255f
        }.getOrDefault(DEFAULT_GESTURE_LEVEL)
        return systemBrightness.coerceIn(MIN_BRIGHTNESS_LEVEL, 1f)
    }

    private fun applyBrightness(level: Float) {
        val window = activity?.window ?: return
        val attributes = window.attributes
        attributes.screenBrightness = level.coerceIn(MIN_BRIGHTNESS_LEVEL, 1f)
        window.attributes = attributes
    }

    private fun currentVolume(): Float {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return currentVolume.toFloat() / maxVolume.toFloat()
    }

    private fun applyVolume(level: Float) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val targetVolume = (level.coerceIn(0f, 1f) * maxVolume.toFloat()).roundToInt()
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            targetVolume.coerceIn(0, maxVolume),
            0,
        )
    }
}

private enum class PlayerPanel(val title: String) {
    NONE(""),
    SPEED("Playback speed"),
    AUDIO("Audio track"),
    SUBTITLES("Subtitles"),
    RESIZE("Video size"),
}

private val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)

private val RESIZE_OPTIONS = listOf(
    ResizeOption(
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
        label = "Fit",
        subtitle = "Natural view with the whole frame visible",
    ),
    ResizeOption(
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        label = "Crop",
        subtitle = "Cinema fill that crops the edges",
    ),
    ResizeOption(
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL,
        label = "Fill",
        subtitle = "Stretch to fill the whole player area",
    ),
    ResizeOption(
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH,
        label = "Full width",
        subtitle = "Keep width locked and allow taller framing",
    ),
    ResizeOption(
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT,
        label = "Full height",
        subtitle = "Keep height locked and allow wider framing",
    ),
)

private const val SEEK_INCREMENT_MS = 10_000L
private const val CONTROLS_AUTO_HIDE_MS = 3_000L
private const val GESTURE_FEEDBACK_MS = 900L
private const val SWIPE_HINT_MS = 5_000L
private const val MIN_PINCH_SCALE = 1f
private const val MAX_PINCH_SCALE = 3f
private const val DEFAULT_GESTURE_LEVEL = 0.5f
private const val MIN_BRIGHTNESS_LEVEL = 0.05f
private const val SWIPE_DIRECTION_LOCK_RATIO = 1.2f
private const val SEEK_SWIPE_DURATION_FRACTION = 0.12f
private const val SEEK_SWIPE_CURVE_POWER = 1.15f
private const val MIN_SWIPE_SEEK_SECONDS = 10f
private const val MAX_SWIPE_SEEK_SECONDS = 180f
private const val DEFAULT_MAX_SWIPE_SEEK_SECONDS = 90f
private const val DOUBLE_TAP_LEFT_ZONE_FRACTION = 0.35f
private const val DOUBLE_TAP_RIGHT_ZONE_FRACTION = 0.65f
