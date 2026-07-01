package com.localdownloader.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class VaultSettings(
    val isEnabled: Boolean = false,
    val pinHash: String = "",
    val isBiometricEnabled: Boolean = false,
    val vaultName: String = "Private Vault",
)

fun VaultSettings.isSecure(): Boolean {
    return isEnabled && pinHash.isNotEmpty()
}
