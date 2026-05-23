package com.localdownloader.utils

import android.util.Patterns
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
            else -> "https://$trimmed"
        }
        if (!Patterns.WEB_URL.matcher(withScheme).matches()) return null

        return when {
            withScheme.startsWith("https://", ignoreCase = true) -> SecureUrlNormalization(
                normalizedUrl = withScheme,
                upgradedToHttps = false,
            )
            withScheme.startsWith("http://", ignoreCase = true) -> SecureUrlNormalization(
                normalizedUrl = HTTP_SCHEME_REGEX.replace(withScheme, "https://"),
                upgradedToHttps = true,
            )
            else -> null
        }
    }

    private companion object {
        private val HTTP_SCHEME_REGEX = Regex("^http://", RegexOption.IGNORE_CASE)
    }
}
