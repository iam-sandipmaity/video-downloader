package com.localdownloader.domain.models

/**
 * High-level source classification for discovery-first link analysis.
 */
enum class LinkSourceKind {
    SINGLE_VIDEO,
    PLAYLIST,
    CHANNEL,
    GENERIC_URL,
}
