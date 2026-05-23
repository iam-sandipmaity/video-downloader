package com.localdownloader.data

import android.content.Context
import com.localdownloader.domain.models.DownloadOptions
import com.localdownloader.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadOptionSecretsStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    private val logger: Logger,
) {

    fun persist(taskId: String, options: DownloadOptions): String {
        val usesSidecarSecrets = options.containsPersistedSecrets()
        if (usesSidecarSecrets) {
            writeSecrets(
                taskId = taskId,
                secrets = PersistedDownloadOptionSecrets.from(options),
            )
        } else {
            clear(taskId)
        }
        return json.encodeToString(
            PersistedDownloadTaskOptions(
                options = if (usesSidecarSecrets) {
                    options.redactedForPersistence()
                } else {
                    options
                },
                usesSidecarSecrets = usesSidecarSecrets,
            ),
        )
    }

    fun hydrate(taskId: String, optionsJson: String): DownloadOptions? {
        val persisted = decodePersistedOptions(optionsJson)
        if (persisted != null) {
            if (!persisted.usesSidecarSecrets) {
                return persisted.options
            }
            val secrets = readSecrets(taskId)
            return if (secrets != null) {
                persisted.options.applyPersistedSecrets(secrets)
            } else {
                logger.w(
                    "DownloadOptionSecretsStore",
                    "Missing or unreadable persisted secrets for taskId=$taskId; falling back to redacted options",
                )
                persisted.options
            }
        }
        return decodeLegacyOptions(optionsJson).also { decoded ->
            if (decoded == null) {
                logger.w("DownloadOptionSecretsStore", "Failed to decode persisted options for taskId=$taskId")
            }
        }
    }

    fun migratePersistedOptions(taskId: String, optionsJson: String): String? {
        val persisted = decodePersistedOptions(optionsJson)
        if (persisted != null) {
            return when {
                !persisted.usesSidecarSecrets && persisted.options.containsPersistedSecrets() ->
                    persist(taskId, persisted.options)
                persisted.usesSidecarSecrets && persisted.options.containsPersistedSecrets() ->
                    persist(taskId, persisted.options)
                else -> null
            }
        }

        val legacyOptions = decodeLegacyOptions(optionsJson) ?: return null
        return persist(taskId, legacyOptions)
    }

    fun clear(taskId: String) {
        secretsFile(taskId).delete()
    }

    fun clear(taskIds: Collection<String>) {
        taskIds.forEach(::clear)
    }

    private fun decodePersistedOptions(optionsJson: String): PersistedDownloadTaskOptions? {
        return runCatching { json.decodeFromString<PersistedDownloadTaskOptions>(optionsJson) }.getOrNull()
    }

    private fun decodeLegacyOptions(optionsJson: String): DownloadOptions? {
        return runCatching { json.decodeFromString<DownloadOptions>(optionsJson) }.getOrNull()
    }

    private fun writeSecrets(taskId: String, secrets: PersistedDownloadOptionSecrets) {
        val target = secretsFile(taskId)
        target.parentFile?.mkdirs()
        val tempFile = File(target.parentFile, "${target.name}.tmp")
        tempFile.writeText(json.encodeToString(secrets))
        if (!tempFile.renameTo(target)) {
            tempFile.copyTo(target, overwrite = true)
            tempFile.delete()
        }
    }

    private fun readSecrets(taskId: String): PersistedDownloadOptionSecrets? {
        val secretFile = secretsFile(taskId)
        if (!secretFile.exists() || !secretFile.isFile) {
            return null
        }
        return runCatching {
            json.decodeFromString<PersistedDownloadOptionSecrets>(secretFile.readText())
        }.onFailure { error ->
            logger.w("DownloadOptionSecretsStore", "Failed reading persisted secrets for taskId=$taskId", error)
        }.getOrNull()
    }

    private fun secretsFile(taskId: String): File {
        return File(secretsDir(), "${sanitizeTaskId(taskId)}.json")
    }

    private fun secretsDir(): File {
        return File(context.noBackupFilesDir, SECRET_DIR_NAME)
    }

    private fun sanitizeTaskId(taskId: String): String {
        return taskId.replace(SAFE_TASK_ID_REGEX, "_")
    }

    private companion object {
        private const val SECRET_DIR_NAME = "task-option-secrets"
        private val SAFE_TASK_ID_REGEX = Regex("[^A-Za-z0-9._-]")
    }
}

@Serializable
internal data class PersistedDownloadTaskOptions(
    val options: DownloadOptions,
    val usesSidecarSecrets: Boolean = false,
)

@Serializable
internal data class PersistedDownloadOptionSecrets(
    val extractorArgs: String? = null,
    val fallbackExtractorArgs: String? = null,
    val loadInfoJsonPath: String? = null,
    val userAgentHeader: String? = null,
    val youtubeCookiesPath: String? = null,
    val youtubePoToken: String? = null,
    val youtubeDataSyncId: String? = null,
) {
    companion object {
        fun from(options: DownloadOptions): PersistedDownloadOptionSecrets {
            return PersistedDownloadOptionSecrets(
                extractorArgs = options.extractorArgs,
                fallbackExtractorArgs = options.fallbackExtractorArgs,
                loadInfoJsonPath = options.loadInfoJsonPath,
                userAgentHeader = options.userAgentHeader,
                youtubeCookiesPath = options.youtubeCookiesPath,
                youtubePoToken = options.youtubePoToken,
                youtubeDataSyncId = options.youtubeDataSyncId,
            )
        }
    }
}

internal fun DownloadOptions.containsPersistedSecrets(): Boolean {
    return !extractorArgs.isNullOrBlank() ||
        !fallbackExtractorArgs.isNullOrBlank() ||
        !loadInfoJsonPath.isNullOrBlank() ||
        !userAgentHeader.isNullOrBlank() ||
        !youtubeCookiesPath.isNullOrBlank() ||
        !youtubePoToken.isNullOrBlank() ||
        !youtubeDataSyncId.isNullOrBlank()
}

internal fun DownloadOptions.redactedForPersistence(): DownloadOptions {
    return copy(
        extractorArgs = null,
        fallbackExtractorArgs = null,
        loadInfoJsonPath = null,
        userAgentHeader = null,
        youtubeCookiesPath = null,
        youtubePoToken = null,
        youtubeDataSyncId = null,
    )
}

internal fun DownloadOptions.applyPersistedSecrets(secrets: PersistedDownloadOptionSecrets): DownloadOptions {
    return copy(
        extractorArgs = secrets.extractorArgs,
        fallbackExtractorArgs = secrets.fallbackExtractorArgs,
        loadInfoJsonPath = secrets.loadInfoJsonPath,
        userAgentHeader = secrets.userAgentHeader,
        youtubeCookiesPath = secrets.youtubeCookiesPath,
        youtubePoToken = secrets.youtubePoToken,
        youtubeDataSyncId = secrets.youtubeDataSyncId,
    )
}
