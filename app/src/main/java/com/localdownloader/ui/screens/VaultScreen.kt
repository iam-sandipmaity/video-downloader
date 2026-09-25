package com.localdownloader.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.localdownloader.ui.components.PreferencePageScaffold
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
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header Shield
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
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
                            1 -> "Choose a secure PIN to encrypt and protect your private media files."
                            2 -> "Re-enter the same PIN to make sure you remember it."
                            else -> "Give your vault a distinctive name (e.g. Work, Secret, Personal)."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                if (step <= 2) {
                    val currentInput = if (step == 1) initialPin else confirmPin

                    // Animated PIN Dots
                    PinDotsIndicator(
                        pinLength = currentInput.length,
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

                    // Keypad
                    PinKeypad(
                        onDigitClick = { digit ->
                            errorMessage = null
                            if (step == 1) {
                                if (initialPin.length < 8) initialPin += digit
                            } else {
                                if (confirmPin.length < 8) confirmPin += digit
                            }
                        },
                        onBackspaceClick = {
                            errorMessage = null
                            if (step == 1) {
                                if (initialPin.isNotEmpty()) initialPin = initialPin.dropLast(1)
                            } else {
                                if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                            }
                        },
                        onClearClick = {
                            errorMessage = null
                            if (step == 1) initialPin = "" else confirmPin = ""
                        },
                    )

                    // Step Action Button
                    Button(
                        onClick = {
                            if (step == 1) {
                                if (initialPin.length < 4) {
                                    triggerShake("PIN must be at least 4 digits")
                                } else {
                                    step = 2
                                    errorMessage = null
                                }
                            } else if (step == 2) {
                                if (confirmPin != initialPin) {
                                    triggerShake("PINs do not match. Try again.")
                                    confirmPin = ""
                                } else {
                                    step = 3
                                    errorMessage = null
                                }
                            }
                        },
                        enabled = (step == 1 && initialPin.length >= 4) || (step == 2 && confirmPin.length >= 4),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(if (step == 1) "Continue" else "Confirm PIN")
                    }
                } else {
                    // Step 3: Vault Name & Finalize
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            OutlinedTextField(
                                value = vaultName,
                                onValueChange = { vaultName = it },
                                label = { Text("Vault Name") },
                                placeholder = { Text("e.g. Private Vault") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Button(
                                onClick = {
                                    vaultViewModel.createNewVault(vaultName, initialPin)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(imageVector = Icons.Outlined.Check, contentDescription = null)
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
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // Header Lock Icon
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
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
        modifier = Modifier.padding(vertical = 10.dp),
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
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        // Rows 1-3 (Digits 1 to 9)
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
            // Left Action
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

            // Zero
            KeypadNumberButton(
                digit = "0",
                subtext = "+",
                onClick = { onDigitClick("0") },
                modifier = Modifier.weight(1f),
            )

            // Backspace
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
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
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
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
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
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Hardware-Isolated Storage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Vault media stays hidden from gallery and system apps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        items(vaults, key = { it.id }) { vault ->
            val itemCount = remember(tasks, vault.id) {
                tasks.count { isTaskInVault(it, vault.id, vaults.map { v -> v.id }) }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelectVault(vault.id) },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
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
                            color = MaterialTheme.colorScheme.primaryContainer,
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
                            Text(
                                text = "$itemCount items · Encrypted",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onCreateNewClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Another Vault")
            }
        }
    }
}

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
    var showSettings by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val notPlayableMessage = stringResource(R.string.vault_file_not_playable)

    val filteredItems = remember(vaultItems, selectedTab, searchQuery) {
        vaultItems.filter { item ->
            val matchesSearch = item.title.contains(searchQuery, ignoreCase = true)
            val path = item.outputPath.orEmpty()
            val isVideo = isLikelyVideoPath(path)
            val isAudio = isLikelyAudioPath(path)
            val matchesTab = when (selectedTab) {
                0 -> true
                1 -> isVideo
                2 -> isAudio
                3 -> !isVideo && !isAudio
                else -> true
            }
            matchesSearch && matchesTab
        }
    }

    PreferencePageScaffold(
        title = vaultName,
        onBack = onBack,
        modifier = modifier,
        actions = {
            IconButton(onClick = { showSettings = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Vault Settings",
                )
            }
        },
    ) {
        // Search Bar
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
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            )
        }

        // Tabs Row
        item {
            val videoCount = vaultItems.count { isLikelyVideoPath(it.outputPath.orEmpty()) }
            val audioCount = vaultItems.count { isLikelyAudioPath(it.outputPath.orEmpty()) }
            val otherCount = vaultItems.size - videoCount - audioCount

            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.Transparent,
            ) {
                val tabs = listOf(
                    "All (${vaultItems.size})",
                    "Videos ($videoCount)",
                    "Audios ($audioCount)",
                    "Other ($otherCount)",
                )
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    )
                }
            }
        }

        if (filteredItems.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching files" else "Vault is Empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                "Try searching with a different name or keyword."
                            } else {
                                "Move completed downloads into this vault from the Downloads tab to keep them secret and encrypted."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        } else {
            items(filteredItems, key = { it.id }) { item ->
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

    if (showSettings) {
        var newVaultName by remember { mutableStateOf(vaultName) }
        var showDeleteConfirm by remember { mutableStateOf(false) }
        var newRule by remember { mutableStateOf("") }
        val rules = activeVaultSettings?.autoMoveUrlRules ?: emptyList()
        val isBiometric = activeVaultSettings?.isBiometricEnabled ?: false

        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("$vaultName Settings") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    // Rename Vault
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Rename Vault", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = newVaultName,
                                    onValueChange = { newVaultName = it },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f),
                                )
                                Button(
                                    onClick = {
                                        if (newVaultName.isNotBlank()) {
                                            vaultViewModel.renameVault(activeId, newVaultName)
                                        }
                                    },
                                    enabled = newVaultName.isNotBlank() && newVaultName != vaultName,
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text("Save")
                                }
                            }
                        }
                    }

                    // Biometric Unlock Switch
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text("Biometric Unlock", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Unlock using fingerprint or face verification",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = isBiometric,
                                onCheckedChange = { enabled ->
                                    vaultViewModel.setVaultBiometricEnabled(activeId, enabled)
                                },
                            )
                        }
                    }

                    // Auto Move URL Rules
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Auto-Move URL Rules", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Downloads matching these URL domain prefixes will automatically move directly into this vault.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = newRule,
                                    onValueChange = { newRule = it },
                                    placeholder = { Text("https://example.com") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f),
                                )
                                Button(
                                    onClick = {
                                        if (newRule.isNotBlank()) {
                                            vaultViewModel.addAutoMoveRule(activeId, newRule)
                                            newRule = ""
                                        }
                                    },
                                    enabled = newRule.isNotBlank(),
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text("Add")
                                }
                            }
                        }
                    }

                    if (rules.isNotEmpty()) {
                        items(rules) { rule ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                        onClick = { vaultViewModel.deleteAutoMoveRule(activeId, rule) },
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

                    // Danger Zone
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            Text(
                                "Danger Zone",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                            )
                            Button(
                                onClick = { showDeleteConfirm = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("Delete Vault & Stored Files", color = MaterialTheme.colorScheme.onError)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("Close")
                }
            },
        )

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete $vaultName?") },
                text = {
                    Text(
                        "Are you sure? This will permanently delete the vault and all ${vaultItems.size} files stored inside it from your device. This action cannot be reversed.",
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirm = false
                            showSettings = false
                            vaultViewModel.deleteVault(activeId, downloadState.tasks)
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
        modifier = Modifier
            .fillMaxWidth()
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
