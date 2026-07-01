package com.localdownloader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.localdownloader.R
import com.localdownloader.domain.models.VaultSettings
import com.localdownloader.viewmodel.VaultUiState

@Composable
fun VaultScreen(
    uiState: VaultUiState,
    onUnlock: (String) -> Boolean,
    onSetPin: (String) -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onSetVaultName: (String) -> Unit,
    onDisableVault: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }
    var newPin by rememberSaveable { mutableStateOf("") }
    var vaultName by rememberSaveable { mutableStateOf(uiState.vaultSettings.vaultName) }

    val isVaultEnabled = uiState.vaultSettings.isEnabled
    val showSetup = !isVaultEnabled

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.vault_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        )

        if (showSetup) {
            SetupVaultContent(
                newPin = newPin,
                onNewPinChange = { newPin = it },
                confirmPin = confirmPin,
                onConfirmPinChange = { confirmPin = it },
                vaultName = vaultName,
                onVaultNameChange = { vaultName = it },
                onEnable = {
                    onSetPin(newPin)
                    onSetVaultName(vaultName)
                    onBack()
                },
                isEnabling = false,
            )
        } else {
            VaultMainContent(
                vaultSettings = uiState.vaultSettings,
                isUnlocked = uiState.isUnlocked,
                pin = pin,
                onPinChange = { pin = it },
                onUnlockRequested = {
                    val result = onUnlock(pin)
                    if (result) {
                        pin = ""
                    }
                },
                onSetBiometricEnabled = onSetBiometricEnabled,
                onSetVaultName = onSetVaultName,
                onDisableVault = onDisableVault,
            )
        }
    }
}

@Composable
private fun SetupVaultContent(
    newPin: String,
    onNewPinChange: (String) -> Unit,
    confirmPin: String,
    onConfirmPinChange: (String) -> Unit,
    vaultName: String,
    onVaultNameChange: (String) -> Unit,
    onEnable: () -> Unit,
    isEnabling: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.vault_setup_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.vault_setup_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = vaultName,
            onValueChange = onVaultNameChange,
            label = { Text(stringResource(R.string.vault_name_title)) },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = newPin,
            onValueChange = onNewPinChange,
            label = { Text(stringResource(R.string.vault_pin_title)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = confirmPin,
            onValueChange = onConfirmPinChange,
            label = { Text(stringResource(R.string.vault_confirm_pin_title)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onEnable,
            modifier = Modifier.fillMaxWidth(),
            enabled = newPin.length >= 4 && newPin == confirmPin && vaultName.isNotBlank() && !isEnabling,
        ) {
            Text(stringResource(R.string.common_set_up))
        }
    }
}

@Composable
private fun VaultMainContent(
    vaultSettings: VaultSettings,
    isUnlocked: Boolean,
    pin: String,
    onPinChange: (String) -> Unit,
    onUnlockRequested: () -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onSetVaultName: (String) -> Unit,
    onDisableVault: () -> Unit,
) {
    if (!isUnlocked) {
        VaultLockScreen(
            pin = pin,
            onPinChange = onPinChange,
            onUnlockRequested = onUnlockRequested,
        )
    } else {
        VaultUnlockScreen(
            vaultSettings = vaultSettings,
            onSetBiometricEnabled = onSetBiometricEnabled,
            onDisableVault = onDisableVault,
        )
    }
}

@Composable
private fun VaultLockScreen(
    pin: String,
    onPinChange: (String) -> Unit,
    onUnlockRequested: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = stringResource(R.string.vault_locked),
            style = MaterialTheme.typography.titleLarge,
        )

        Text(
            text = stringResource(R.string.vault_unlock),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )

        OutlinedTextField(
            value = pin,
            onValueChange = onPinChange,
            label = { Text(stringResource(R.string.vault_pin_title)) },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.width(200.dp),
        )

        TextButton(
            onClick = onUnlockRequested,
            enabled = pin.length >= 4,
        ) {
            Text(stringResource(R.string.common_unlock))
        }
    }
}

@Composable
private fun VaultUnlockScreen(
    vaultSettings: VaultSettings,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onDisableVault: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = vaultSettings.vaultName,
                style = MaterialTheme.typography.titleLarge,
            )
            Icon(
                imageVector = Icons.Outlined.PrivacyTip,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        Text(
            text = stringResource(R.string.vault_files_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Security Info",
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = "Files in the vault are stored in an encrypted location and require authentication to access.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onDisableVault,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.vault_disable_title))
        }
    }
}