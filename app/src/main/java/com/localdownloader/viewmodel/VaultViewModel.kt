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

    fun updatePin(newPin: String) {
        viewModelScope.launch {
            runCatching {
                val currentSettings = repository.getVaultSettings()
                repository.updateSettings(
                    repository.observeSettings().first().copy(
                        vaultSettings = currentSettings.copy(
                            isEnabled = true,
                            pinHash = hashPin(newPin),
                        )
                    )
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
                _uiState.value = _uiState.value.copy(isUnlocked = true)
            }
            onResult(isValid)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getVaultSettings()
            repository.updateSettings(
                repository.observeSettings().first().copy(
                    vaultSettings = current.copy(isBiometricEnabled = enabled)
                )
            )
        }
    }

    fun setVaultName(name: String) {
        viewModelScope.launch {
            val current = repository.getVaultSettings()
            repository.updateSettings(
                repository.observeSettings().first().copy(
                    vaultSettings = current.copy(vaultName = name)
                )
            )
        }
    }

    fun disableVault() {
        viewModelScope.launch {
            val current = repository.getVaultSettings()
            repository.updateSettings(
                repository.observeSettings().first().copy(
                    vaultSettings = current.copy(isEnabled = false, pinHash = "")
                )
            )
            _uiState.value = _uiState.value.copy(isUnlocked = false)
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
    val unlockPin: String = "",
    val newPin: String = "",
    val confirmPin: String = "",
    val editName: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
)
