package com.localdownloader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
    modifier: Modifier = Modifier,
) {
    val vaultState = vaultViewModel.uiState.value
    val downloadState = downloadViewModel.uiState.value

    // If vault is not enabled, show setup
    if (!vaultState.vaultSettings.isEnabled) {
        VaultSetupScreen(
            vaultViewModel = vaultViewModel,
            onBack = onBack,
        )
    return
    }

    // If vault is enabled but locked, show PIN entry
    if (!vaultState.isUnlocked) {
        VaultPinEntryDialog(
            onDismiss = onBack,
            onVerify = { pin ->
                vaultViewModel.verifyPin(pin) { success ->
                    // PIN verification handled by ViewModel
                }
            },
        )
    }

    val vaultItems = remember(downloadState.tasks) {
        downloadState.tasks.filter { task -> task.isInVault }
    }

    VaultContentScreen(
        vaultItems = vaultItems,
        vaultState = vaultState,
        downloadViewModel = downloadViewModel,
        onBack = onBack,
        onMoveToDownloads = onMoveToDownloads,
    )
}

@Composable
private fun VaultPinEntryDialog(
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.vault_unlock)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.vault_locked),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 16.dp),
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text(stringResource(R.string.vault_pin_hint)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onVerify(pin) },
                enabled = pin.length >= 4,
            ) {
                Text(stringResource(R.string.vault_unlock))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun VaultSetupScreen(
    vaultViewModel: VaultViewModel,
    onBack: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var vaultName by remember { mutableStateOf("Private Vault") }

    AlertDialog(
        onDismissRequest = onBack,
        title = { Text(stringResource(R.string.vault_pin_setup_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.vault_pin_setup_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 16.dp),
                )
                OutlinedTextField(
                    value = vaultName,
                    onValueChange = { vaultName = it },
                    label = { Text(stringResource(R.string.vault_vault_name)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text(stringResource(R.string.vault_pin_new)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it },
                    label = { Text(stringResource(R.string.vault_pin_confirm)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (pin.length >= 4 && pin == confirmPin) {
                        vaultViewModel.setupVault(pin, vaultName)
                    }
                },
                enabled = pin.length >= 4 && pin == confirmPin,
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

@Composable
private fun VaultContentScreen(
    vaultItems: List<DownloadTask>,
    vaultState: VaultUiState,
    downloadViewModel: DownloadViewModel,
    onBack: () -> Unit,
    onMoveToDownloads: () -> Unit,
) {
    val vaultName = vaultState.vaultSettings.vaultName.ifBlank { "Vault" }

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
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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