package com.localdownloader.downloader

import com.localdownloader.domain.models.LinkSourceKind

/**
 * Centralized source classification for URL-based discovery.
 */
object LinkSourceClassifier {
    fun classify(input: String): LinkSourceKind {
        val normalized = input.trim().lowercase()
        if (normalized.isBlank()) return LinkSourceKind.GENERIC_URL
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            return LinkSourceKind.GENERIC_URL
        }
        if (looksLikeYoutubeUrl(normalized)) {
            if (normalized.contains("list=") || normalized.contains("/playlist")) {
                return LinkSourceKind.PLAYLIST
            }
            if (
                normalized.contains("/channel/") ||
                normalized.contains("/c/") ||
                normalized.contains("/user/") ||
                normalized.contains("/@")
            ) {
                return LinkSourceKind.CHANNEL
            }
            return LinkSourceKind.SINGLE_VIDEO
        }
        return LinkSourceKind.GENERIC_URL
    }
}
