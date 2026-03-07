package com.localdownloader.utils

import android.util.Patterns
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UrlValidator @Inject constructor() {
    fun isValidHttpUrl(url: String): Boolean {
        if (url.isBlank()) return false
        if (!Patterns.WEB_URL.matcher(url).matches()) return false
        return url.startsWith("http://") || url.startsWith("https://")
    }
}
