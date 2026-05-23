package com.localdownloader.utils

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView

object WebViewSessionSanitizer {
    fun resetSession() {
        runCatching {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
        runCatching {
            WebStorage.getInstance().deleteAllData()
        }
    }

    fun destroyWebView(webView: WebView?) {
        webView ?: return
        runCatching { webView.stopLoading() }
        runCatching { webView.loadUrl("about:blank") }
        runCatching { webView.clearHistory() }
        runCatching { webView.clearCache(true) }
        runCatching { webView.clearFormData() }
        runCatching { webView.removeAllViews() }
        runCatching { webView.destroy() }
    }

    fun clearAndDestroy(webView: WebView?) {
        resetSession()
        destroyWebView(webView)
    }
}
