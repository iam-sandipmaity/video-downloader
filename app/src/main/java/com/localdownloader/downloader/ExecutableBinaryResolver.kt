package com.localdownloader.downloader

import java.io.File

internal object ExecutableBinaryResolver {
    fun resolveFirstExisting(directory: File, candidates: List<String>): File? {
        return candidates.firstNotNullOfOrNull { candidate ->
            File(directory, candidate).takeIf { it.exists() && it.isFile }
        }
    }
}
