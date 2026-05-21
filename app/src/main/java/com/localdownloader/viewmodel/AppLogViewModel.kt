package com.localdownloader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localdownloader.utils.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class AppLogViewModel @Inject constructor(
    private val logger: Logger,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppLogUiState())
    val uiState: StateFlow<AppLogUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val entries = parseLogEntries(
                    logger.appLogFiles().flatMap { file ->
                        runCatching { file.readText() }
                            .getOrDefault("")
                            .lineSequence()
                            .toList()
                    },
                )
                val availableDays = entries.mapNotNull { entry ->
                    entry.day
                }.distinct().sortedDescending()
                val selectedDay = _uiState.value.selectedDay?.takeIf { it in availableDays }
                val selectedOutcome = _uiState.value.selectedOutcome
                AppLogUiState(
                    isLoading = false,
                    entries = entries,
                    filteredEntries = filterEntries(
                        entries = entries,
                        outcome = selectedOutcome,
                        day = selectedDay,
                    ),
                    availableDays = availableDays,
                    selectedOutcome = selectedOutcome,
                    selectedDay = selectedDay,
                    errorMessage = null,
                    lastUpdatedAt = System.currentTimeMillis(),
                )
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure { error ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to read app.log right now.",
                    )
                }
            }
        }
    }

    fun setOutcomeFilter(filter: AppLogOutcomeFilter) {
        _uiState.update { state ->
            state.copy(
                selectedOutcome = filter,
                filteredEntries = filterEntries(
                    entries = state.entries,
                    outcome = filter,
                    day = state.selectedDay,
                ),
            )
        }
    }

    fun setDayFilter(day: String?) {
        _uiState.update { state ->
            state.copy(
                selectedDay = day,
                filteredEntries = filterEntries(
                    entries = state.entries,
                    outcome = state.selectedOutcome,
                    day = day,
                ),
            )
        }
    }

    private fun filterEntries(
        entries: List<AppLogEntry>,
        outcome: AppLogOutcomeFilter,
        day: String?,
    ): List<AppLogEntry> {
        return entries.filter { entry ->
            val matchesOutcome = when (outcome) {
                AppLogOutcomeFilter.ALL -> true
                AppLogOutcomeFilter.FAILED -> entry.category == AppLogEntryCategory.FAILED
                AppLogOutcomeFilter.SUCCESSFUL -> entry.category == AppLogEntryCategory.SUCCESSFUL
            }
            val matchesDay = day == null || entry.day == day
            matchesOutcome && matchesDay
        }
    }

    private fun parseLogEntries(lines: List<String>): List<AppLogEntry> {
        val entries = mutableListOf<AppLogEntry>()
        var current: ParsedLogEntryBuilder? = null

        lines.forEach { line ->
            val match = LOG_LINE_PATTERN.matchEntire(line)
            if (match != null) {
                current?.build()?.let(entries::add)
                current = ParsedLogEntryBuilder(
                    datePart = match.groupValues[1],
                    timePart = match.groupValues[2],
                    level = match.groupValues[3],
                    tag = match.groupValues[4],
                    thread = match.groupValues[5],
                    message = match.groupValues[6],
                    rawLines = mutableListOf(line),
                )
            } else if (current != null) {
                current = current?.copy(
                    rawLines = (current?.rawLines ?: mutableListOf()).apply { add(line) },
                    details = current?.details.orEmpty().let { existing ->
                        if (existing.isBlank()) line else "$existing\n$line"
                    },
                )
            } else if (line.isNotBlank()) {
                current = ParsedLogEntryBuilder(
                    datePart = null,
                    timePart = null,
                    level = null,
                    tag = null,
                    thread = null,
                    message = line,
                    details = "",
                    rawLines = mutableListOf(line),
                )
            }
        }

        current?.build()?.let(entries::add)
        return entries
    }

    private fun ParsedLogEntryBuilder.build(): AppLogEntry {
        val timestamp = if (!datePart.isNullOrBlank() && !timePart.isNullOrBlank()) {
            runCatching {
                LocalDateTime.parse(
                    "$datePart $timePart",
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
                )
            }.getOrNull()
        } else {
            null
        }
        val normalizedText = buildString {
            append(message)
            if (details.isNotBlank()) {
                append('\n')
                append(details)
            }
        }
        val category = classifyEntry(level = level, text = normalizedText)
        return AppLogEntry(
            timestamp = timestamp,
            day = timestamp?.toLocalDate()?.toString(),
            level = level,
            tag = tag,
            thread = thread,
            message = message,
            details = details.ifBlank { null },
            rawText = rawLines.joinToString("\n"),
            category = category,
        )
    }

    private fun classifyEntry(level: String?, text: String): AppLogEntryCategory {
        val normalized = text.lowercase()
        return when {
            level == "E" || level == "W" ||
                normalized.contains(" failed") ||
                normalized.contains("error") ||
                normalized.contains("exception") ||
                normalized.contains("unable") ||
                normalized.contains("rejected") ||
                normalized.contains("stuck") ->
                AppLogEntryCategory.FAILED

            normalized.contains("success") ||
                normalized.contains("completed") ||
                normalized.contains("saved") ||
                normalized.contains("queued") ||
                normalized.contains("updated") ||
                normalized.contains("installed") ||
                normalized.contains("refreshed") ->
                AppLogEntryCategory.SUCCESSFUL

            else -> AppLogEntryCategory.OTHER
        }
    }

    private companion object {
        val LOG_LINE_PATTERN = Regex(
            """^(\d{4}-\d{2}-\d{2}) (\d{2}:\d{2}:\d{2}\.\d{3}) ([A-Z])/([^\s]+) \[([^\]]+)] (.*)$""",
        )
    }
}

data class AppLogUiState(
    val isLoading: Boolean = true,
    val entries: List<AppLogEntry> = emptyList(),
    val filteredEntries: List<AppLogEntry> = emptyList(),
    val availableDays: List<String> = emptyList(),
    val selectedOutcome: AppLogOutcomeFilter = AppLogOutcomeFilter.ALL,
    val selectedDay: String? = null,
    val errorMessage: String? = null,
    val lastUpdatedAt: Long? = null,
)

data class AppLogEntry(
    val timestamp: LocalDateTime?,
    val day: String?,
    val level: String?,
    val tag: String?,
    val thread: String?,
    val message: String,
    val details: String?,
    val rawText: String,
    val category: AppLogEntryCategory,
)

enum class AppLogOutcomeFilter(val label: String) {
    ALL("All"),
    FAILED("Failed"),
    SUCCESSFUL("Successful"),
}

enum class AppLogEntryCategory {
    FAILED,
    SUCCESSFUL,
    OTHER,
}

private data class ParsedLogEntryBuilder(
    val datePart: String?,
    val timePart: String?,
    val level: String?,
    val tag: String?,
    val thread: String?,
    val message: String,
    val details: String = "",
    val rawLines: MutableList<String>,
)

fun formatAppLogDayLabel(day: String): String {
    return runCatching {
        LocalDate.parse(day).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    }.getOrDefault(day)
}
