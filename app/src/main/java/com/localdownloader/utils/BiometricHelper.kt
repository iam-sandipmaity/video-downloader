package com.localdownloader.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.DialogInterface
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.core.content.ContextCompat
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object BiometricHelper {

    private const val KEYSTORE_NAME = "AndroidKeyStore"
    private const val KEY_ALIAS = "com.localdownloader.vault.biometric_key"
    private const val CIPHER_TRANSFORMATION = "AES/CBC/PKCS7Padding"
    private val AUTH_CHALLENGE = "localdownloader_vault_auth_token".toByteArray(Charsets.UTF_8)

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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val biometricManager = context.getSystemService(BiometricManager::class.java)
            biometricManager?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val biometricManager = context.getSystemService(BiometricManager::class.java)
            @Suppress("DEPRECATION")
            biometricManager?.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val fingerprintManager = FingerprintManagerCompat.from(context)
            fingerprintManager.isHardwareDetected && fingerprintManager.hasEnrolledFingerprints()
        } else {
            false
        }
    }

    @Synchronized
    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_NAME).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateSecretKey()
        }
        return (keyStore.getKey(KEY_ALIAS, null) as? SecretKey) ?: run {
            keyStore.deleteEntry(KEY_ALIAS)
            generateSecretKey()
            keyStore.getKey(KEY_ALIAS, null) as SecretKey
        }
    }

    private fun generateSecretKey() {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_NAME)
        val builder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setUserAuthenticationRequired(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
        }

        keyGenerator.init(builder.build())
        keyGenerator.generateKey()
    }

    @Synchronized
    fun getInitializedCipher(): Cipher? {
        return runCatching {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            try {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                cipher
            } catch (e: KeyPermanentlyInvalidatedException) {
                // Key was invalidated due to new biometric enrollment. Re-create key and re-init cipher.
                val keyStore = KeyStore.getInstance(KEYSTORE_NAME).apply { load(null) }
                keyStore.deleteEntry(KEY_ALIAS)
                val newKey = getOrCreateSecretKey()
                cipher.init(Cipher.ENCRYPT_MODE, newKey)
                cipher
            }
        }.getOrNull()
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
        val cipher = getInitializedCipher()
        if (cipher == null) {
            onError("Cryptographic biometric key could not be initialized")
            return null
        }

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
            val cryptoObject = BiometricPrompt.CryptoObject(cipher)

            prompt.authenticate(
                cryptoObject,
                cancellationSignal,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                        super.onAuthenticationSucceeded(result)
                        val authenticatedCipher = result?.cryptoObject?.cipher
                        val verified = runCatching {
                            authenticatedCipher?.doFinal(AUTH_CHALLENGE) != null
                        }.getOrDefault(false)

                        if (verified) {
                            onSuccess()
                        } else {
                            onError("Biometric cryptographic verification failed")
                        }
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
            val cryptoObjectCompat = FingerprintManagerCompat.CryptoObject(cipher)
            fingerprintManager.authenticate(
                cryptoObjectCompat,
                0,
                cancellationSignalCompat,
                object : FingerprintManagerCompat.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: FingerprintManagerCompat.AuthenticationResult) {
                        val authenticatedCipher = result.cryptoObject.cipher
                        val verified = runCatching {
                            authenticatedCipher?.doFinal(AUTH_CHALLENGE) != null
                        }.getOrDefault(false)

                        if (verified) {
                            onSuccess()
                        } else {
                            onError("Fingerprint cryptographic verification failed")
                        }
                    }

                    override fun onAuthenticationError(errMsgId: Int, errString: CharSequence) {
                        if (errMsgId == 5 /* FINGERPRINT_ERROR_CANCELED */) {
                            onCancel()
                        } else {
                            onError(errString.toString())
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
