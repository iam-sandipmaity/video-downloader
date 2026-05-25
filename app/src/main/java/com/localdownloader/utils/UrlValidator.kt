package com.localdownloader.utils

import java.net.URI
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class SecureUrlNormalization(
    val normalizedUrl: String,
    val upgradedToHttps: Boolean,
)

@Singleton
class UrlValidator @Inject constructor() {
    fun normalizeForSecureUse(url: String): SecureUrlNormalization? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null

        val withScheme = when {
            trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("http://", ignoreCase = true) -> trimmed
            SCHEME_REGEX.containsMatchIn(trimmed) -> return null
            else -> "https://$trimmed"
        }
        val parsed = runCatching { URI(withScheme) }.getOrNull() ?: return null
        val scheme = parsed.scheme?.lowercase(Locale.ROOT) ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = parsed.host?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val normalizedScheme = if (scheme == "http") "https" else scheme
        val normalizedUri = URI(
            normalizedScheme,
            parsed.userInfo,
            host.lowercase(Locale.ROOT),
            parsed.port,
            parsed.rawPath,
            parsed.rawQuery,
            parsed.rawFragment,
        ).toASCIIString()

        return when {
            scheme == "https" -> SecureUrlNormalization(
                normalizedUrl = normalizedUri,
                upgradedToHttps = false,
            )
            scheme == "http" -> SecureUrlNormalization(
                normalizedUrl = normalizedUri,
                upgradedToHttps = true,
            )
            else -> null
        }
    }

    private companion object {
        private val SCHEME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")
    }
}
