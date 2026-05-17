package com.localdownloader.utils

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.webkit.CookieManager
import java.io.File
import java.net.URI

object WebViewCookieExporter {
    private val youtubeRelatedHosts = setOf(
        "youtube.com",
        "youtu.be",
        "google.com",
        "googlevideo.com",
        "ytimg.com",
        "ggpht.com",
        "doubleclick.net",
        "youtube-nocookie.com",
    )
    private val projection = arrayOf(
        "host_key",
        "expires_utc",
        "path",
        "name",
        "value",
        "is_secure",
    )

    fun exportForUrl(context: Context, url: String): String {
        val normalizedUrl = CookieTextCodec.normalizeUrl(url)
            ?: throw IllegalArgumentException("That site URL could not be used for cookies.")
        val targetHost = URI(normalizedUrl).host.orEmpty().removePrefix(".")

        runCatching {
            CookieManager.getInstance().flush()
        }

        val cookieLines = readCookieLines(context, targetHost)
        if (cookieLines.isNotEmpty()) {
            return CookieTextCodec.buildStoredText(
                normalizedUrl,
                cookieLines.joinToString("\n"),
            )
        }

        val cookieHeader = CookieManager.getInstance().getCookie(normalizedUrl).orEmpty()
        if (cookieHeader.isBlank()) {
            throw IllegalStateException("No cookies were captured from that page yet.")
        }
        return CookieTextCodec.fromCookieHeader(normalizedUrl, cookieHeader)
    }

    private fun readCookieLines(context: Context, targetHost: String): List<String> {
        val dbPath = File(context.applicationInfo.dataDir)
            .walkTopDown()
            .firstOrNull { it.name == "Cookies" }
            ?: return emptyList()

        val db = SQLiteDatabase.openDatabase(
            dbPath.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY,
        )

        return db.useDatabase {
            val cookies = mutableListOf<String>()
            query("cookies", projection, null, null, null, null, null).use { cursor ->
                while (cursor.moveToNext()) {
                    val hostKey = cursor.getString(cursor.getColumnIndexOrThrow("host_key")).orEmpty()
                    val expiry = cursor.getLong(cursor.getColumnIndexOrThrow("expires_utc"))
                    val path = cursor.getString(cursor.getColumnIndexOrThrow("path")).orEmpty().ifBlank { "/" }
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name")).orEmpty()
                    val value = cursor.getString(cursor.getColumnIndexOrThrow("value")).orEmpty()
                    val secure = cursor.getLong(cursor.getColumnIndexOrThrow("is_secure")) == 1L
                    if (hostKey.isBlank() || name.isBlank()) continue
                    val normalizedHost = hostKey.removePrefix(".")
                    if (!matchesTargetHost(targetHost, normalizedHost)) continue

                    val domain = if (hostKey.startsWith(".")) hostKey else ".$hostKey"
                    cookies += listOf(
                        domain,
                        "TRUE",
                        path,
                        if (secure) "TRUE" else "FALSE",
                        expiry.toString(),
                        name,
                        value,
                    ).joinToString("\t")
                }
            }
            cookies
        }
    }

    private inline fun <T> SQLiteDatabase.useDatabase(block: SQLiteDatabase.() -> T): T {
        return try {
            block()
        } finally {
            close()
        }
    }

    private fun matchesTargetHost(targetHost: String, cookieHost: String): Boolean {
        if (cookieHost == targetHost ||
            cookieHost.endsWith(".$targetHost") ||
            targetHost.endsWith(".$cookieHost")
        ) {
            return true
        }

        val youtubeTarget = youtubeRelatedHosts.any { targetHost == it || targetHost.endsWith(".$it") }
        return youtubeTarget && youtubeRelatedHosts.any { cookieHost == it || cookieHost.endsWith(".$it") }
    }
}
