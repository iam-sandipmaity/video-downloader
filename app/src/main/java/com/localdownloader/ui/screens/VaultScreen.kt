package com.localdownloader.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.localdownloader.R
import com.localdownloader.domain.models.DownloadTask
import com.localdownloader.domain.models.SingleVaultSettings
import com.localdownloader.domain.models.getAllVaults
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.viewmodel.DownloadViewModel
import com.localdownloader.viewmodel.VaultUiState
import com.localdownloader.viewmodel.VaultViewModel

@Composable
fun VaultScreen(
    vaultViewModel: VaultViewModel,
    downloadViewModel: DownloadViewModel,
    onBack: () -> Unit,
    onMoveToDownloads: () -> Unit,
    onPlayItem: (String) -> Unit,
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
                }
            )
        }

        vaultState.unlockingVaultId != null -> {
            val targetVaultId = vaultState.unlockingVaultId!!
            val targetVaultName = allVaults.firstOrNull { it.id == targetVaultId }?.name ?: "Vault"
            VaultUnlockScreen(
                vaultName = targetVaultName,
                errorMessage = vaultState.errorMessage,
                onCancel = { vaultViewModel.cancelUnlock() },
                onUnlock = { pin ->
                    vaultViewModel.unlockVault(targetVaultId, pin) { success ->
                        // VM updates state
                    }
                }
            )
        }

        vaultState.activeVaultId != null -> {
            val activeId = vaultState.activeVaultId!!
            val activeVault = allVaults.firstOrNull { it.id == activeId }
            val activeVaultName = activeVault?.name ?: "Vault"

            val vaultItems = remember(downloadState.tasks, activeId) {
                downloadState.tasks.filter { task ->
                    isTaskInVault(task, activeId, allVaults.map { it.id })
                }
            }

            VaultContentScreen(
                vaultItems = vaultItems,
                vaultName = activeVaultName,
                downloadViewModel = downloadViewModel,
                onBack = { vaultViewModel.lockActiveVault() },
                onMoveToDownloads = onMoveToDownloads,
                onPlayItem = onPlayItem,
            )
        }

        else -> {
            VaultSelectorScreen(
                vaults = allVaults,
                onSelectVault = { vaultId -> vaultViewModel.selectVaultForUnlock(vaultId) },
                onCreateNewClick = { vaultViewModel.showSetupScreen(true) },
                onBack = onBack
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

    PreferencePageScaffold(
        title = stringResource(R.string.vault_pin_setup_title),
        onBack = onBack,
        content = {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = vaultName,
                        onValueChange = { vaultName = it },
                        label = { Text(stringResource(R.string.vault_vault_name)) },
                        placeholder = { Text("e.g. Work, Personal") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 8) pin = it },
                        label = { Text(stringResource(R.string.vault_pin_new)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 8) confirmPin = it },
                        label = { Text(stringResource(R.string.vault_pin_confirm)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            if (pin.length >= 4 && pin == confirmPin) {
                                vaultViewModel.createNewVault(vaultName, pin)
                            }
                        },
                        enabled = pin.length >= 4 && pin == confirmPin,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text(stringResource(R.string.common_save))
                    }
                }
            }
        }
    )
}

@Composable
private fun VaultUnlockScreen(
    vaultName: String,
    errorMessage: String?,
    onCancel: () -> Unit,
    onUnlock: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }

    PreferencePageScaffold(
        title = "Unlock $vaultName",
        onBack = onCancel,
        content = {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Enter PIN to access $vaultName.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.all { char -> char.isDigit() } && it.length <= 8) pin = it },
                        label = { Text("Vault PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = { onUnlock(pin) },
                        enabled = pin.length >= 4,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Unlock")
                    }

                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                }
            }
        }
    )
}

@Composable
private fun VaultSelectorScreen(
    vaults: List<SingleVaultSettings>,
    onSelectVault: (String) -> Unit,
    onCreateNewClick: () -> Unit,
    onBack: () -> Unit
) {
    PreferencePageScaffold(
        title = "Private Vaults",
        onBack = onBack,
        content = {
            if (vaults.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "No Vaults Created",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Button(onClick = onCreateNewClick) {
                            Text("Create First Vault")
                        }
                    }
                }
            } else {
                items(vaults) { vault ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable { onSelectVault(vault.id) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = vault.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(onClick = onCreateNewClick) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Create New Vault")
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun VaultContentScreen(
    vaultItems: List<DownloadTask>,
    vaultName: String,
    downloadViewModel: DownloadViewModel,
    onBack: () -> Unit,
    onMoveToDownloads: () -> Unit,
    onPlayItem: (String) -> Unit,
) {
    PreferencePageScaffold(
        title = vaultName,
        onBack = onBack,
        content = {
            if (vaultItems.isEmpty()) {
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
                            text = stringResource(R.string.vault_empty),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.vault_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                    }
                }
            } else {
                items(vaultItems) { item ->
                    VaultItemCard(
                        item = item,
                        onMoveToDownloads = { taskId ->
                            downloadViewModel.moveFromVault(taskId)
                        },
                        onPlayClick = { onPlayItem(item.id) },
                    )
                }
            }
        }
    )
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