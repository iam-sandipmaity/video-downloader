package com.localdownloader.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

internal fun Context.canUseDeviceBiometrics(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val manager = getSystemService(BiometricManager::class.java) ?: return false
    val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK,
        )
    } else {
        @Suppress("DEPRECATION")
        manager.canAuthenticate()
    }
    return status == BiometricManager.BIOMETRIC_SUCCESS
}

internal fun Activity.promptDeviceBiometrics(
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        onError("Biometrics require Android 10 or newer")
        return
    }
    val executor = ContextCompat.getMainExecutor(this)
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
            onSuccess()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
            if (
                errorCode == BiometricPrompt.BIOMETRIC_ERROR_CANCELED ||
                errorCode == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED
            ) {
                return
            }
            onError(errString?.toString().orEmpty().ifBlank { "Biometric unlock failed" })
        }
    }
    val cancel = CancellationSignal()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        BiometricPrompt.Builder(this)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButton("Use PIN", executor) { _, _ -> }
            .build()
            .authenticate(cancel, executor, callback)
        return
    }
    onError("Biometrics require Android 10 or newer")
}

internal fun Context.findActivityOrNull(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
