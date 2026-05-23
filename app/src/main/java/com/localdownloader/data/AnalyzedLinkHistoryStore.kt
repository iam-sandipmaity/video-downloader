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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyzedLinkHistoryStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    private val mutex = Mutex()
    private val file: File
        get() = File(context.noBackupFilesDir, "analyzed-link-history.json")

    suspend fun load(retentionDays: Int): List<AnalyzedLinkRecord> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val existing = readUnsafe()
            val pruned = prune(existing, retentionDays)
            if (pruned.size != existing.size) {
                writeUnsafe(pruned)
            }
            pruned
        }
    }

    suspend fun upsert(
        record: AnalyzedLinkRecord,
        retentionDays: Int,
    ): List<AnalyzedLinkRecord> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val updated = prune(
                listOf(record) + readUnsafe().filterNot { it.webpageUrl == record.webpageUrl },
                retentionDays,
            )
            writeUnsafe(updated)
            updated
        }
    }

    suspend fun remove(webpageUrl: String, retentionDays: Int): List<AnalyzedLinkRecord> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val updated = prune(readUnsafe().filterNot { it.webpageUrl == webpageUrl }, retentionDays)
            writeUnsafe(updated)
            updated
        }
    }

    suspend fun replaceAll(
        records: List<AnalyzedLinkRecord>,
        retentionDays: Int,
    ): List<AnalyzedLinkRecord> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val updated = prune(records, retentionDays)
            writeUnsafe(updated)
            updated
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

    private fun writeUnsafe(records: List<AnalyzedLinkRecord>) {
        val parent = file.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }
        file.writeText(json.encodeToString(ListSerializer(AnalyzedLinkRecord.serializer()), records))
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
}
