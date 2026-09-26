package com.localdownloader.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class SingleVaultSettings(
    val id: String,
    val name: String,
    val pinHash: String,
    val isBiometricEnabled: Boolean = false,
    val autoMoveUrlRules: List<String> = emptyList(),
    val autoLockTimeoutSeconds: Int = 0, // 0 = Immediately on background, 30, 60, 300, -1 = Never
    val secureScreen: Boolean = true, // Hide app preview in recent apps switcher
    val autoDeleteOriginal: Boolean = true, // Remove original file from public storage after moving to vault
    val sortOrder: String = "newest", // newest, oldest, name, size
)

@Serializable
data class VaultSettings(
    val isEnabled: Boolean = false,
    val pinHash: String = "",
    val isBiometricEnabled: Boolean = false,
    val vaultName: String = "Private Vault",
    val vaults: List<SingleVaultSettings> = emptyList(),
)

fun VaultSettings.isSecure(): Boolean {
    return isEnabled && (pinHash.isNotEmpty() || vaults.isNotEmpty())
}

fun VaultSettings.getAllVaults(): List<SingleVaultSettings> {
    if (vaults.isNotEmpty()) return vaults
    if (isEnabled && pinHash.isNotEmpty()) {
        return listOf(
            SingleVaultSettings(
                id = "default",
                name = vaultName.ifBlank { "Private Vault" },
                pinHash = pinHash,
                isBiometricEnabled = isBiometricEnabled,
            )
        )
    }
    return emptyList()
}
