package com.localdownloader.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
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
            val targetVaultName = targetVault?.name
                ?: stringResource(R.string.vault_title)
            BackHandler { vaultViewModel.cancelUnlock() }
            VaultUnlockScreen(
                vaultName = targetVaultName,
                errorMessage = vaultState.errorMessage,
                biometricEnabled = targetVault?.isBiometricEnabled == true ||
                    vaultState.vaultSettings.isBiometricEnabled,
                onCancel = { vaultViewModel.cancelUnlock() },
                onUnlock = { pin ->
                    vaultViewModel.unlockVault(targetVaultId, pin) { _ -> }
                },
                onBiometricUnlock = {
                    vaultViewModel.unlockVaultWithBiometric(targetVaultId)
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
    onBack: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var vaultName by remember { mutableStateOf("") }
    val pinsMatch = pin.isNotEmpty() && confirmPin.isNotEmpty() && pin == confirmPin
    val pinValid = pin.length in 4..8
    val canSave = pinValid && pinsMatch

    PreferencePageScaffold(
        title = stringResource(R.string.vault_pin_setup_title),
        onBack = onBack,
        content = {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .align(Alignment.CenterHorizontally),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.vault_pin_setup_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    OutlinedTextField(
                        value = vaultName,
                        onValueChange = { vaultName = it },
                        label = { Text(stringResource(R.string.vault_vault_name)) },
                        placeholder = { Text(stringResource(R.string.vault_name_optional_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 8) pin = it },
                        label = { Text(stringResource(R.string.vault_pin_new)) },
                        placeholder = { Text(stringResource(R.string.vault_pin_hint)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 8) confirmPin = it },
                        label = { Text(stringResource(R.string.vault_pin_confirm)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = confirmPin.isNotEmpty() && !pinsMatch,
                        supportingText = {
                            when {
                                confirmPin.isNotEmpty() && !pinsMatch -> {
                                    Text(stringResource(R.string.vault_pin_mismatch))
                                }
                                pin.isNotEmpty() && !pinValid -> {
                                    Text(stringResource(R.string.vault_pin_too_short))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            if (canSave) {
                                vaultViewModel.createNewVault(vaultName, pin)
                            }
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    ) {
                        Text(stringResource(R.string.common_save))
                    }
                }
            }
        },
    )
}

@Composable
private fun VaultUnlockScreen(
    vaultName: String,
    errorMessage: String?,
    biometricEnabled: Boolean,
    onCancel: () -> Unit,
    onUnlock: (String) -> Unit,
    onBiometricUnlock: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivityOrNull() }
    val biometricAvailable = remember(context) { context.canUseDeviceBiometrics() }
    var pin by remember { mutableStateOf("") }
    var biometricError by remember { mutableStateOf<String?>(null) }

    fun launchBiometricPrompt() {
        val host = activity ?: return
        host.promptDeviceBiometrics(
            title = "Unlock $vaultName",
            subtitle = "Confirm it's you to open this private vault",
            onSuccess = onBiometricUnlock,
            onError = { message -> biometricError = message },
        )
    }

    LaunchedEffect(biometricEnabled, biometricAvailable) {
        if (biometricEnabled && biometricAvailable) {
            launchBiometricPrompt()
        }
    }

    PreferencePageScaffold(
        title = stringResource(R.string.vault_unlock_title, vaultName),
        onBack = onCancel,
        content = {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )

                    Text(
                        text = stringResource(R.string.vault_unlock_body, vaultName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 8) pin = it },
                        label = { Text(stringResource(R.string.vault_pin_prompt)) },
                        placeholder = { Text(stringResource(R.string.vault_pin_hint)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (errorMessage != null || biometricError != null) {
                        Text(
                            text = errorMessage ?: biometricError.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    Button(
                        onClick = { onUnlock(pin) },
                        enabled = pin.length >= 4,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.vault_unlock))
                    }

                    if (biometricEnabled && biometricAvailable) {
                        TextButton(
                            onClick = { launchBiometricPrompt() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Use fingerprint or face unlock")
                        }
                    }

                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            }
        },
    )
}

@Composable
private fun VaultSelectorScreen(
    vaults: List<SingleVaultSettings>,
    onSelectVault: (String) -> Unit,
    onCreateNewClick: () -> Unit,
    onBack: () -> Unit,
) {
    PreferencePageScaffold(
        title = stringResource(R.string.vault_selector_title),
        onBack = onBack,
        content = {
            if (vaults.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.vault_no_vaults),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Button(onClick = onCreateNewClick) {
                            Text(stringResource(R.string.vault_create_first))
                        }
                    }
                }
            } else {
                items(vaults, key = { it.id }) { vault ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable { onSelectVault(vault.id) },
                        shape = RoundedCornerShape(12.dp),
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = vault.name,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            )
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Button(onClick = onCreateNewClick) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(stringResource(R.string.vault_create_new))
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun VaultContentScreen(
    vaultItems: List<DownloadTask>,
    vaultName: String,
    activeId: String,
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
    var selectedTab by remember { mutableStateOf(0) }
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
                    contentDescription = stringResource(R.string.common_settings),
                )
            }
        },
        content = {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.vault_search_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                ) {
                    val tabs = listOf(
                        stringResource(R.string.vault_tab_all),
                        stringResource(R.string.vault_tab_videos),
                        stringResource(R.string.vault_tab_audios),
                        stringResource(R.string.vault_tab_others),
                    )
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) },
                        )
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                stringResource(R.string.vault_no_search_results)
                            } else {
                                stringResource(R.string.vault_empty)
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                stringResource(R.string.vault_no_search_results_body)
                            } else {
                                stringResource(R.string.vault_empty_body)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
            } else {
                items(filteredItems, key = { it.id }) { item ->
                    val path = item.outputPath.orEmpty()
                    val isAudio = isLikelyAudioPath(path)
                    val isVideo = isLikelyVideoPath(path)
                    VaultItemCard(
                        item = item,
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
        },
    )

    if (showSettings) {
        var newVaultName by remember { mutableStateOf(vaultName) }
        var showDeleteConfirm by remember { mutableStateOf(false) }
        var newRule by remember { mutableStateOf("") }
        val activeVaultSettings = vaultState.vaultSettings.getAllVaults().firstOrNull { it.id == activeId }
        val rules = activeVaultSettings?.autoMoveUrlRules ?: emptyList()

        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("$vaultName Settings") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Rename Vault", style = MaterialTheme.typography.titleSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = newVaultName,
                                    onValueChange = { newVaultName = it },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                )
                                Button(
                                    onClick = {
                                        if (newVaultName.isNotBlank()) {
                                            vaultViewModel.renameVault(activeId, newVaultName)
                                        }
                                    },
                                    enabled = newVaultName.isNotBlank() && newVaultName != vaultName,
                                ) {
                                    Text("Rename")
                                }
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Unlock with biometrics", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Use fingerprint or face unlock as a shortcut. Your PIN remains the fallback.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Biometric unlock")
                                Switch(
                                    checked = activeVaultSettings?.isBiometricEnabled == true,
                                    onCheckedChange = { enabled ->
                                        vaultViewModel.setVaultBiometricEnabled(activeId, enabled)
                                    },
                                )
                            }
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Auto-Move URL Rules", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Downloads matching these prefixes will automatically move here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
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
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(
                                        onClick = { vaultViewModel.deleteAutoMoveRule(activeId, rule) },
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.common_delete),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 16.dp),
                        ) {
                            Text(
                                "Danger Zone",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Button(
                                onClick = { showDeleteConfirm = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth(),
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
                    Text(stringResource(R.string.common_close))
                }
            },
        )

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete $vaultName?") },
                text = {
                    Text(
                        "Are you sure? This will permanently delete the vault and delete all ${vaultItems.size} files stored inside it. This action is irreversible.",
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
                        Text(stringResource(R.string.common_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun VaultItemCard(
    item: DownloadTask,
    onMoveToDownloads: (String) -> Unit,
    onPlayClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onPlayClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                )
                Text(
                    text = item.outputPath?.substringAfterLast('/')?.substringAfterLast('.')?.let { ".$it" } ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            TextButton(
                onClick = { onMoveToDownloads(item.id) },
                modifier = Modifier.height(36.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoveToInbox,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(stringResource(R.string.vault_move_to_downloads))
                }
            }
        }
    }
}
