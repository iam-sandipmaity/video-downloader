package com.localdownloader.utils

import android.annotation.SuppressLint
import android.os.Build
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * Enables JavaScript only for tightly controlled in-app auth/token flows.
 * Callers are expected to load trusted HTTPS content and keep file/content
 * access disabled.
 */
@SuppressLint("SetJavaScriptEnabled")
fun WebView.configureRestrictedJavascriptSession(
    enableCookies: Boolean = false,
    enableThirdPartyCookies: Boolean = false,
) {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        settings.safeBrowsingEnabled = true
    }
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
    settings.allowFileAccessFromFileURLs = false
    settings.allowUniversalAccessFromFileURLs = false

    if (enableCookies) {
        CookieManager.getInstance().setAcceptCookie(true)
    }
    if (enableThirdPartyCookies) {
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
    }
}
