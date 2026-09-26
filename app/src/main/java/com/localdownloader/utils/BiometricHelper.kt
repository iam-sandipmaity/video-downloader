package com.localdownloader.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.DialogInterface
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.hardware.fingerprint.FingerprintManagerCompat

object BiometricHelper {

    fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }

    fun isBiometricAvailable(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val biometricManager = context.getSystemService(android.hardware.biometrics.BiometricManager::class.java)
            val authenticators = android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG or
                android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK
            biometricManager?.canAuthenticate(authenticators) == android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val fingerprintManager = FingerprintManagerCompat.from(context)
            fingerprintManager.isHardwareDetected && fingerprintManager.hasEnrolledFingerprints()
        } else {
            false
        }
    }

    fun authenticate(
        activity: Activity,
        title: String,
        subtitle: String? = null,
        negativeButtonText: String = "Cancel",
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onCancel: () -> Unit = {},
    ): CancellationSignal? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cancellationSignal = CancellationSignal()
            val executor = ContextCompat.getMainExecutor(activity)

            val promptBuilder = BiometricPrompt.Builder(activity)
                .setTitle(title)
                .apply {
                    if (!subtitle.isNullOrBlank()) {
                        setSubtitle(subtitle)
                    }
                }
                .setNegativeButton(negativeButtonText, executor, DialogInterface.OnClickListener { _, _ ->
                    onCancel()
                })

            val prompt = promptBuilder.build()
            prompt.authenticate(
                cancellationSignal,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                        super.onAuthenticationError(errorCode, errString)
                        // 10 = BIOMETRIC_ERROR_USER_CANCELED, 13 = BIOMETRIC_ERROR_NEGATIVE_BUTTON, 5 = BIOMETRIC_ERROR_CANCELED
                        if (errorCode == 10 || errorCode == 13 || errorCode == 5) {
                            onCancel()
                        } else {
                            onError(errString?.toString() ?: "Biometric authentication failed")
                        }
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onError("Biometric not recognized. Please try again or use PIN.")
                    }
                },
            )
            return cancellationSignal
        } else {
            val cancellationSignalCompat = androidx.core.os.CancellationSignal()
            val fingerprintManager = FingerprintManagerCompat.from(activity)
            if (!fingerprintManager.isHardwareDetected || !fingerprintManager.hasEnrolledFingerprints()) {
                onError("Biometric authentication not available on this device")
                return null
            }
            fingerprintManager.authenticate(
                null,
                0,
                cancellationSignalCompat,
                object : FingerprintManagerCompat.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult?) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(errMsgId: Int, errString: CharSequence?) {
                        if (errMsgId == 5 /* FINGERPRINT_ERROR_CANCELED */) {
                            onCancel()
                        } else {
                            onError(errString?.toString() ?: "Fingerprint error")
                        }
                    }

                    override fun onAuthenticationFailed() {
                        onError("Fingerprint not recognized. Please try again or use PIN.")
                    }
                },
                null,
            )
            return null
        }
    }
}
