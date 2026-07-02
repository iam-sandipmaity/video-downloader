package com.localdownloader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localdownloader.domain.models.VaultSettings
import com.localdownloader.domain.repositories.DownloaderRepository
import com.localdownloader.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.localdownloader.domain.models.SingleVaultSettings
import com.localdownloader.domain.models.getAllVaults

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: DownloaderRepository,
    private val logger: Logger,
) : ViewModel() {
    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeSettings().collect { settings ->
                _uiState.value = _uiState.value.copy(
                    vaultSettings = settings.vaultSettings,
                )
            }
        }
    }

    fun createNewVault(name: String, pin: String) {
        viewModelScope.launch {
            runCatching {
                val newVaultId = java.util.UUID.randomUUID().toString()
                val pinHash = hashPin(pin)
                val newVault = SingleVaultSettings(
                    id = newVaultId,
                    name = name.ifBlank { "Private Vault" },
                    pinHash = pinHash,
                )
                val current = repository.getVaultSettings()
                val migratedVaults = current.getAllVaults()
                val updatedVaults = migratedVaults + newVault
                val updatedSettings = current.copy(
                    isEnabled = true,
                    vaults = updatedVaults,
                )
                repository.updateVaultSettings(updatedSettings).getOrThrow()
                
                _uiState.value = _uiState.value.copy(
                    vaultSettings = updatedSettings,
                    activeVaultId = newVaultId,
                    unlockingVaultId = null,
                    showSetup = false,
                )
            }.onFailure { error ->
                logger.e("VaultViewModel", "createNewVault failed", error)
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to create vault")
            }
        }
    }

    fun setupVault(pin: String, name: String) {
        createNewVault(name, pin)
    }

    fun selectVaultForUnlock(vaultId: String) {
        _uiState.value = _uiState.value.copy(
            unlockingVaultId = vaultId,
            showSetup = false,
            errorMessage = null,
        )
    }

    fun cancelUnlock() {
        _uiState.value = _uiState.value.copy(
            unlockingVaultId = null,
            showSetup = false,
            errorMessage = null,
        )
    }

    fun unlockVault(vaultId: String, pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val settings = repository.getVaultSettings()
            val vaultsList = settings.getAllVaults()
            val targetVault = vaultsList.firstOrNull { it.id == vaultId }
            val isValid = targetVault != null && hashPin(pin) == targetVault.pinHash
            if (isValid) {
                _uiState.value = _uiState.value.copy(
                    activeVaultId = vaultId,
                    unlockingVaultId = null,
                    showSetup = false,
                    errorMessage = null,
                )
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Invalid PIN")
            }
            onResult(isValid)
        }
    }

    fun lockActiveVault() {
        _uiState.value = _uiState.value.copy(
            activeVaultId = null,
            unlockingVaultId = null,
            showSetup = false,
            errorMessage = null,
        )
    }

    fun showSetupScreen(show: Boolean) {
        _uiState.value = _uiState.value.copy(
            showSetup = show,
            unlockingVaultId = null,
            errorMessage = null,
        )
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun updatePin(newPin: String) {
        // Kept for backward compatibility
        viewModelScope.launch {
            runCatching {
                val pinHash = hashPin(newPin)
                val currentSettings = repository.getVaultSettings()
                val updatedSettings = currentSettings.copy(
                    isEnabled = true,
                    pinHash = pinHash,
                )
                repository.updateVaultSettings(updatedSettings).getOrThrow()
                _uiState.value = _uiState.value.copy(
                    vaultSettings = updatedSettings,
                )
            }.onFailure { error ->
                logger.e("VaultViewModel", "updatePin failed", error)
            }
        }
    }

    fun verifyPin(pin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val settings = repository.getVaultSettings()
            val isValid = settings.isEnabled && hashPin(pin) == settings.pinHash
            if (isValid) {
                _uiState.value = _uiState.value.copy(
                    vaultSettings = settings,
                    activeVaultId = "default",
                )
            }
            onResult(isValid)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                val current = repository.getVaultSettings()
                val updated = current.copy(isBiometricEnabled = enabled)
                repository.updateVaultSettings(updated).getOrThrow()
            }.onFailure { error ->
                logger.e("VaultViewModel", "setBiometricEnabled failed", error)
            }
        }
    }

    fun setVaultName(name: String) {
        viewModelScope.launch {
            runCatching {
                val current = repository.getVaultSettings()
                val updated = current.copy(vaultName = name)
                repository.updateVaultSettings(updated).getOrThrow()
            }.onFailure { error ->
                logger.e("VaultViewModel", "setVaultName failed", error)
            }
        }
    }

    fun disableVault() {
        viewModelScope.launch {
            runCatching {
                val current = repository.getVaultSettings()
                val updated = current.copy(isEnabled = false, pinHash = "", vaults = emptyList())
                repository.updateVaultSettings(updated).getOrThrow()
                _uiState.value = _uiState.value.copy(activeVaultId = null, unlockingVaultId = null)
            }.onFailure { error ->
                logger.e("VaultViewModel", "disableVault failed", error)
            }
        }
    }

    private fun hashPin(pin: String): String {
        return runCatching {
            java.security.MessageDigest.getInstance("SHA-256")
                .digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
        }.getOrNull() ?: ""
    }
}

data class VaultUiState(
    val vaultSettings: VaultSettings = VaultSettings(),
    val isUnlocked: Boolean = false,
    val activeVaultId: String? = null,
    val unlockingVaultId: String? = null,
    val showSetup: Boolean = false,
    val unlockPin: String = "",
    val newPin: String = "",
    val confirmPin: String = "",
    val editName: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
)
