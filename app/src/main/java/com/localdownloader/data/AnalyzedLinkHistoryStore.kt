package com.localdownloader.data

import android.content.Context
import com.localdownloader.domain.models.AnalyzedLinkRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyzedLinkHistoryStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    private val mutex = Mutex()
    private val backupFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
    private val file: File
        get() = File(context.noBackupFilesDir, "analyzed-link-history.json")

    suspend fun load(retentionDays: Int): List<AnalyzedLinkRecord> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val existing = readUnsafe()
            val prepared = prepareForPersistence(existing, retentionDays)
            if (prepared.records != existing || prepared.backupPayload != null) {
                writeUnsafe(prepared)
            }
            prepared.records
        }
    }

    suspend fun upsert(
        record: AnalyzedLinkRecord,
        retentionDays: Int,
    ): List<AnalyzedLinkRecord> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val prepared = prepareForPersistence(
                listOf(record) + readUnsafe().filterNot { it.webpageUrl == record.webpageUrl },
                retentionDays,
            )
            writeUnsafe(prepared)
            prepared.records
        }
    }

    suspend fun remove(webpageUrl: String, retentionDays: Int): List<AnalyzedLinkRecord> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val prepared = prepareForPersistence(
                readUnsafe().filterNot { it.webpageUrl == webpageUrl },
                retentionDays,
            )
            writeUnsafe(prepared)
            prepared.records
        }
    }

    suspend fun replaceAll(
        records: List<AnalyzedLinkRecord>,
        retentionDays: Int,
    ): List<AnalyzedLinkRecord> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val prepared = prepareForPersistence(records, retentionDays)
            writeUnsafe(prepared)
            prepared.records
        }
    }

    suspend fun clear() = mutex.withLock {
        withContext(Dispatchers.IO) {
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private fun readUnsafe(): List<AnalyzedLinkRecord> {
        if (!file.exists()) return emptyList()
        val payload = runCatching { file.readText() }.getOrDefault("")
        if (payload.isBlank()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(AnalyzedLinkRecord.serializer()), payload)
        }.getOrDefault(emptyList())
    }

    private fun writeUnsafe(prepared: PreparedHistoryPayload) {
        val parent = file.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        prepared.backupPayload?.let(::writeBackupUnsafe)
        file.writeText(prepared.payload)
    }

    private fun prepareForPersistence(
        records: List<AnalyzedLinkRecord>,
        retentionDays: Int,
    ): PreparedHistoryPayload {
        val retained = prune(records, retentionDays)
        val payload = encodeRecords(retained)
        if (payload.toByteArray(Charsets.UTF_8).size <= MAX_HISTORY_FILE_SIZE_BYTES) {
            return PreparedHistoryPayload(
                records = retained,
                payload = payload,
                backupPayload = null,
            )
        }
        val trimmed = trimToSize(retained)
        return PreparedHistoryPayload(
            records = trimmed,
            payload = encodeRecords(trimmed),
            backupPayload = payload,
        )
    }

    private fun encodeRecords(records: List<AnalyzedLinkRecord>): String {
        return json.encodeToString(ListSerializer(AnalyzedLinkRecord.serializer()), records)
    }

    private fun trimToSize(records: List<AnalyzedLinkRecord>): List<AnalyzedLinkRecord> {
        if (records.isEmpty()) return emptyList()
        for (size in records.size downTo 1) {
            val candidate = records.take(size)
            if (encodeRecords(candidate).toByteArray(Charsets.UTF_8).size <= MAX_HISTORY_FILE_SIZE_BYTES) {
                return candidate
            }
        }
        return listOf(records.first())
    }

    private fun writeBackupUnsafe(payload: String) {
        val parent = file.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        historyBackupFile().writeText(payload)
        pruneBackupFilesUnsafe()
    }

    private fun historyBackupFile(): File {
        val parent = file.parentFile ?: context.noBackupFilesDir
        return File(parent, "analyzed-link-history-${LocalDateTime.now().format(backupFormatter)}.json")
    }

    private fun pruneBackupFilesUnsafe() {
        val parent = file.parentFile ?: return
        parent.listFiles()
            ?.filter { candidate ->
                candidate.isFile &&
                    candidate.name.startsWith("analyzed-link-history-") &&
                    candidate.name.endsWith(".json")
            }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_HISTORY_BACKUP_FILES)
            ?.forEach { candidate ->
                runCatching { candidate.delete() }
            }
    }

    private fun prune(
        records: List<AnalyzedLinkRecord>,
        retentionDays: Int,
    ): List<AnalyzedLinkRecord> {
        val retentionWindowMs = retentionDays.coerceAtLeast(1) * 24L * 60L * 60L * 1000L
        val cutoff = System.currentTimeMillis() - retentionWindowMs
        return records
            .filter { it.analyzedAtEpochMs >= cutoff }
            .sortedByDescending { it.analyzedAtEpochMs }
    }

    private data class PreparedHistoryPayload(
        val records: List<AnalyzedLinkRecord>,
        val payload: String,
        val backupPayload: String?,
    )

    private companion object {
        private const val MAX_HISTORY_FILE_SIZE_BYTES = 5L * 1024L * 1024L
        private const val MAX_HISTORY_BACKUP_FILES = 3
    }
}
