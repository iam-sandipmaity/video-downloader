package com.localdownloader.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class SingleVaultSettings(
    val id: String,
    val name: String,
    val pinHash: String,
    val isBiometricEnabled: Boolean = false,
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
