package com.localdownloader.ui.support

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.util.Locale

internal fun formatByteCount(bytes: Long): String {
    if (bytes < 0L) return "unknown"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex += 1
    }
    return if (unitIndex == 0) {
        "${bytes} ${units[unitIndex]}"
    } else {
        String.format(Locale.US, "%.1f %s", value, units[unitIndex])
    }
}

internal fun formatJvmMemoryLines(
    maxBytes: Long,
    totalBytes: Long,
    freeBytes: Long,
): List<String> {
    val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
    return listOf(
        "- JVM heap used: ${formatByteCount(usedBytes)} / ${formatByteCount(totalBytes)}",
        "- JVM heap max: ${formatByteCount(maxBytes)}",
    )
}

internal fun formatStorageLines(
    internalTotalBytes: Long,
    internalAvailableBytes: Long,
    sharedTotalBytes: Long?,
    sharedAvailableBytes: Long?,
): List<String> {
    return buildList {
        add("- App internal storage: ${formatByteCount(internalAvailableBytes)} free of ${formatByteCount(internalTotalBytes)}")
        if (sharedTotalBytes != null && sharedAvailableBytes != null) {
            add("- Shared storage: ${formatByteCount(sharedAvailableBytes)} free of ${formatByteCount(sharedTotalBytes)}")
        }
    }
}

internal fun deviceMemorySnapshot(context: Context): DeviceMemorySnapshot {
    val runtime = Runtime.getRuntime()
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo().also { info ->
        activityManager?.getMemoryInfo(info)
    }
    val internal = storageSnapshot(context.filesDir)
    val shared = storageSnapshot(Environment.getDataDirectory())
    return DeviceMemorySnapshot(
        jvmMaxBytes = runtime.maxMemory(),
        jvmTotalBytes = runtime.totalMemory(),
        jvmFreeBytes = runtime.freeMemory(),
        systemAvailBytes = memoryInfo.availMem.takeIf { activityManager != null },
        systemTotalBytes = memoryInfo.totalMem.takeIf { activityManager != null && it > 0L },
        lowMemory = memoryInfo.lowMemory.takeIf { activityManager != null },
        internalTotalBytes = internal?.totalBytes,
        internalAvailableBytes = internal?.availableBytes,
        sharedTotalBytes = shared?.totalBytes,
        sharedAvailableBytes = shared?.availableBytes,
    )
}

internal fun DeviceMemorySnapshot.toReportLines(): List<String> {
    return buildList {
        addAll(
            formatJvmMemoryLines(
                maxBytes = jvmMaxBytes,
                totalBytes = jvmTotalBytes,
                freeBytes = jvmFreeBytes,
            ),
        )
        if (systemAvailBytes != null) {
            val total = systemTotalBytes?.let { " of ${formatByteCount(it)}" }.orEmpty()
            add("- System memory available: ${formatByteCount(systemAvailBytes)}$total")
        }
        if (lowMemory == true) {
            add("- System low-memory flag: true")
        }
        val internalTotal = internalTotalBytes
        val internalAvail = internalAvailableBytes
        if (internalTotal != null && internalAvail != null) {
            addAll(
                formatStorageLines(
                    internalTotalBytes = internalTotal,
                    internalAvailableBytes = internalAvail,
                    sharedTotalBytes = sharedTotalBytes,
                    sharedAvailableBytes = sharedAvailableBytes,
                ),
            )
        }
    }
}

internal data class DeviceMemorySnapshot(
    val jvmMaxBytes: Long,
    val jvmTotalBytes: Long,
    val jvmFreeBytes: Long,
    val systemAvailBytes: Long?,
    val systemTotalBytes: Long?,
    val lowMemory: Boolean?,
    val internalTotalBytes: Long?,
    val internalAvailableBytes: Long?,
    val sharedTotalBytes: Long?,
    val sharedAvailableBytes: Long?,
)

private data class StorageSnapshot(
    val totalBytes: Long,
    val availableBytes: Long,
)

private fun storageSnapshot(path: File): StorageSnapshot? {
    return runCatching {
        if (!path.exists()) return null
        val stats = StatFs(path.absolutePath)
        StorageSnapshot(
            totalBytes = stats.blockCountLong * stats.blockSizeLong,
            availableBytes = stats.availableBlocksLong * stats.blockSizeLong,
        )
    }.getOrNull()
}
