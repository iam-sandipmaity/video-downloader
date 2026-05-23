package com.localdownloader.updates

import java.io.File
import java.security.MessageDigest

internal fun findExpectedSha256(checksumPayload: String, assetName: String): String? {
    return parseSha256Checksums(checksumPayload)[assetName]
}

internal fun parseSha256Checksums(checksumPayload: String): Map<String, String> {
    return checksumPayload.lineSequence()
        .mapNotNull { line ->
            val match = SHA256_CHECKSUM_LINE_REGEX.find(line.trim()) ?: return@mapNotNull null
            val digest = match.groupValues.getOrNull(1)?.lowercase().orEmpty()
            val name = match.groupValues.getOrNull(2)?.trim().orEmpty()
            if (digest.length == 64 && name.isNotBlank()) {
                name to digest
            } else {
                null
            }
        }
        .toMap()
}

internal fun sha256Hex(file: File): String {
    val messageDigest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read > 0) {
                messageDigest.update(buffer, 0, read)
            }
        }
    }
    return messageDigest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
}

private val SHA256_CHECKSUM_LINE_REGEX = Regex("^([A-Fa-f0-9]{64})\\s+\\*?(.+)$")
