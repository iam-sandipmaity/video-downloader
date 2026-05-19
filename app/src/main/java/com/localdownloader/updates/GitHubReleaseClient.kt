package com.localdownloader.updates

import com.localdownloader.BuildConfig
import com.localdownloader.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubReleaseClient @Inject constructor(
    private val json: Json,
    private val logger: Logger,
) {

    suspend fun fetchRelease(apiUrl: String): GitHubReleaseDto = withContext(Dispatchers.IO) {
        val payload = requestJson(apiUrl)
        json.decodeFromString(GitHubReleaseDto.serializer(), payload)
    }

    suspend fun fetchReleases(repository: String): List<GitHubReleaseDto> = withContext(Dispatchers.IO) {
        val payload = requestJson("https://api.github.com/repos/$repository/releases")
        json.decodeFromString(ListSerializer(GitHubReleaseDto.serializer()), payload)
    }

    suspend fun downloadFile(
        url: String,
        target: File,
        onProgress: ((Int) -> Unit)? = null,
    ) = withContext(Dispatchers.IO) {
        target.parentFile?.mkdirs()
        val connection = openConnection(url, acceptJson = false)
        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("GitHub download failed: ${extractErrorMessage(connection)}")
            }
            val contentLength = connection.contentLengthLong.takeIf { it > 0L }
            var downloadedBytes = 0L
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead < 0) break
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (contentLength != null) {
                            onProgress?.invoke(((downloadedBytes * 100L) / contentLength).toInt().coerceIn(0, 100))
                        }
                    }
                }
            }
            onProgress?.invoke(100)
        } finally {
            connection.disconnect()
        }
    }

    private fun requestJson(apiUrl: String): String {
        val connection = openConnection(apiUrl, acceptJson = true)
        try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (responseCode !in 200..299) {
                throw IOException("GitHub API request failed: ${extractErrorMessage(body)}")
            }
            return body
        } finally {
            connection.disconnect()
        }
    }

    private fun openConnection(url: String, acceptJson: Boolean): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 20000
        connection.setRequestProperty("User-Agent", BuildConfig.APPLICATION_ID)
        connection.setRequestProperty("Accept", if (acceptJson) "application/vnd.github+json" else "*/*")
        connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        connection.instanceFollowRedirects = true
        return connection
    }

    private fun extractErrorMessage(connection: HttpURLConnection): String {
        return runCatching {
            connection.errorStream?.bufferedReader()?.use { it.readText() }
        }.getOrNull()?.let(::extractErrorMessage)
            ?: "HTTP ${connection.responseCode}"
    }

    private fun extractErrorMessage(body: String): String {
        if (body.isBlank()) return "empty response"
        return runCatching {
            val element = json.parseToJsonElement(body)
            val message = when (element) {
                is JsonObject -> {
                    element["message"]?.jsonPrimitive?.contentOrNull
                        ?: element["error"]?.jsonPrimitive?.contentOrNull
                }
                else -> null
            }
            message ?: body.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        }.getOrElse { error ->
            logger.w("GitHubReleaseClient", "Failed to parse GitHub error response", error)
            body.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        }
    }
}
