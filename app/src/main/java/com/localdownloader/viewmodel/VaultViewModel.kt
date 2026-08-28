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
import com.localdownloader.domain.models.DownloadTask
import com.localdownloader.security.PinHasher
import com.localdownloader.utils.FileUtils

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: DownloaderRepository,
    private val fileUtils: FileUtils,
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
                val pinHash = PinHasher.hash(pin)
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
            if (targetVault != null && PinHasher.verify(pin, targetVault.pinHash)) {
                if (PinHasher.needsUpgrade(targetVault.pinHash)) {
                    val upgradedVaults = vaultsList.map { vault ->
                        if (vault.id == vaultId) vault.copy(pinHash = PinHasher.hash(pin)) else vault
                    }
                    val updatedSettings = settings.copy(vaults = upgradedVaults)
                    runCatching { repository.updateVaultSettings(updatedSettings) }
                }
                runCatching { fileUtils.encryptVaultContentsIfNeeded(vaultId) }
                _uiState.value = _uiState.value.copy(
                    activeVaultId = vaultId,
                    unlockingVaultId = null,
                    showSetup = false,
                    errorMessage = null,
                )
                onResult(true)
            } else {
                _uiState.value = _uiState.value.copy(errorMessage = "Invalid PIN")
                onResult(false)
            }
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

    fun renameVault(vaultId: String, newName: String) {
        viewModelScope.launch {
            runCatching {
                val current = repository.getVaultSettings()
                val updatedVaults = current.vaults.map { vault ->
                    if (vault.id == vaultId) vault.copy(name = newName) else vault
                }
                val updatedSettings = current.copy(vaults = updatedVaults)
                repository.updateVaultSettings(updatedSettings).getOrThrow()
            }.onFailure { error ->
                logger.e("VaultViewModel", "renameVault failed", error)
            }
        }
    }

    fun deleteVault(vaultId: String, tasks: List<DownloadTask>) {
        viewModelScope.launch {
            runCatching {
                val vaultDir = fileUtils.ensureVaultDir(vaultId)
                vaultDir.deleteRecursively()

                tasks.filter { it.isInVault && it.outputPath?.contains("/vault/$vaultId/") == true }.forEach { task ->
                    repository.deleteDownloadedFile(task.id)
                }

                val current = repository.getVaultSettings()
                val updatedVaults = current.vaults.filter { it.id != vaultId }
                val updatedSettings = current.copy(
                    isEnabled = updatedVaults.isNotEmpty(),
                    vaults = updatedVaults
                )
                repository.updateVaultSettings(updatedSettings).getOrThrow()

                _uiState.value = _uiState.value.copy(
                    activeVaultId = null,
                    unlockingVaultId = null,
                )
            }.onFailure { error ->
                logger.e("VaultViewModel", "deleteVault failed", error)
            }
        }
    }

    fun addAutoMoveRule(vaultId: String, rule: String) {
        if (rule.isBlank()) return
        viewModelScope.launch {
            runCatching {
                val current = repository.getVaultSettings()
                val updatedVaults = current.vaults.map { vault ->
                    if (vault.id == vaultId) {
                        val newRules = (vault.autoMoveUrlRules + rule.trim()).distinct()
                        vault.copy(autoMoveUrlRules = newRules)
                    } else vault
                }
                val updatedSettings = current.copy(vaults = updatedVaults)
                repository.updateVaultSettings(updatedSettings).getOrThrow()
            }.onFailure { error ->
                logger.e("VaultViewModel", "addAutoMoveRule failed", error)
            }
        }
    }

    fun deleteAutoMoveRule(vaultId: String, rule: String) {
        viewModelScope.launch {
            runCatching {
                val current = repository.getVaultSettings()
                val updatedVaults = current.vaults.map { vault ->
                    if (vault.id == vaultId) {
                        val newRules = vault.autoMoveUrlRules.filter { it != rule }
                        vault.copy(autoMoveUrlRules = newRules)
                    } else vault
                }
                val updatedSettings = current.copy(vaults = updatedVaults)
                repository.updateVaultSettings(updatedSettings).getOrThrow()
            }.onFailure { error ->
                logger.e("VaultViewModel", "deleteAutoMoveRule failed", error)
            }
        }
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
                val pinHash = PinHasher.hash(newPin)
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
            val isValid = settings.isEnabled && PinHasher.verify(pin, settings.pinHash)
            if (isValid) {
                if (PinHasher.needsUpgrade(settings.pinHash)) {
                    val updatedSettings = settings.copy(pinHash = PinHasher.hash(pin))
                    runCatching { repository.updateVaultSettings(updatedSettings) }
                }
                runCatching { fileUtils.encryptVaultContentsIfNeeded("default") }
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
