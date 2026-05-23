package com.localdownloader.utils

import java.io.File
import java.net.URI

object SensitiveDataSanitizer {
    private val httpUrlRegex = Regex("""(?<!<url:)https?://[^\s"'<>]+""", RegexOption.IGNORE_CASE)
    private val windowsPathRegex = Regex("""[A-Za-z]:\\[^\s"'<>]+""")
    private val unixPathRegex = Regex("""(?<!<url:)(?<!content:)(?<!file:)/(?:[^\s"'<>/]+/)+[^\s"'<>/]+""")

    fun sanitize(message: String): String {
        if (message.isBlank()) return message

        var sanitized = message
        sanitized = redactTokenAssignments(sanitized)
        sanitized = redactCommandFlagValues(sanitized)
        sanitized = redactKeyValueAssignments(sanitized)
        sanitized = httpUrlRegex.replace(sanitized) { match ->
            "<url:${describeUrl(match.value)}>"
        }
        sanitized = windowsPathRegex.replace(sanitized) { match ->
            "<path:${describePath(match.value)}>"
        }
        sanitized = unixPathRegex.replace(sanitized) { match ->
            "<path:${describePath(match.value)}>"
        }
        return sanitized
    }

    fun describeUrl(url: String?): String {
        val candidate = url?.trim().orEmpty()
        if (candidate.isBlank()) return "unknown"

        return runCatching {
            val uri = URI(candidate)
            val scheme = uri.scheme?.ifBlank { "https" } ?: "https"
            val host = uri.host?.removePrefix(".")?.ifBlank { "unknown" } ?: "unknown"
            val pathHint = uri.path?.takeIf { it.isNotBlank() && it != "/" }?.let { " (path)" }.orEmpty()
            "$scheme://$host$pathHint"
        }.getOrElse {
            candidate.substringBefore('?').substringBefore('#').take(80)
        }
    }

    fun describePath(path: String?): String {
        val candidate = path?.trim().orEmpty()
        if (candidate.isBlank()) return "path"

        return runCatching {
            File(candidate).name.ifBlank { "path" }
        }.getOrDefault("path")
    }

    private fun redactTokenAssignments(message: String): String {
        return message
            .replace(Regex("""(?i)po_token=[^\s;]+"""), "po_token=<redacted>")
            .replace(Regex("""(?i)data_sync_id=[^\s;]+"""), "data_sync_id=<redacted>")
            .replace(Regex("""(?i)x-goog-api-key[:=][^\s;]+"""), "x-goog-api-key=<redacted>")
            .replace(Regex("""(?i)visitor(data|_data)?[:=][^\s;]+"""), "visitor_data=<redacted>")
    }

    private fun redactCommandFlagValues(message: String): String {
        var sanitized = message
        val pathFlags = listOf("--cookies", "--load-info-json", "--ffmpeg-location", "--cache-dir", "-o", "-P")
        pathFlags.forEach { flag ->
            val pattern = Regex("""${Regex.escape(flag)}\s+([^\s"'<>]+)""")
            sanitized = sanitized.replace(pattern) { match ->
                "$flag <path:${describePath(match.groupValues[1])}>"
            }
        }
        sanitized = sanitized.replace(Regex("""--add-header\s+User-Agent:[^\s"'<>]+""", RegexOption.IGNORE_CASE)) {
            "--add-header User-Agent:<redacted>"
        }
        sanitized = sanitized.replace(Regex("""--extractor-args\s+([^\s"'<>]+)""")) { match ->
            "--extractor-args ${redactTokenAssignments(match.groupValues[1])}"
        }
        return sanitized
    }

    private fun redactKeyValueAssignments(message: String): String {
        var sanitized = message
        val urlKeys = listOf("url", "URL", "webpageUrl")
        urlKeys.forEach { key ->
            sanitized = sanitized.replace(Regex("""\b${Regex.escape(key)}=([^\s"'<>]+)""")) { match ->
                "$key=<url:${describeUrl(match.groupValues[1])}>"
            }
        }

        val pathKeys = listOf(
            "outputTemplate",
            "outputPath",
            "path",
            "python",
            "ytdlp",
            "ffmpeg",
            "quickjs",
            "cookiesPath",
            "loadInfoJsonPath",
        )
        pathKeys.forEach { key ->
            sanitized = sanitized.replace(Regex("""\b${Regex.escape(key)}=([^\s"'<>]+)""")) { match ->
                "$key=<path:${describePath(match.groupValues[1])}>"
            }
        }
        return sanitized
    }
}
