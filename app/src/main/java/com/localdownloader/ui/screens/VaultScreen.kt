package com.localdownloader.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localdownloader.R
import com.localdownloader.domain.models.DownloadTask
import com.localdownloader.domain.models.SingleVaultSettings
import com.localdownloader.domain.models.getAllVaults
import com.localdownloader.media.isLikelyAudioPath
import com.localdownloader.media.isLikelyVideoPath
import com.localdownloader.ui.components.PreferenceItem
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceSubtitle
import com.localdownloader.ui.components.PreferenceSwitch
import com.localdownloader.ui.screens.settings.SettingChoiceDialog
import com.localdownloader.ui.screens.settings.SettingChoiceDialogState
import com.localdownloader.ui.screens.settings.SettingChoiceOption
import com.localdownloader.viewmodel.DownloadUiState
import com.localdownloader.viewmodel.DownloadViewModel
import com.localdownloader.viewmodel.VaultUiState
import com.localdownloader.viewmodel.VaultViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun VaultScreen(
    vaultViewModel: VaultViewModel,
    downloadViewModel: DownloadViewModel,
    onBack: () -> Unit,
    onPlayVideo: (String) -> Unit,
    onPlayAudio: (String, List<DownloadTask>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vaultState by vaultViewModel.uiState.collectAsStateWithLifecycle()
    val downloadState by downloadViewModel.uiState.collectAsStateWithLifecycle()

    val allVaults = remember(vaultState.vaultSettings) {
        vaultState.vaultSettings.getAllVaults()
    }

    when {
        vaultState.showSetup || allVaults.isEmpty() -> {
            VaultSetupScreen(
                vaultViewModel = vaultViewModel,
                isFirstVault = allVaults.isEmpty(),
                onBack = {
                    if (allVaults.isEmpty()) {
                        onBack()
                    } else {
                        vaultViewModel.showSetupScreen(false)
                    }
                },
            )
        }

        vaultState.unlockingVaultId != null -> {
            val targetVaultId = vaultState.unlockingVaultId ?: return
            val targetVault = allVaults.firstOrNull { it.id == targetVaultId }
            val targetVaultName = targetVault?.name ?: stringResource(R.string.vault_title)
            val isBiometricAllowed = targetVault?.isBiometricEnabled == true

            BackHandler { vaultViewModel.cancelUnlock() }
            VaultUnlockScreen(
                vaultName = targetVaultName,
                isBiometricAllowed = isBiometricAllowed,
                errorMessage = vaultState.errorMessage,
                onCancel = { vaultViewModel.cancelUnlock() },
                onUnlock = { pin ->
                    vaultViewModel.unlockVault(targetVaultId, pin) { _ -> }
                },
                onBiometricUnlock = {
                    vaultViewModel.unlockVaultDirectly(targetVaultId)
                },
            )
        }

        vaultState.activeVaultId != null -> {
            val activeId = vaultState.activeVaultId ?: return
            val activeVault = allVaults.firstOrNull { it.id == activeId }
            val activeVaultName = activeVault?.name ?: stringResource(R.string.vault_title)

            val vaultItems = remember(downloadState.tasks, activeId) {
                downloadState.tasks.filter { task ->
                    isTaskInVault(task, activeId, allVaults.map { it.id })
                }
            }

            BackHandler {
                vaultViewModel.lockActiveVault()
            }

            VaultContentScreen(
                vaultItems = vaultItems,
                vaultName = activeVaultName,
                activeId = activeId,
                activeVaultSettings = activeVault,
                vaultState = vaultState,
                downloadState = downloadState,
                vaultViewModel = vaultViewModel,
                downloadViewModel = downloadViewModel,
                onBack = { vaultViewModel.lockActiveVault() },
                onPlayVideo = onPlayVideo,
                onPlayAudio = onPlayAudio,
                modifier = modifier,
            )
        }

        else -> {
            VaultSelectorScreen(
                vaults = allVaults,
                tasks = downloadState.tasks,
                onSelectVault = { vaultId -> vaultViewModel.selectVaultForUnlock(vaultId) },
                onCreateNewClick = { vaultViewModel.showSetupScreen(true) },
                onBack = onBack,
            )
        }
    }
}

private fun isTaskInVault(task: DownloadTask, activeVaultId: String, allVaultIds: List<String>): Boolean {
    if (!task.isInVault) return false
    val path = task.outputPath ?: return false

    val otherVaultIds = allVaultIds.filter { it != activeVaultId }
    if (activeVaultId == "default") {
        return otherVaultIds.none { path.contains("/vault/$it/") }
    }
    return path.contains("/vault/$activeVaultId/")
}

@Composable
private fun VaultSetupScreen(
    vaultViewModel: VaultViewModel,
    isFirstVault: Boolean,
    onBack: () -> Unit,
) {
    var step by remember { mutableIntStateOf(1) } // 1: Enter PIN, 2: Confirm PIN, 3: Vault Details
    var initialPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var vaultName by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    fun triggerShake(message: String) {
        errorMessage = message
        coroutineScope.launch {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    0f at 0
                    -20f at 50
                    20f at 100
                    -15f at 150
                    15f at 200
                    -8f at 250
                    8f at 300
                    0f at 350
                },
            )
        }
    }

    PreferencePageScaffold(
        title = if (isFirstVault) "Set Up Private Vault" else "Create New Vault",
        onBack = {
            if (step > 1) {
                step = 1
                initialPin = ""
                confirmPin = ""
                errorMessage = null
            } else {
                onBack()
            }
        },
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header Shield Icon
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.size(72.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                // Step Breadcrumbs
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SetupStepChip(stepNumber = 1, label = "PIN", isActive = step == 1, isCompleted = step > 1)
                    Icon(
                        imageVector = Icons.AutoMirrored.rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    SetupStepChip(stepNumber = 2, label = "Confirm", isActive = step == 2, isCompleted = step > 2)
                    Icon(
                        imageVector = Icons.AutoMirrored.rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    SetupStepChip(stepNumber = 3, label = "Details", isActive = step == 3, isCompleted = false)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = when (step) {
                            1 -> "Create a 4–8 Digit PIN"
                            2 -> "Confirm Your PIN"
                            else -> "Name Your Vault"
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = when (step) {
                            1 -> "Choose a secure numerical code to protect your files."
                            2 -> "Re-enter the same PIN to make sure you remember it."
                            else -> "Give this vault a recognizable label."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                when (step) {
                    1 -> {
                        PinDotsIndicator(
                            pinLength = initialPin.length,
                            maxExpectedLength = 6,
                            isError = errorMessage != null,
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        PinKeypad(
                            onDigitClick = { digit ->
                                if (initialPin.length < 8) {
                                    initialPin += digit
                                    errorMessage = null
                                }
                            },
                            onBackspaceClick = {
                                if (initialPin.isNotEmpty()) {
                                    initialPin = initialPin.dropLast(1)
                                    errorMessage = null
                                }
                            },
                            onClearClick = {
                                initialPin = ""
                                errorMessage = null
                            },
                        )

                        Button(
                            onClick = {
                                if (initialPin.length < 4) {
                                    triggerShake("PIN must be at least 4 digits")
                                } else {
                                    errorMessage = null
                                    step = 2
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            enabled = initialPin.length >= 4,
                        ) {
                            Text("Continue")
                        }
                    }

                    2 -> {
                        PinDotsIndicator(
                            pinLength = confirmPin.length,
                            maxExpectedLength = initialPin.length,
                            isError = errorMessage != null,
                        )

                        if (errorMessage != null) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        PinKeypad(
                            onDigitClick = { digit ->
                                if (confirmPin.length < initialPin.length) {
                                    confirmPin += digit
                                    errorMessage = null
                                    if (confirmPin.length == initialPin.length) {
                                        if (confirmPin == initialPin) {
                                            step = 3
                                        } else {
                                            triggerShake("PINs do not match. Try again.")
                                            confirmPin = ""
                                        }
                                    }
                                }
                            },
                            onBackspaceClick = {
                                if (confirmPin.isNotEmpty()) {
                                    confirmPin = confirmPin.dropLast(1)
                                    errorMessage = null
                                }
                            },
                            onClearClick = {
                                confirmPin = ""
                                errorMessage = null
                            },
                        )
                    }

                    3 -> {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                OutlinedTextField(
                                    value = vaultName,
                                    onValueChange = { vaultName = it },
                                    label = { Text("Vault Name") },
                                    placeholder = { Text("e.g. Personal, Work, Media") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                Button(
                                    onClick = {
                                        vaultViewModel.createNewVault(vaultName, initialPin)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                ) {
                                    Icon(imageVector = Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Create & Open Vault")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupStepChip(stepNumber: Int, label: String, isActive: Boolean, isCompleted: Boolean) {
    val bgColor = when {
        isCompleted -> MaterialTheme.colorScheme.primaryContainer
        isActive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val textColor = when {
        isCompleted -> MaterialTheme.colorScheme.onPrimaryContainer
        isActive -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = bgColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$stepNumber.",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
            )
        }
    }
}

@Composable
private fun VaultUnlockScreen(
    vaultName: String,
    isBiometricAllowed: Boolean,
    errorMessage: String?,
    onCancel: () -> Unit,
    onUnlock: (String) -> Unit,
    onBiometricUnlock: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    val shakeOffset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            pin = ""
            coroutineScope.launch {
                shakeOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = keyframes {
                        durationMillis = 350
                        0f at 0
                        -20f at 50
                        20f at 100
                        -15f at 150
                        15f at 200
                        -8f at 250
                        8f at 300
                        0f at 350
                    },
                )
            }
        }
    }

    PreferencePageScaffold(
        title = "Unlock Vault",
        onBack = onCancel,
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header Lock Icon
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.size(76.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(38.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = vaultName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Enter your PIN to access protected files",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // PIN Indicator
                PinDotsIndicator(
                    pinLength = pin.length,
                    maxExpectedLength = 6,
                    isError = errorMessage != null,
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                // Keypad
                PinKeypad(
                    onDigitClick = { digit ->
                        if (pin.length < 8) {
                            val nextPin = pin + digit
                            pin = nextPin
                            if (nextPin.length >= 4) {
                                onUnlock(nextPin)
                            }
                        }
                    },
                    onBackspaceClick = {
                        if (pin.isNotEmpty()) pin = pin.dropLast(1)
                    },
                    onClearClick = {
                        pin = ""
                    },
                    showBiometricButton = isBiometricAllowed,
                    onBiometricClick = onBiometricUnlock,
                )

                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text("Cancel", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun PinDotsIndicator(
    pinLength: Int,
    maxExpectedLength: Int = 6,
    isError: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp),
    ) {
        val totalSlots = maxOf(4, minOf(pinLength + 1, maxExpectedLength))
        for (i in 0 until totalSlots) {
            val isFilled = i < pinLength
            val dotColor by animateColorAsState(
                targetValue = when {
                    isError -> MaterialTheme.colorScheme.error
                    isFilled -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
                animationSpec = tween(150),
                label = "dotColor",
            )

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (isFilled) dotColor else Color.Transparent)
                    .border(2.dp, dotColor, CircleShape),
            )
        }
    }
}

@Composable
private fun PinKeypad(
    onDigitClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit,
    showBiometricButton: Boolean = false,
    onBiometricClick: () -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        val keyLayout = listOf(
            listOf("1" to "", "2" to "ABC", "3" to "DEF"),
            listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
            listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
        )

        keyLayout.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                row.forEach { (digit, subtext) ->
                    KeypadNumberButton(
                        digit = digit,
                        subtext = subtext,
                        onClick = { onDigitClick(digit) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Row 4: Biometric/Clear, 0, Backspace
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBiometricButton) {
                KeypadIconButton(
                    icon = Icons.Outlined.Fingerprint,
                    onClick = onBiometricClick,
                    modifier = Modifier.weight(1f),
                )
            } else {
                KeypadTextButton(
                    text = "Clear",
                    onClick = onClearClick,
                    modifier = Modifier.weight(1f),
                )
            }

            KeypadNumberButton(
                digit = "0",
                subtext = "+",
                onClick = { onDigitClick("0") },
                modifier = Modifier.weight(1f),
            )

            KeypadIconButton(
                icon = Icons.Outlined.Backspace,
                onClick = onBackspaceClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun KeypadNumberButton(
    digit: String,
    subtext: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = modifier
            .height(68.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = digit,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
            if (subtext.isNotBlank()) {
                Text(
                    text = subtext,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun KeypadIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = modifier
            .height(68.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun KeypadTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
        modifier = modifier
            .height(68.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun VaultSelectorScreen(
    vaults: List<SingleVaultSettings>,
    tasks: List<DownloadTask>,
    onSelectVault: (String) -> Unit,
    onCreateNewClick: () -> Unit,
    onBack: () -> Unit,
) {
    PreferencePageScaffold(
        title = "Private Vaults",
        onBack = onBack,
    ) {
        // Hero Security Banner Card
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Hardware-Isolated Vaults",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Protected media stays hidden from gallery and system apps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            PreferenceSubtitle(text = "YOUR VAULTS (${vaults.size})")
        }

        items(vaults, key = { it.id }) { vault ->
            val itemCount = remember(tasks, vault.id) {
                tasks.count { isTaskInVault(it, vault.id, vaults.map { v -> v.id }) }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelectVault(vault.id) },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = vault.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "$itemCount items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (vault.isBiometricEnabled) {
                                    Text(
                                        text = "• Biometric",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onCreateNewClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Another Vault")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultContentScreen(
    vaultItems: List<DownloadTask>,
    vaultName: String,
    activeId: String,
    activeVaultSettings: SingleVaultSettings?,
    vaultState: VaultUiState,
    downloadState: DownloadUiState,
    vaultViewModel: VaultViewModel,
    downloadViewModel: DownloadViewModel,
    onBack: () -> Unit,
    onPlayVideo: (String) -> Unit,
    onPlayAudio: (String, List<DownloadTask>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSettingsModal by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableIntStateOf(0) } // 0: All, 1: Videos, 2: Audios, 3: Other
    val context = LocalContext.current
    val notPlayableMessage = stringResource(R.string.vault_file_not_playable)

    val sortedAndFilteredItems = remember(vaultItems, selectedCategory, searchQuery, activeVaultSettings?.sortOrder) {
        val filtered = vaultItems.filter { item ->
            val matchesSearch = searchQuery.isBlank() || item.title.contains(searchQuery, ignoreCase = true)
            val path = item.outputPath.orEmpty()
            val isVideo = isLikelyVideoPath(path)
            val isAudio = isLikelyAudioPath(path)
            val matchesCategory = when (selectedCategory) {
                0 -> true
                1 -> isVideo
                2 -> isAudio
                3 -> !isVideo && !isAudio
                else -> true
            }
            matchesSearch && matchesCategory
        }

        when (activeVaultSettings?.sortOrder) {
            "oldest" -> filtered.sortedBy { it.createdAt }
            "name" -> filtered.sortedBy { it.title.lowercase() }
            "size" -> filtered.sortedByDescending { it.fileSizeBytes }
            else -> filtered.sortedByDescending { it.createdAt }
        }
    }

    if (showSettingsModal && activeVaultSettings != null) {
        VaultSettingsModal(
            vault = activeVaultSettings,
            vaultItems = vaultItems,
            vaultViewModel = vaultViewModel,
            downloadViewModel = downloadViewModel,
            tasks = downloadState.tasks,
            onDismiss = { showSettingsModal = false },
        )
    }

    PreferencePageScaffold(
        title = vaultName,
        onBack = onBack,
        modifier = modifier,
        actions = {
            IconButton(onClick = { showSettingsModal = true }) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Vault Settings",
                )
            }
        },
    ) {
        // Search Bar with 16dp margins
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search files in vault...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Clear search",
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }

        // Clean Single-Row Filter Chips (Replacing bulky TabRow)
        item {
            val videoCount = vaultItems.count { isLikelyVideoPath(it.outputPath.orEmpty()) }
            val audioCount = vaultItems.count { isLikelyAudioPath(it.outputPath.orEmpty()) }
            val otherCount = vaultItems.size - videoCount - audioCount

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == 0,
                        onClick = { selectedCategory = 0 },
                        label = { Text("All (${vaultItems.size})") },
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategory == 1,
                        onClick = { selectedCategory = 1 },
                        label = { Text("Videos ($videoCount)") },
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategory == 2,
                        onClick = { selectedCategory = 2 },
                        label = { Text("Audios ($audioCount)") },
                    )
                }
                item {
                    FilterChip(
                        selected = selectedCategory == 3,
                        onClick = { selectedCategory = 3 },
                        label = { Text("Other ($otherCount)") },
                    )
                }
            }
        }

        if (sortedAndFilteredItems.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(30.dp),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching files" else "Vault is Empty",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                "No items in this vault match \"$searchQuery\"."
                            } else {
                                "Move downloads into this vault to keep them encrypted and hidden from public device apps."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )

                        if (searchQuery.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Security,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Text(
                                            text = "How to move files into Vault",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    Text(
                                        text = "1. Open Downloads tab.\n2. Tap the menu (⋮) on any video or audio file.\n3. Tap 'Move to Vault'.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            items(sortedAndFilteredItems, key = { it.id }) { item ->
                val path = item.outputPath.orEmpty()
                val isAudio = isLikelyAudioPath(path)
                val isVideo = isLikelyVideoPath(path)

                VaultMediaCard(
                    item = item,
                    isVideo = isVideo,
                    isAudio = isAudio,
                    onMoveToDownloads = { taskId ->
                        downloadViewModel.moveFromVault(taskId)
                    },
                    onPlayClick = {
                        when {
                            isAudio -> {
                                val audioTasks = vaultItems.filter {
                                    isLikelyAudioPath(it.outputPath.orEmpty())
                                }
                                onPlayAudio(item.id, audioTasks)
                            }
                            isVideo -> onPlayVideo(item.id)
                            else -> {
                                Toast.makeText(context, notPlayableMessage, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun VaultMediaCard(
    item: DownloadTask,
    isVideo: Boolean,
    isAudio: Boolean,
    onMoveToDownloads: (String) -> Unit,
    onPlayClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onPlayClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isVideo) MaterialTheme.colorScheme.primaryContainer else if (isAudio) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isVideo) Icons.Outlined.Movie else if (isAudio) Icons.Outlined.Audiotrack else Icons.Outlined.Description,
                        contentDescription = null,
                        tint = if (isVideo) MaterialTheme.colorScheme.primary else if (isAudio) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.outputPath?.substringAfterLast('/') ?: "Protected item",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(
                onClick = { onMoveToDownloads(item.id) },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoveToInbox,
                    contentDescription = "Move out to Downloads",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VaultSettingsModal(
    vault: SingleVaultSettings,
    vaultItems: List<DownloadTask>,
    vaultViewModel: VaultViewModel,
    downloadViewModel: DownloadViewModel,
    tasks: List<DownloadTask>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var choiceDialog by remember { mutableStateOf<SettingChoiceDialogState?>(null) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showExportConfirm by remember { mutableStateOf(false) }
    var showAddRuleDialog by remember { mutableStateOf(false) }

    choiceDialog?.let { state ->
        SettingChoiceDialog(
            state = state,
            onDismiss = { choiceDialog = null },
        )
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            vaultId = vault.id,
            currentPinHash = vault.pinHash,
            onSuccess = { newPin ->
                vaultViewModel.updateVaultPin(vault.id, newPin)
                showChangePinDialog = false
            },
            onDismiss = { showChangePinDialog = false },
        )
    }

    if (showRenameDialog) {
        var nameInput by remember { mutableStateOf(vault.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Vault") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true,
                    label = { Text("Vault Name") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isNotBlank()) {
                            vaultViewModel.renameVault(vault.id, nameInput.trim())
                            showRenameDialog = false
                        }
                    },
                    enabled = nameInput.isNotBlank(),
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showAddRuleDialog) {
        var ruleInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddRuleDialog = false },
            title = { Text("Add Auto-Move URL Rule") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Downloads from URLs containing this domain will automatically move into this vault.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = ruleInput,
                        onValueChange = { ruleInput = it },
                        placeholder = { Text("e.g. instagram.com, tiktok.com") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (ruleInput.isNotBlank()) {
                            vaultViewModel.addAutoMoveRule(vault.id, ruleInput.trim())
                            showAddRuleDialog = false
                        }
                    },
                    enabled = ruleInput.isNotBlank(),
                ) {
                    Text("Add Rule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRuleDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showExportConfirm) {
        AlertDialog(
            onDismissRequest = { showExportConfirm = false },
            title = { Text("Export all files to Downloads?") },
            text = {
                Text("This will move all ${vaultItems.size} files from this private vault back into your regular public downloads directory.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        vaultItems.forEach { downloadViewModel.moveFromVault(it.id) }
                        showExportConfirm = false
                    },
                ) {
                    Text("Export All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${vault.name}?") },
            text = {
                Text("Are you sure? This will permanently delete this vault and all ${vaultItems.size} files stored inside it from your device. This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDismiss()
                        vaultViewModel.deleteVault(vault.id, tasks)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${vault.name} Settings",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = "Close")
                    }
                }
            }

            // Section: Security & Access
            item {
                PreferenceSubtitle(text = "SECURITY & ACCESS")
            }
            item {
                PreferenceItem(
                    icon = Icons.Outlined.Password,
                    title = "Change Vault PIN",
                    description = "Update the numerical passcode for this vault",
                    onClick = { showChangePinDialog = true },
                )
            }
            item {
                PreferenceSwitch(
                    icon = Icons.Outlined.Fingerprint,
                    title = "Biometric unlock",
                    description = "Unlock using device fingerprint or face verification",
                    isChecked = vault.isBiometricEnabled,
                    onClick = {
                        vaultViewModel.setVaultBiometricEnabled(vault.id, !vault.isBiometricEnabled)
                    },
                )
            }
            item {
                PreferenceItem(
                    icon = Icons.Outlined.LockClock,
                    title = "Auto-lock timeout",
                    description = when (vault.autoLockTimeoutSeconds) {
                        0 -> "Immediately on background (Recommended)"
                        30 -> "After 30 seconds"
                        60 -> "After 1 minute"
                        300 -> "After 5 minutes"
                        -1 -> "Never (until manually locked)"
                        else -> "${vault.autoLockTimeoutSeconds}s"
                    },
                    onClick = {
                        choiceDialog = SettingChoiceDialogState(
                            title = "Auto-Lock Timeout",
                            selected = "${vault.autoLockTimeoutSeconds}",
                            options = listOf(
                                0 to "Immediately on background (Recommended)",
                                30 to "After 30 seconds",
                                60 to "After 1 minute",
                                300 to "After 5 minutes",
                                -1 to "Never (until manually locked)",
                            ).map { (sec, label) ->
                                SettingChoiceOption(
                                    title = "$sec",
                                    subtitle = label,
                                    onSelect = {
                                        vaultViewModel.setVaultAutoLockTimeout(vault.id, sec)
                                    },
                                )
                            },
                        )
                    },
                )
            }
            item {
                PreferenceSwitch(
                    icon = Icons.Outlined.VisibilityOff,
                    title = "Hide in App Switcher (FLAG_SECURE)",
                    description = "Block system screenshots and recent apps switcher previews",
                    isChecked = vault.secureScreen,
                    onClick = {
                        vaultViewModel.setVaultSecureScreen(vault.id, !vault.secureScreen)
                    },
                )
            }

            // Section: Automation & Routing
            item {
                PreferenceSubtitle(text = "AUTOMATION & ROUTING")
            }
            item {
                PreferenceSwitch(
                    icon = Icons.Outlined.DeleteForever,
                    title = "Auto-delete source file",
                    description = "Delete public download when moving media into this vault",
                    isChecked = vault.autoDeleteOriginal,
                    onClick = {
                        vaultViewModel.setVaultAutoDeleteOriginal(vault.id, !vault.autoDeleteOriginal)
                    },
                )
            }
            item {
                PreferenceItem(
                    icon = Icons.Outlined.OpenInNew,
                    title = "Auto-move URL rules (${vault.autoMoveUrlRules.size})",
                    description = "Add website domains to automatically ingest into this vault",
                    onClick = { showAddRuleDialog = true },
                )
            }

            if (vault.autoMoveUrlRules.isNotEmpty()) {
                items(vault.autoMoveUrlRules) { rule ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 3.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = rule,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(
                                onClick = { vaultViewModel.deleteAutoMoveRule(vault.id, rule) },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Rule",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Section: Organization & Actions
            item {
                PreferenceSubtitle(text = "ORGANIZATION")
            }
            item {
                PreferenceItem(
                    icon = Icons.Outlined.Description,
                    title = "Rename Vault",
                    description = vault.name,
                    onClick = { showRenameDialog = true },
                )
            }
            item {
                PreferenceItem(
                    icon = Icons.Outlined.Sort,
                    title = "Media sort order",
                    description = when (vault.sortOrder) {
                        "oldest" -> "Oldest First"
                        "name" -> "Name (A–Z)"
                        "size" -> "File Size (Largest)"
                        else -> "Newest First"
                    },
                    onClick = {
                        choiceDialog = SettingChoiceDialogState(
                            title = "Media Sort Order",
                            selected = vault.sortOrder,
                            options = listOf(
                                "newest" to "Newest First",
                                "oldest" to "Oldest First",
                                "name" to "Name (A–Z)",
                                "size" to "File Size (Largest First)",
                            ).map { (key, label) ->
                                SettingChoiceOption(
                                    title = key,
                                    subtitle = label,
                                    onSelect = {
                                        vaultViewModel.setVaultSortOrder(vault.id, key)
                                    },
                                )
                            },
                        )
                    },
                )
            }
            if (vaultItems.isNotEmpty()) {
                item {
                    PreferenceItem(
                        icon = Icons.Outlined.DriveFileMove,
                        title = "Export all files to Downloads",
                        description = "Restore all ${vaultItems.size} files back to public storage",
                        onClick = { showExportConfirm = true },
                    )
                }
            }

            // Danger Zone
            item {
                PreferenceSubtitle(text = "DANGER ZONE", color = MaterialTheme.colorScheme.error)
            }
            item {
                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(imageVector = Icons.Outlined.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Vault & Stored Files", color = MaterialTheme.colorScheme.onError)
                }
            }
        }
    }
}

@Composable
private fun ChangePinDialog(
    vaultId: String,
    currentPinHash: String,
    onSuccess: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var step by remember { mutableIntStateOf(1) } // 1: Old PIN, 2: New PIN, 3: Confirm New PIN
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun hashPin(pin: String): String {
        return runCatching {
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
        }.getOrNull() ?: ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (step) {
                    1 -> "Enter Current PIN"
                    2 -> "Enter New PIN"
                    else -> "Confirm New PIN"
                },
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = when (step) {
                        1 -> "Verify your existing passcode."
                        2 -> "Enter a new 4–8 digit PIN."
                        else -> "Re-enter your new PIN to confirm."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                val activeLength = when (step) {
                    1 -> oldPin.length
                    2 -> newPin.length
                    else -> confirmPin.length
                }

                PinDotsIndicator(
                    pinLength = activeLength,
                    maxExpectedLength = 6,
                    isError = errorMsg != null,
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                PinKeypad(
                    onDigitClick = { digit ->
                        when (step) {
                            1 -> {
                                if (oldPin.length < 8) {
                                    val next = oldPin + digit
                                    oldPin = next
                                    errorMsg = null
                                    if (next.length >= 4 && hashPin(next) == currentPinHash) {
                                        step = 2
                                    }
                                }
                            }
                            2 -> {
                                if (newPin.length < 8) {
                                    newPin += digit
                                    errorMsg = null
                                }
                            }
                            3 -> {
                                if (confirmPin.length < newPin.length) {
                                    val next = confirmPin + digit
                                    confirmPin = next
                                    errorMsg = null
                                    if (next.length == newPin.length) {
                                        if (next == newPin) {
                                            onSuccess(newPin)
                                        } else {
                                            errorMsg = "PINs do not match"
                                            confirmPin = ""
                                        }
                                    }
                                }
                            }
                        }
                    },
                    onBackspaceClick = {
                        when (step) {
                            1 -> if (oldPin.isNotEmpty()) oldPin = oldPin.dropLast(1)
                            2 -> if (newPin.isNotEmpty()) newPin = newPin.dropLast(1)
                            3 -> if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                        }
                        errorMsg = null
                    },
                    onClearClick = {
                        when (step) {
                            1 -> oldPin = ""
                            2 -> newPin = ""
                            3 -> confirmPin = ""
                        }
                        errorMsg = null
                    },
                )

                if (step == 2 && newPin.length >= 4) {
                    Button(
                        onClick = { step = 3 },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Continue")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
