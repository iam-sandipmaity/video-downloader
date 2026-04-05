package com.localdownloader.data.persistence

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "progress_percent") val progressPercent: Int = 0,
    @ColumnInfo(name = "speed") val speed: String? = null,
    @ColumnInfo(name = "eta") val eta: String? = null,
    @ColumnInfo(name = "output_path") val outputPath: String? = null,
    @ColumnInfo(name = "downloaded_str") val downloadedStr: String? = null,
    @ColumnInfo(name = "total_size_str") val totalSizeStr: String? = null,
    @ColumnInfo(name = "error_message") val errorMessage: String? = null,
    @ColumnInfo(name = "debug_trace") val debugTrace: String? = null,
    @ColumnInfo(name = "options_json") val optionsJson: String? = null,
    @ColumnInfo(name = "created_at") val createdAtEpochMs: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAtEpochMs: Long = System.currentTimeMillis(),
)
