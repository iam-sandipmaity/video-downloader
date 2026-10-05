package com.localdownloader.media.lyrics

object LyricsParser {

    private val LRC_TIMESTAMP_REGEX = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?\]""")
    private val LRC_HEADER_REGEX = Regex("""^\[(ti|ar|al|offset):([^\]]+)\]$""", RegexOption.IGNORE_CASE)

    private val SRT_TIME_REGEX = Regex(
        """(\d{1,2}):(\d{2}):(\d{2})[,.](\d{1,3})\s*-->\s*(\d{1,2}):(\d{2}):(\d{2})[,.](\d{1,3})"""
    )

    private val VTT_TIME_REGEX = Regex(
        """(?:(\d{1,2}):)?(\d{2}):(\d{2})\.(\d{1,3})\s*-->\s*(?:(\d{1,2}):)?(\d{2}):(\d{2})\.(\d{1,3})"""
    )

    private val HTML_TAG_REGEX = Regex("""<[^>]*>""")

    fun parse(rawContent: String, formatHint: String? = null): LyricsDocument {
        val trimmed = rawContent.trim()
        if (trimmed.isEmpty()) {
            return LyricsDocument()
        }

        val hint = formatHint?.lowercase()?.trim()

        return when {
            hint == "lrc" || (hint == null && isLikelyLrc(trimmed)) -> parseLrc(trimmed)
            hint in listOf("vtt", "webvtt") || (hint == null && isLikelyVtt(trimmed)) -> parseVtt(trimmed)
            hint == "srt" || (hint == null && isLikelySrt(trimmed)) -> parseSrt(trimmed)
            else -> parseFallback(trimmed)
        }
    }

    private fun isLikelyLrc(content: String): Boolean {
        return LRC_TIMESTAMP_REGEX.containsMatchIn(content)
    }

    private fun isLikelyVtt(content: String): Boolean {
        return content.startsWith("WEBVTT", ignoreCase = true) || VTT_TIME_REGEX.containsMatchIn(content)
    }

    private fun isLikelySrt(content: String): Boolean {
        return SRT_TIME_REGEX.containsMatchIn(content)
    }

    fun parseLrc(content: String): LyricsDocument {
        val rawLines = content.lines()
        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var offsetMs = 0L

        val lyricItems = mutableListOf<LyricLine>()

        for (line in rawLines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty()) continue

            val headerMatch = LRC_HEADER_REGEX.matchEntire(trimmedLine)
            if (headerMatch != null) {
                val tag = headerMatch.groupValues[1].lowercase()
                val value = headerMatch.groupValues[2].trim()
                when (tag) {
                    "ti" -> title = value
                    "ar" -> artist = value
                    "al" -> album = value
                    "offset" -> offsetMs = value.toLongOrNull() ?: 0L
                }
                continue
            }

            val matches = LRC_TIMESTAMP_REGEX.findAll(trimmedLine).toList()
            if (matches.isNotEmpty()) {
                val text = trimmedLine.replace(LRC_TIMESTAMP_REGEX, "").trim()
                for (match in matches) {
                    val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                    val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                    val fractionStr = match.groupValues[3]
                    val millis = when (fractionStr.length) {
                        1 -> (fractionStr.toIntOrNull() ?: 0) * 100L
                        2 -> (fractionStr.toIntOrNull() ?: 0) * 10L
                        3 -> fractionStr.toLongOrNull() ?: 0L
                        else -> 0L
                    }
                    val timestamp = (minutes * 60_000L) + (seconds * 1_000L) + millis + offsetMs
                    lyricItems.add(
                        LyricLine(
                            startTimeMs = timestamp.coerceAtLeast(0L),
                            text = text,
                        )
                    )
                }
            }
        }

        if (lyricItems.isEmpty()) {
            return parseFallback(content)
        }

        val sorted = lyricItems.sortedBy { it.startTimeMs }
        val withEndTimes = sorted.mapIndexed { index, item ->
            val nextTime = sorted.getOrNull(index + 1)?.startTimeMs
            item.copy(endTimeMs = nextTime)
        }

        return LyricsDocument(
            lines = withEndTimes,
            isSynced = true,
            title = title,
            artist = artist,
            album = album,
            offsetMs = offsetMs,
        )
    }

    fun parseSrt(content: String): LyricsDocument {
        val lines = content.lines()
        val lyricItems = mutableListOf<LyricLine>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.all { it.isDigit() }) {
                i++
                continue
            }

            val timeMatch = SRT_TIME_REGEX.matchEntire(line)
            if (timeMatch != null) {
                val startMs = parseSrtTime(
                    timeMatch.groupValues[1],
                    timeMatch.groupValues[2],
                    timeMatch.groupValues[3],
                    timeMatch.groupValues[4],
                )
                val endMs = parseSrtTime(
                    timeMatch.groupValues[5],
                    timeMatch.groupValues[6],
                    timeMatch.groupValues[7],
                    timeMatch.groupValues[8],
                )
                i++
                val textBuilder = StringBuilder()
                while (i < lines.size && lines[i].isNotBlank()) {
                    if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                    textBuilder.append(cleanFormattingTags(lines[i].trim()))
                    i++
                }
                val text = textBuilder.toString().trim()
                if (text.isNotEmpty()) {
                    lyricItems.add(
                        LyricLine(
                            startTimeMs = startMs,
                            endTimeMs = endMs,
                            text = text,
                        )
                    )
                }
            } else {
                i++
            }
        }

        if (lyricItems.isEmpty()) {
            return parseFallback(content)
        }

        return LyricsDocument(
            lines = lyricItems.sortedBy { it.startTimeMs },
            isSynced = true,
        )
    }

    fun parseVtt(content: String): LyricsDocument {
        val lines = content.lines()
        val lyricItems = mutableListOf<LyricLine>()
        var i = 0

        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.startsWith("WEBVTT", ignoreCase = true) || line.startsWith("NOTE", ignoreCase = true)) {
                i++
                continue
            }

            val timeMatch = VTT_TIME_REGEX.find(line)
            if (timeMatch != null) {
                val startHours = timeMatch.groupValues[1]
                val startMins = timeMatch.groupValues[2]
                val startSecs = timeMatch.groupValues[3]
                val startMillis = timeMatch.groupValues[4]

                val endHours = timeMatch.groupValues[5]
                val endMins = timeMatch.groupValues[6]
                val endSecs = timeMatch.groupValues[7]
                val endMillis = timeMatch.groupValues[8]

                val startMs = parseVttTime(startHours, startMins, startSecs, startMillis)
                val endMs = parseVttTime(endHours, endMins, endSecs, endMillis)

                i++
                val textBuilder = StringBuilder()
                while (i < lines.size && lines[i].isNotBlank()) {
                    if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                    textBuilder.append(cleanFormattingTags(lines[i].trim()))
                    i++
                }
                val text = textBuilder.toString().trim()
                if (text.isNotEmpty()) {
                    lyricItems.add(
                        LyricLine(
                            startTimeMs = startMs,
                            endTimeMs = endMs,
                            text = text,
                        )
                    )
                }
            } else {
                i++
            }
        }

        if (lyricItems.isEmpty()) {
            return parseFallback(content)
        }

        return LyricsDocument(
            lines = lyricItems.sortedBy { it.startTimeMs },
            isSynced = true,
        )
    }

    private fun parseFallback(content: String): LyricsDocument {
        val lines = content.lines()
            .map { cleanFormattingTags(it).trim() }
            .filter { it.isNotBlank() }
            .map { LyricLine(startTimeMs = 0L, text = it) }

        return LyricsDocument(
            lines = lines,
            isSynced = false,
        )
    }

    private fun cleanFormattingTags(text: String): String {
        return text.replace(HTML_TAG_REGEX, "").trim()
    }

    private fun parseSrtTime(hours: String, minutes: String, seconds: String, millis: String): Long {
        val h = hours.toLongOrNull() ?: 0L
        val m = minutes.toLongOrNull() ?: 0L
        val s = seconds.toLongOrNull() ?: 0L
        val ms = millis.padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        return (h * 3_600_000L) + (m * 60_000L) + (s * 1_000L) + ms
    }

    private fun parseVttTime(hours: String?, minutes: String, seconds: String, millis: String): Long {
        val h = hours?.takeIf { it.isNotBlank() }?.toLongOrNull() ?: 0L
        val m = minutes.toLongOrNull() ?: 0L
        val s = seconds.toLongOrNull() ?: 0L
        val ms = millis.padEnd(3, '0').take(3).toLongOrNull() ?: 0L
        return (h * 3_600_000L) + (m * 60_000L) + (s * 1_000L) + ms
    }
}
