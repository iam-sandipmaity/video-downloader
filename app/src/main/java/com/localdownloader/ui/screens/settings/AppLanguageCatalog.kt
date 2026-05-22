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
    "ar",
    "bn",
    "de",
    "es",
    "fr",
    "gu",
    "hi",
    "id",
    "it",
    "ja",
    "kn",
    "ko",
    "ml",
    "mr",
    "ne",
    "nl",
    "pa",
    "pl",
    "pt-BR",
    "pt-PT",
    "ru",
    "ta",
    "te",
    "th",
    "tr",
    "uk",
    "ur",
    "vi",
    "zh-CN",
    "zh-TW",
)

fun supportedAppLanguageOptions(): List<AppLanguageOption> {
    return supportedLanguageTags.map { tag ->
        val locale = Locale.forLanguageTag(tag)
        val nativeLabel = locale.readableDisplayName(locale)
        val englishLabel = locale.readableDisplayName(Locale.ENGLISH)
        AppLanguageOption(
            tag = tag,
            title = nativeLabel,
            subtitle = if (nativeLabel.equals(englishLabel, ignoreCase = true)) {
                "Interface language"
            } else {
                englishLabel
            },
        )
    }
}

fun appLanguageLabel(languageTag: String): String {
    if (languageTag == SYSTEM_LANGUAGE_TAG) return "System default"
    val locale = Locale.forLanguageTag(languageTag)
    val nativeLabel = locale.readableDisplayName(locale)
    val englishLabel = locale.readableDisplayName(Locale.ENGLISH)
    return if (nativeLabel.equals(englishLabel, ignoreCase = true)) {
        nativeLabel
    } else {
        "$nativeLabel • $englishLabel"
    }
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
