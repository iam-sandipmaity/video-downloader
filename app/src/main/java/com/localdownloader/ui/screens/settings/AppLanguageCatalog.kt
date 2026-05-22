package com.localdownloader.ui.screens.settings

import com.localdownloader.domain.models.SYSTEM_LANGUAGE_TAG
import java.util.Locale

data class AppLanguageOption(
    val tag: String,
    val title: String,
    val subtitle: String,
)

private val supportedLanguageTags = listOf(
    "en",
    "bn",
    "hi",
)

fun supportedAppLanguageOptions(interfaceLanguageLabel: String): List<AppLanguageOption> {
    return supportedLanguageTags.map { tag ->
        val locale = Locale.forLanguageTag(tag)
        val nativeLabel = locale.readableDisplayName(locale)
        val englishLabel = locale.readableDisplayName(Locale.ENGLISH)
        AppLanguageOption(
            tag = tag,
            title = nativeLabel,
            subtitle = if (nativeLabel.equals(englishLabel, ignoreCase = true)) {
                interfaceLanguageLabel
            } else {
                englishLabel
            },
        )
    }
}

fun appLanguageLabel(languageTag: String, systemDefaultLabel: String): String {
    if (languageTag == SYSTEM_LANGUAGE_TAG) return systemDefaultLabel
    val locale = Locale.forLanguageTag(languageTag)
    val nativeLabel = locale.readableDisplayName(locale)
    return nativeLabel
}

private fun Locale.readableDisplayName(displayLocale: Locale): String {
    val fallback = displayLanguage.ifBlank { toLanguageTag() }
    val label = runCatching { getDisplayName(displayLocale) }
        .getOrDefault(fallback)
        .trim()
        .ifBlank { fallback }
    return label.replaceFirstChar { first ->
        if (first.isLowerCase()) {
            first.titlecase(displayLocale)
        } else {
            first.toString()
        }
    }
}
