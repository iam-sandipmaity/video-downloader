package com.localdownloader.updates

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkSignatureVerifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun verifyMatchesInstalledSigner(apkFile: File) {
        val trustedDigests = installedSignerDigests()
        require(trustedDigests.isNotEmpty()) {
            "Unable to determine the installed application signer"
        }
        verifySignerDigest(apkFile, trustedDigests)
    }

    fun verifySignerDigest(apkFile: File, trustedDigests: Set<String>) {
        val actualDigests = archiveSignerDigests(apkFile)
        require(actualDigests.isNotEmpty()) {
            "Unable to read APK signing certificates for ${apkFile.name}"
        }
        val normalizedTrusted = trustedDigests.map(::normalizeDigest).toSet()
        check(actualDigests.any { it in normalizedTrusted }) {
            "APK signer verification failed for ${apkFile.name}"
        }
    }

    private fun installedSignerDigests(): Set<String> {
        val packageInfo = packageInfoForInstalledApp()
        return signerDigests(packageInfo)
    }

    private fun archiveSignerDigests(apkFile: File): Set<String> {
        val packageInfo = packageInfoForArchive(apkFile)
            ?: throw IllegalStateException("Unable to inspect APK archive ${apkFile.name}")
        return signerDigests(packageInfo)
    }

    @Suppress("DEPRECATION")
    private fun packageInfoForInstalledApp(): PackageInfo {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        return context.packageManager.getPackageInfo(context.packageName, flags)
    }

    @Suppress("DEPRECATION")
    private fun packageInfoForArchive(apkFile: File): PackageInfo? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        return context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags)
    }

    @Suppress("DEPRECATION")
    private fun signerDigests(packageInfo: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo ?: return emptySet()
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            packageInfo.signatures ?: emptyArray()
        }
        return signatures
            .map(Signature::toByteArray)
            .map(::sha256Hex)
            .map(::normalizeDigest)
            .toSet()
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    private fun normalizeDigest(raw: String): String {
        return raw.lowercase().replace(":", "").trim()
    }
}
