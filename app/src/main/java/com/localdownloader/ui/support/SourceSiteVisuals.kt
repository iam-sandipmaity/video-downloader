package com.localdownloader.ui.support

import android.net.Uri
import androidx.compose.ui.graphics.Color

data class SourceSiteVisual(
    val label: String,
    val assetPath: String,
    val accent: Color,
)

fun sourceSiteVisualForUrl(url: String): SourceSiteVisual? {
    val host = normalizedHost(url) ?: return null
    return when {
        host == "youtube.com" || host == "youtu.be" || host.endsWith(".youtube.com") ->
            SourceSiteVisual(
                label = "YouTube",
                assetPath = "file:///android_asset/platform_logos/youtube.svg",
                accent = Color(0xFFFF3B30),
            )

        host == "instagram.com" || host.endsWith(".instagram.com") ->
            SourceSiteVisual(
                label = "Instagram",
                assetPath = "file:///android_asset/platform_logos/instagram.svg",
                accent = Color(0xFFE4405F),
            )

        host == "tiktok.com" || host.endsWith(".tiktok.com") ->
            SourceSiteVisual(
                label = "TikTok",
                assetPath = "file:///android_asset/platform_logos/tiktok.svg",
                accent = Color(0xFF25F4EE),
            )

        host == "x.com" || host.endsWith(".x.com") || host == "twitter.com" || host.endsWith(".twitter.com") ->
            SourceSiteVisual(
                label = "X",
                assetPath = "file:///android_asset/platform_logos/x.svg",
                accent = Color(0xFFB0A7BC),
            )

        host == "facebook.com" || host.endsWith(".facebook.com") || host == "fb.watch" ->
            SourceSiteVisual(
                label = "Facebook",
                assetPath = "file:///android_asset/platform_logos/facebook.svg",
                accent = Color(0xFF1877F2),
            )

        host == "dailymotion.com" || host.endsWith(".dailymotion.com") || host == "dai.ly" ->
            SourceSiteVisual(
                label = "Dailymotion",
                assetPath = "file:///android_asset/platform_logos/dailymotion.svg",
                accent = Color(0xFF0066DC),
            )

        host == "pinterest.com" || host.endsWith(".pinterest.com") || host == "pin.it" ->
            SourceSiteVisual(
                label = "Pinterest",
                assetPath = "file:///android_asset/platform_logos/pinterest.svg",
                accent = Color(0xFFE60023),
            )

        host == "twitch.tv" || host.endsWith(".twitch.tv") ->
            SourceSiteVisual(
                label = "Twitch",
                assetPath = "file:///android_asset/platform_logos/twitch.svg",
                accent = Color(0xFF9146FF),
            )

        host == "linkedin.com" || host.endsWith(".linkedin.com") ->
            SourceSiteVisual(
                label = "LinkedIn",
                assetPath = "file:///android_asset/platform_logos/linkedin.svg",
                accent = Color(0xFF0A66C2),
            )

        else -> null
    }
}

fun sourceHostLabel(url: String): String? {
    return normalizedHost(url)
}

private fun normalizedHost(url: String): String? {
    val host = runCatching { Uri.parse(url).host }.getOrNull()
        ?.removePrefix("www.")
        ?.trim()
        ?.lowercase()
        .orEmpty()
    return host.ifBlank { null }
}
