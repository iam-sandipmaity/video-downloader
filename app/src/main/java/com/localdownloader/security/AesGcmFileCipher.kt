package com.localdownloader.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AesGcmFileCipher @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val key: SecretKey by lazy { AndroidKeystoreAesKey.getOrCreate() }

    fun isEncrypted(file: File): Boolean = AesGcmCodec.isEncryptedFile(file)

    fun encryptBytes(plaintext: ByteArray): ByteArray = AesGcmCodec.seal(key, plaintext)

    fun decryptBytes(payload: ByteArray): ByteArray = AesGcmCodec.open(key, payload)

    fun encryptFile(source: File, target: File) {
        AesGcmCodec.encryptFile(key, source, target)
    }

    fun decryptFile(source: File, target: File) {
        AesGcmCodec.decryptFile(key, source, target)
    }

    fun encryptFileInPlace(file: File) {
        if (!file.exists() || isEncrypted(file)) return
        val tempFile = File(file.parentFile, "${file.name}.enc.tmp")
        try {
            encryptFile(file, tempFile)
            if (!tempFile.renameTo(file)) {
                tempFile.copyTo(file, overwrite = true)
                tempFile.delete()
            }
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    fun resolveForPlayback(path: String): String {
        val source = File(path)
        if (!source.exists() || !isEncrypted(source)) {
            return path
        }
        val target = playbackCacheFile(source)
        if (!target.exists() || target.length() == 0L) {
            val temp = File(target.parentFile, "${target.name}.tmp")
            try {
                decryptFile(source, temp)
                if (!temp.renameTo(target)) {
                    temp.copyTo(target, overwrite = true)
                    temp.delete()
                }
            } catch (error: Throwable) {
                temp.delete()
                target.delete()
                throw error
            }
        }
        return target.absolutePath
    }

    fun clearPlaybackCache() {
        playbackCacheDir().deleteRecursively()
    }

    private fun playbackCacheFile(source: File): File {
        val digest = Integer.toHexString(source.absolutePath.hashCode())
        return File(File(playbackCacheDir(), digest), source.name)
    }

    private fun playbackCacheDir(): File {
        return File(context.cacheDir, "vault-play")
    }
}
