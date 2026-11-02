package com.localdownloader.viewmodel

import com.localdownloader.domain.models.VaultSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class VaultUiStateTest {

    @Test
    fun defaultUiState_hasCorrectInitialValues() {
        val state = VaultUiState()
        assertEquals(VaultSettings(), state.vaultSettings)
        assertFalse(state.isUnlocked)
        assertNull(state.activeVaultId)
        assertNull(state.unlockingVaultId)
        assertFalse(state.showSetup)
        assertEquals("", state.unlockPin)
        assertEquals("", state.newPin)
        assertEquals("", state.confirmPin)
        assertEquals("", state.editName)
        assertNull(state.errorMessage)
        assertNull(state.successMessage)
        assertEquals(0, state.lockoutRemainingSeconds)
    }

    @Test
    fun lockoutState_reflectsRemainingSecondsAndErrorMessage() {
        val lockedOutState = VaultUiState(
            unlockingVaultId = "default",
            lockoutRemainingSeconds = 30,
            errorMessage = "Too many failed attempts. Try again in 30s.",
        )
        assertEquals(30, lockedOutState.lockoutRemainingSeconds)
        assertEquals("Too many failed attempts. Try again in 30s.", lockedOutState.errorMessage)
        assertEquals("default", lockedOutState.unlockingVaultId)
    }

    @Test
    fun constants_haveExpectedSecurityValues() {
        assertEquals(5, VaultViewModel.MAX_FAILED_ATTEMPTS)
        assertEquals(30, VaultViewModel.LOCKOUT_DURATION_SECONDS)
    }
}
