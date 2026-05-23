package com.localdownloader.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class YoutubeAuthConfig(
    val enabled: Boolean = false,
    val clientHint: String = "web.gvs",
    val gvsToken: String = "",
    val playerToken: String = "",
    val subsToken: String = "",
    val visitorData: String = "",
    val dataSyncId: String = "",
    val updatedAtEpochMs: Long = 0L,
) {
    fun isConfigured(): Boolean {
        return hasSessionHints()
    }

    fun hasSessionHints(): Boolean {
        return hasPoTokens() ||
            visitorData.trim().isNotBlank() ||
            dataSyncId.trim().isNotBlank()
    }

    fun hasPoTokens(): Boolean {
        return buildPoTokenValue() != null
    }

    fun buildPoTokenValue(): String? {
        val clientName = clientHint.substringBefore('.').trim().ifBlank { "web" }
        val tokens = buildList {
            if (gvsToken.isNotBlank()) {
                add("$clientName.gvs+${gvsToken.trim()}")
            }
            if (playerToken.isNotBlank()) {
                add("$clientName.player+${playerToken.trim()}")
            }
            val effectiveSubsToken = subsToken.ifBlank { playerToken }.trim()
            if (effectiveSubsToken.isNotBlank()) {
                add("$clientName.subs+$effectiveSubsToken")
            }
        }
        return tokens.distinct().takeIf { it.isNotEmpty() }?.joinToString(",")
    }
}
