package com.localdownloader.downloader

import android.content.Context
import com.localdownloader.domain.models.MediaFormat
import com.localdownloader.domain.models.PlaylistEntry
import com.localdownloader.domain.models.VideoInfo
import com.localdownloader.utils.Logger
import com.localdownloader.utils.SensitiveDataSanitizer
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.net.URI
import java.util.zip.CRC32
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FormatExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ytDlpExecutor: YtDlpExecutor,
    private val json: Json,
    private val logger: Logger,
) {
    suspend fun analyze(
        url: String,
        cookiesPath: String? = null,
        userAgent: String? = null,
        preferredExtractorArgs: String? = null,
    ): Result<VideoInfo> {
        return runCatching {
            logger.i("FormatExtractor", "Starting yt-dlp analyze for URL: $url")
            val analyzeMode = resolveAnalyzeRequestMode(url)

            val hasCookies = !cookiesPath.isNullOrBlank() && File(cookiesPath).exists()
            val extractorCandidates = if (isYoutubeUrl(url)) {
                YoutubeRequestPlanner.analyzeCandidates(cookiesAvailable = hasCookies)
            } else {
                listOf<String?>(null)
            }

            var best: AnalyzeCandidate? = null
            var lastFailureMessage: String? = null
            extractorCandidates.forEachIndexed { index, extractorArgs ->
                if (analyzeMode == AnalyzeRequestMode.YOUTUBE_PLAYLIST_FAST &&
                    best?.isPlaylistResult == true &&
                    (best?.playlistEntryCount ?: 0) > 0
                ) {
                    return@forEachIndexed
                }
                if (best != null && !shouldTryMoreCandidates(best?.stats)) return@forEachIndexed
                val attempt = runCatching {
                    analyzeWithExtractor(
                        url = url,
                        extractorArgs = extractorArgs,
                        cookiesPath = cookiesPath,
                        userAgent = userAgent,
                        requestMode = analyzeMode,
                    )
                }.getOrElse { error ->
                    logger.w(
                        "FormatExtractor",
                        "Analyze candidate[$index] crashed for args=${extractorArgs ?: "(default)"}",
                        error,
                    )
                    AnalyzeAttempt(
                        candidate = null,
                        errorMessage = sanitizeAnalyzeFailureMessage(
                            error.message?.takeIf { it.isNotBlank() } ?: "yt-dlp analyze failed",
                        ),
                    )
                }
                val candidate = attempt.candidate
                if (candidate != null) {
                    val stats = candidate.stats
                    val descriptor = extractorArgs ?: "(default)"
                    if (analyzeMode == AnalyzeRequestMode.YOUTUBE_PLAYLIST_FAST) {
                        logger.i(
                            "FormatExtractor",
                            "Analyze candidate[$index] args=$descriptor playlistEntries=${candidate.playlistEntryCount} playlist=${candidate.isPlaylistResult}",
                        )
                    } else {
                        logger.i(
                            "FormatExtractor",
                            "Analyze candidate[$index] args=$descriptor formats=${stats.total} videoOnly=${stats.videoOnly} audioOnly=${stats.audioOnly} maxHeight=${stats.maxHeight}",
                        )
                    }
                    if (analyzeMode == AnalyzeRequestMode.YOUTUBE_PLAYLIST_FAST) {
                        if (best == null || candidate.playlistEntryCount > (best?.playlistEntryCount ?: -1)) {
                            best = candidate
                        }
                        if (candidate.isPlaylistResult && candidate.playlistEntryCount > 0) return@forEachIndexed
                        return@forEachIndexed
                    }
                    if (best == null || stats.isBetterThan(best?.stats)) {
                        best = candidate
                    }
                    if (stats.hasAdaptiveVideo) return@forEachIndexed
                } else if (!attempt.errorMessage.isNullOrBlank()) {
                    lastFailureMessage = attempt.errorMessage
                }
            }

            val resolved = best ?: throw IllegalStateException(lastFailureMessage ?: "yt-dlp analyze failed")
            mapVideoInfo(
                root = resolved.root,
                fallbackUrl = url,
                extractorArgs = resolved.extractorArgs,
                infoJsonPath = resolved.infoJsonPath,
            ).also { info ->
                logger.i(
                    "FormatExtractor",
                    "Analyze parsed successfully title='${info.title}', formats=${info.formats.size}, extractorArgs=${resolved.extractorArgs}",
                )
                logFormatSummary(url = url, formats = info.formats, extractorArgs = resolved.extractorArgs)
            }
        }.onFailure { error ->
            logger.e("FormatExtractor", "Analyze exception for URL: $url", error)
        }
    }

    private fun logFormatSummary(url: String, formats: List<MediaFormat>, extractorArgs: String?) {
        if (!isYoutubeUrl(url)) return
        val videoOnly = formats.filter { it.isVideoOnly }
        val audioOnly = formats.filter { it.isAudioOnly }
        val muxed = formats.filter { !it.isVideoOnly && !it.isAudioOnly }
        val maxHeight = formats.mapNotNull { parseHeight(it.resolution) }.maxOrNull() ?: 0
        logger.i(
            "FormatExtractor",
            "YouTube formats summary extractorArgs=${extractorArgs ?: "(none)"} total=${formats.size} videoOnly=${videoOnly.size} audioOnly=${audioOnly.size} muxed=${muxed.size} maxHeight=${maxHeight}",
        )
        formats.take(12).forEachIndexed { index, format ->
            logger.i(
                "FormatExtractor",
                "Format[$index] id=${format.formatId} ext=${format.extension} res=${format.resolution} vcodec=${format.videoCodec} acodec=${format.audioCodec} fps=${format.fps ?: "-"} tbr=${format.bitrateKbps ?: "-"}",
            )
        }
    }

    private fun mapVideoInfo(
        root: JsonObject,
        fallbackUrl: String,
        extractorArgs: String?,
        infoJsonPath: String?,
    ): VideoInfo {
        val playlistEntries = parsePlaylistEntries(
            entries = root["entries"] as? JsonArray,
            playlistSourceUrl = fallbackUrl,
        )
        val rootFormats = parseFormats(root["formats"] as? JsonArray ?: JsonArray(emptyList()))
        val fallbackFormats = firstEntryWithFormats(root["entries"] as? JsonArray)
            ?.let(::parseFormats)
            .orEmpty()
        val formats = rootFormats.ifEmpty { fallbackFormats }
        val type = root["\u005ftype"]?.jsonPrimitive?.contentOrNull
        return VideoInfo(
            id = root["id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            title = root["title"]?.jsonPrimitive?.contentOrNull ?: "Untitled",
            uploader = root["uploader"]?.jsonPrimitive?.contentOrNull,
            durationSeconds = root["duration"]?.jsonPrimitive?.longOrNull,
            thumbnailUrl = root["thumbnail"]?.jsonPrimitive?.contentOrNull
                ?: playlistEntries.firstOrNull()?.thumbnailUrl,
            webpageUrl = root["webpage_url"]?.jsonPrimitive?.contentOrNull ?: fallbackUrl,
            formats = formats,
            extractorArgs = extractorArgs,
            infoJsonPath = infoJsonPath,
            isPlaylist = type == "playlist",
            playlistCount = root["playlist_count"]?.jsonPrimitive?.intOrNull ?: playlistEntries.size.takeIf { it > 0 },
            playlistEntries = playlistEntries,
        )
    }

    private fun parsePlaylistEntries(
        entries: JsonArray?,
        playlistSourceUrl: String,
    ): List<PlaylistEntry> {
        return entries.orEmpty().mapIndexedNotNull { index, element ->
            val item = element as? JsonObject ?: return@mapIndexedNotNull null
            val id = item["id"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val title = item["title"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val rawWebpageUrl = item["webpage_url"]?.jsonPrimitive?.contentOrNull
                ?: item["original_url"]?.jsonPrimitive?.contentOrNull
                ?: item["url"]?.jsonPrimitive?.contentOrNull
            val webpageUrl = resolvePlaylistEntryWebpageUrl(
                rawUrl = rawWebpageUrl,
                entryId = id,
                playlistSourceUrl = playlistSourceUrl,
            )
                ?: return@mapIndexedNotNull null
            if (title.isBlank() && id.isBlank()) return@mapIndexedNotNull null
            PlaylistEntry(
                playlistItemIndex = index + 1,
                id = id,
                title = title.ifBlank { "Item ${index + 1}" },
                webpageUrl = webpageUrl,
                uploader = item["uploader"]?.jsonPrimitive?.contentOrNull,
                durationSeconds = item["duration"]?.jsonPrimitive?.longOrNull,
                thumbnailUrl = item["thumbnail"]?.jsonPrimitive?.contentOrNull,
                formats = parseFormats(item["formats"] as? JsonArray ?: JsonArray(emptyList())),
            )
        }
    }

    private fun firstEntryWithFormats(entries: JsonArray?): JsonArray? {
        return entries.orEmpty()
            .asSequence()
            .mapNotNull { it as? JsonObject }
            .mapNotNull { it["formats"] as? JsonArray }
            .firstOrNull { it.isNotEmpty() }
    }

    private suspend fun analyzeWithExtractor(
        url: String,
        extractorArgs: String?,
        cookiesPath: String?,
        userAgent: String?,
        requestMode: AnalyzeRequestMode,
    ): AnalyzeAttempt {
        val capturedInfoJsonFile = when (requestMode) {
            AnalyzeRequestMode.STANDARD -> createAnalyzeCaptureFile(url = url, extractorArgs = extractorArgs)
            AnalyzeRequestMode.YOUTUBE_PLAYLIST_FAST -> null
        }
        val primaryAttempt = executeAnalyzeAttempt(
            url = url,
            extractorArgs = extractorArgs,
            cookiesPath = cookiesPath,
            userAgent = userAgent,
            capturedInfoJsonFile = capturedInfoJsonFile,
            socketTimeoutSeconds = DEFAULT_ANALYZE_SOCKET_TIMEOUT_SECONDS,
            useLineJsonMode = false,
            requestMode = requestMode,
        )
        if (primaryAttempt.candidate != null) {
            return primaryAttempt
        }

        if (requestMode == AnalyzeRequestMode.YOUTUBE_PLAYLIST_FAST) {
            return primaryAttempt
        }

        if (!isYoutubeUrl(url)) {
            val lineJsonAttempt = executeAnalyzeAttempt(
                url = url,
                extractorArgs = extractorArgs,
                cookiesPath = cookiesPath,
                userAgent = userAgent,
                capturedInfoJsonFile = capturedInfoJsonFile,
                socketTimeoutSeconds = DEFAULT_ANALYZE_SOCKET_TIMEOUT_SECONDS,
                useLineJsonMode = true,
                requestMode = requestMode,
            )
            if (lineJsonAttempt.candidate != null) {
                return lineJsonAttempt
            }

            if (!shouldRetryAnalyzeWithExtendedTimeout(primaryAttempt.errorMessage, lineJsonAttempt.errorMessage)) {
                return resolveFailedAnalyzeAttempt(primaryAttempt, lineJsonAttempt)
            }

            logger.i(
                "FormatExtractor",
                "Retrying non-YouTube analyze with a longer socket timeout after transient failure",
            )

            val extendedPrimaryAttempt = executeAnalyzeAttempt(
                url = url,
                extractorArgs = extractorArgs,
                cookiesPath = cookiesPath,
                userAgent = userAgent,
                capturedInfoJsonFile = capturedInfoJsonFile,
                socketTimeoutSeconds = EXTENDED_ANALYZE_SOCKET_TIMEOUT_SECONDS,
                useLineJsonMode = false,
                requestMode = requestMode,
            )
            if (extendedPrimaryAttempt.candidate != null) {
                return extendedPrimaryAttempt
            }

            val extendedLineJsonAttempt = executeAnalyzeAttempt(
                url = url,
                extractorArgs = extractorArgs,
                cookiesPath = cookiesPath,
                userAgent = userAgent,
                capturedInfoJsonFile = capturedInfoJsonFile,
                socketTimeoutSeconds = EXTENDED_ANALYZE_SOCKET_TIMEOUT_SECONDS,
                useLineJsonMode = true,
                requestMode = requestMode,
            )
            if (extendedLineJsonAttempt.candidate != null) {
                return extendedLineJsonAttempt
            }

            return resolveFailedAnalyzeAttempt(
                primaryAttempt,
                lineJsonAttempt,
                extendedPrimaryAttempt,
                extendedLineJsonAttempt,
            )
        }

        return primaryAttempt
    }

    private suspend fun executeAnalyzeAttempt(
        url: String,
        extractorArgs: String?,
        cookiesPath: String?,
        userAgent: String?,
        capturedInfoJsonFile: File?,
        socketTimeoutSeconds: Int,
        useLineJsonMode: Boolean,
        requestMode: AnalyzeRequestMode,
    ): AnalyzeAttempt {
        val args = buildAnalyzeArgs(
            url = url,
            extractorArgs = extractorArgs,
            cookiesPath = cookiesPath,
            userAgent = userAgent,
            capturedInfoJsonFile = capturedInfoJsonFile,
            socketTimeoutSeconds = socketTimeoutSeconds,
            useLineJsonMode = useLineJsonMode,
            requestMode = requestMode,
        )
        val processTimeoutMs = analyzeProcessTimeoutMillis(socketTimeoutSeconds)

        capturedInfoJsonFile?.delete()
        val commandOutcome = runCatching { executeAnalyzeCommand(args, processTimeoutMs) }
        val result = commandOutcome.getOrNull()
        val commandError = commandOutcome.exceptionOrNull()
        if (result != null) {
            logger.i(
                "FormatExtractor",
                "Analyze command finished exitCode=${result.exitCode}, stdoutLen=${result.stdout.length}, stderrLen=${result.stderr.length}, socketTimeout=${socketTimeoutSeconds}s, lineJson=$useLineJsonMode",
            )
        } else {
            logger.w(
                "FormatExtractor",
                "Analyze command threw before returning a result, socketTimeout=${socketTimeoutSeconds}s, lineJson=$useLineJsonMode",
                commandError,
            )
        }

        val parsedAttempt = parseAnalyzeSuccess(
            url = url,
            extractorArgs = extractorArgs,
            stdout = result?.stdout.orEmpty(),
            capturedInfoJsonPath = capturedInfoJsonFile?.absolutePath,
        )
        if (parsedAttempt.candidate != null) {
            return parsedAttempt
        }

        if (commandError != null) {
            return AnalyzeAttempt(
                candidate = null,
                errorMessage = sanitizeAnalyzeFailureMessage(
                    commandError.message?.takeIf { it.isNotBlank() } ?: "yt-dlp analyze failed",
                ),
            )
        }

        val resolvedResult = result ?: return AnalyzeAttempt(
            candidate = null,
            errorMessage = "yt-dlp analyze failed",
        )

        if (!resolvedResult.isSuccess) {
            val failureMessage = extractAnalyzeFailureMessage(resolvedResult.stderr, resolvedResult.stdout)
            if (looksLikeClosedStreamFailure(failureMessage)) {
                logger.w("FormatExtractor", "Analyze returned transient closed-stream failure; retrying once")
                capturedInfoJsonFile?.delete()
                val retryResult = executeAnalyzeCommand(args, processTimeoutMs)
                logger.i(
                    "FormatExtractor",
                    "Analyze retry finished exitCode=${retryResult.exitCode}, stdoutLen=${retryResult.stdout.length}, stderrLen=${retryResult.stderr.length}, socketTimeout=${socketTimeoutSeconds}s, lineJson=$useLineJsonMode",
                )
                val parsedRetry = parseAnalyzeSuccess(
                    url = url,
                    extractorArgs = extractorArgs,
                    stdout = retryResult.stdout,
                    capturedInfoJsonPath = capturedInfoJsonFile?.absolutePath,
                )
                if (parsedRetry.candidate != null) {
                    return parsedRetry
                }
                logger.w("FormatExtractor", "Analyze retry failed stderr=${retryResult.stderr.take(1000)}")
                return AnalyzeAttempt(
                    candidate = null,
                    errorMessage = sanitizeAnalyzeFailureMessage(
                        extractAnalyzeFailureMessage(retryResult.stderr, retryResult.stdout),
                    ),
                )
            }
            logger.w("FormatExtractor", "Analyze failed stderr=${resolvedResult.stderr.take(1000)}")
            return AnalyzeAttempt(
                candidate = null,
                errorMessage = sanitizeAnalyzeFailureMessage(failureMessage),
            )
        }

        return parsedAttempt
    }

    private fun buildAnalyzeArgs(
        url: String,
        extractorArgs: String?,
        cookiesPath: String?,
        userAgent: String?,
        capturedInfoJsonFile: File?,
        socketTimeoutSeconds: Int,
        useLineJsonMode: Boolean,
        requestMode: AnalyzeRequestMode,
    ): List<String> {
        val tempDir = File(context.cacheDir, "tmp").apply { mkdirs() }
        return buildAnalyzeArgsForRequest(
            url = url,
            extractorArgs = extractorArgs,
            cookiesPath = cookiesPath,
            userAgent = userAgent,
            capturedInfoJsonPath = capturedInfoJsonFile?.absolutePath,
            tempDirPath = tempDir.absolutePath,
            socketTimeoutSeconds = socketTimeoutSeconds,
            useLineJsonMode = useLineJsonMode,
            requestMode = requestMode,
        )
    }

    private fun extractAnalyzeFailureMessage(stderr: String, stdout: String): String {
        return preferredAnalyzeFailureLine(stderr)
            ?: preferredAnalyzeFailureLine(stdout)
            ?: "yt-dlp analyze failed"
    }

    private suspend fun executeAnalyzeCommand(
        args: List<String>,
        timeoutMs: Long,
    ): CommandResult {
        return runCatching {
            ytDlpExecutor.execute(
                args = args,
                logOutputLines = false,
                timeoutMs = timeoutMs,
            )
        }.recoverCatching { error ->
            if (error.matchesClosedStreamFailure()) {
                logger.w("FormatExtractor", "Analyze command hit closed-stream race; retrying once", error)
                ytDlpExecutor.execute(
                    args = args,
                    logOutputLines = false,
                    timeoutMs = timeoutMs,
                )
            } else {
                throw error
            }
        }.getOrThrow()
    }

    private fun parseAnalyzeSuccess(
        url: String,
        extractorArgs: String?,
        stdout: String,
        capturedInfoJsonPath: String?,
    ): AnalyzeAttempt {
        var lastError: Throwable? = null
        fun tryParseSource(rawJson: String, sourceLabel: String): AnalyzeAttempt? {
            val parsed = runCatching {
                val root = json.parseToJsonElement(rawJson).jsonObject
                val formats = parseFormats(root["formats"] as? JsonArray ?: JsonArray(emptyList()))
                val playlistEntries = root["entries"] as? JsonArray
                val playlistEntryCount = playlistEntries?.size ?: 0
                val type = root["_type"]?.jsonPrimitive?.contentOrNull
                val infoJsonPath = persistInfoJsonSnapshot(
                    url = url,
                    extractorArgs = extractorArgs,
                    root = root,
                    rawJson = rawJson,
                )
                AnalyzeAttempt(
                    candidate = AnalyzeCandidate(
                        root = root,
                        formats = formats,
                        extractorArgs = extractorArgs,
                        infoJsonPath = infoJsonPath,
                        stats = FormatStats.from(formats),
                        isPlaylistResult = type == "playlist" || playlistEntryCount > 0,
                        playlistEntryCount = playlistEntryCount,
                    ),
                    errorMessage = null,
                )
            }
            parsed.getOrNull()?.let { return it }
            lastError = parsed.exceptionOrNull()
            logger.w("FormatExtractor", "Analyze JSON parse failed for $sourceLabel", lastError)
            return null
        }

        val capturedJson = readCapturedAnalyzeJson(capturedInfoJsonPath)
        if (!capturedJson.isNullOrBlank()) {
            tryParseSource(capturedJson, "captured info-json")?.let { return it }
        }

        val normalizedStdout = stdout.trim().takeIf { it.isNotBlank() }
        if (!normalizedStdout.isNullOrBlank() && normalizedStdout != capturedJson) {
            tryParseSource(normalizedStdout, "stdout fallback")?.let { return it }
        }

        return AnalyzeAttempt(
            candidate = null,
            errorMessage = sanitizeAnalyzeFailureMessage(
                lastError?.message?.takeIf { it.isNotBlank() } ?: "yt-dlp returned unreadable analyze output",
            ),
        )
    }

    private fun createAnalyzeCaptureFile(url: String, extractorArgs: String?): File {
        val key = buildString {
            append(url)
            append('|')
            append(extractorArgs.orEmpty())
        }
        val hash = CRC32().apply { update(key.toByteArray()) }.value.toString(16)
        val directory = File(context.cacheDir, "ytdlp-analyze").apply { mkdirs() }
        return File(directory, "$hash-${System.currentTimeMillis()}.info.json")
    }

    private fun readCapturedAnalyzeJson(path: String?): String? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists()) return null
        return runCatching { file.readText() }
            .getOrElse { error ->
                logger.w("FormatExtractor", "Unable to read captured analyze info-json", error)
                null
            }
            ?.trim()
            ?.ifBlank { null }
    }

    private fun isYoutubeUrl(url: String): Boolean {
        return looksLikeYoutubeUrl(url)
    }

    private fun resolveAnalyzeRequestMode(url: String): AnalyzeRequestMode {
        return if (shouldUseFastPlaylistAnalyze(url)) {
            AnalyzeRequestMode.YOUTUBE_PLAYLIST_FAST
        } else {
            AnalyzeRequestMode.STANDARD
        }
    }

    private fun resolvePlaylistEntryWebpageUrl(
        rawUrl: String?,
        entryId: String,
        playlistSourceUrl: String,
    ): String? {
        val candidate = rawUrl?.trim().orEmpty()
        if (candidate.startsWith("https://") || candidate.startsWith("http://")) {
            return candidate
        }
        resolveRelativePlaylistEntryUrl(
            playlistSourceUrl = playlistSourceUrl,
            candidate = candidate,
        )?.let { return it }
        if (looksLikeYoutubeUrl(playlistSourceUrl)) {
            if (candidate.startsWith("/")) {
                return "https://www.youtube.com$candidate"
            }
            if (candidate.startsWith("watch?") || candidate.startsWith("shorts/")) {
                return "https://www.youtube.com/$candidate"
            }
            val videoId = entryId.ifBlank { candidate }.trim()
            if (videoId.isNotBlank()) {
                val playlistId = extractYoutubePlaylistId(playlistSourceUrl)
                return buildString {
                    append("https://www.youtube.com/watch?v=")
                    append(videoId)
                    playlistId?.let {
                        append("&list=")
                        append(it)
                    }
                }
            }
        }
        return candidate.ifBlank { null }
    }

    private fun resolveRelativePlaylistEntryUrl(
        playlistSourceUrl: String,
        candidate: String,
    ): String? {
        if (candidate.isBlank()) return null
        if (candidate.startsWith("//")) {
            val scheme = if (playlistSourceUrl.startsWith("http://")) "http" else "https"
            return "$scheme:$candidate"
        }
        if (looksLikeYoutubeUrl(playlistSourceUrl) &&
            !candidate.startsWith("/") &&
            !candidate.contains('/') &&
            !candidate.contains('?')
        ) {
            return null
        }
        return runCatching {
            URI(playlistSourceUrl).resolve(candidate).toString()
        }.getOrNull()?.takeIf { resolved ->
            resolved.startsWith("https://") || resolved.startsWith("http://")
        }
    }

    private fun persistInfoJsonSnapshot(
        url: String,
        extractorArgs: String?,
        root: JsonObject,
        rawJson: String,
    ): String? {
        val type = root["_type"]?.jsonPrimitive?.contentOrNull
        if (type == "playlist") return null

        val file = persistedInfoJsonFile(
            url = url,
            extractorArgs = extractorArgs,
        )
        return runCatching {
            file.writeText(rawJson)
            file.absolutePath
        }.getOrElse { error ->
            logger.w("FormatExtractor", "Unable to persist analyze info-json snapshot", error)
            null
        }
    }

    private fun persistedInfoJsonFile(
        url: String,
        extractorArgs: String?,
    ): File {
        val hash = CRC32().apply {
            update(url.toByteArray())
            update('|'.code)
            update(extractorArgs.orEmpty().toByteArray())
        }.value.toString(16)
        val directory = File(context.cacheDir, "ytdlp-info").apply { mkdirs() }
        return File(directory, "$hash-video.info.json")
    }

    private fun preferredAnalyzeFailureLine(output: String): String? {
        val lines = output.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { it.removePrefix("ERROR: ") }
            .toList()
        return lines.firstOrNull { !looksLikeClosedStreamFailure(it) } ?: lines.firstOrNull()
    }

    private fun sanitizeAnalyzeFailureMessage(message: String): String {
        val normalized = SensitiveDataSanitizer.sanitize(message.trim())
        return if (looksLikeClosedStreamFailure(normalized)) {
            "Link analysis was interrupted before yt-dlp returned a usable result. Please try again."
        } else {
            normalized.take(240)
        }
    }

    private fun Throwable.matchesClosedStreamFailure(): Boolean {
        return generateSequence(this) { it.cause }
            .map { it.message.orEmpty() }
            .any(::looksLikeClosedStreamFailure)
    }

    private fun looksLikeClosedStreamFailure(message: String): Boolean {
        val detail = message.lowercase()
        return detail.contains("i/o operation on closed file") ||
            detail.contains("operation on closed file") ||
            detail.contains("stream closed") ||
            detail.contains("interrupted by close") ||
            detail == "closed"
    }

    private fun parseFormats(elements: JsonArray): List<MediaFormat> {
        return elements.mapNotNull { element ->
            parseFormatObject(element)
        }.sortedWith(
            compareByDescending<MediaFormat> { it.resolution?.substringBefore("p")?.toIntOrNull() ?: 0 }
                .thenByDescending { it.bitrateKbps ?: 0 },
        )
    }

    private fun parseFormatObject(element: JsonElement): MediaFormat? {
        val item = element.jsonObject
        if (shouldIgnoreFormat(item)) return null
        val formatId = item["format_id"]?.jsonPrimitive?.contentOrNull ?: return null
        val ext = item["ext"]?.jsonPrimitive?.contentOrNull ?: "bin"
        val height = item["height"]?.jsonPrimitive?.intOrNull
        val width = item["width"]?.jsonPrimitive?.intOrNull
        val resolutionText = item["resolution"]?.jsonPrimitive?.contentOrNull
        val resolution = when {
            height != null -> "${height}p"
            resolutionText != null -> resolutionText
            width != null -> "${width}w"
            else -> null
        }

        val approximateSize = item["filesize"]?.jsonPrimitive?.longOrNull
            ?: item["filesize_approx"]?.jsonPrimitive?.longOrNull

        return MediaFormat(
            formatId = formatId,
            extension = ext,
            container = ext,
            resolution = resolution,
            videoCodec = item["vcodec"]?.jsonPrimitive?.contentOrNull ?: "none",
            audioCodec = item["acodec"]?.jsonPrimitive?.contentOrNull ?: "none",
            fileSizeBytes = approximateSize,
            bitrateKbps = parseFormatBitrateKbps(item),
            fps = item["fps"]?.jsonPrimitive?.doubleOrNull,
            note = item["format_note"]?.jsonPrimitive?.contentOrNull,
        )
    }

    private fun parseHeight(resolution: String?): Int? {
        val trimmed = resolution ?: return null
        return trimmed.substringBefore("p", trimmed).toIntOrNull()
    }

    private data class AnalyzeCandidate(
        val root: JsonObject,
        val formats: List<MediaFormat>,
        val extractorArgs: String?,
        val infoJsonPath: String?,
        val stats: FormatStats,
        val isPlaylistResult: Boolean,
        val playlistEntryCount: Int,
    )

    private data class AnalyzeAttempt(
        val candidate: AnalyzeCandidate?,
        val errorMessage: String?,
    )

    private data class FormatStats(
        val total: Int,
        val videoOnly: Int,
        val audioOnly: Int,
        val maxHeight: Int,
        val hasAdaptiveVideo: Boolean,
    ) {
        fun isBetterThan(other: FormatStats?): Boolean {
            if (other == null) return true
            if (total != other.total) return total > other.total
            if (videoOnly != other.videoOnly) return videoOnly > other.videoOnly
            if (maxHeight != other.maxHeight) return maxHeight > other.maxHeight
            return audioOnly > other.audioOnly
        }

        companion object {
            fun from(formats: List<MediaFormat>): FormatStats {
                val videoOnly = formats.count { it.isVideoOnly }
                val audioOnly = formats.count { it.isAudioOnly }
                val maxHeight = formats.mapNotNull { parseHeight(it.resolution) }.maxOrNull() ?: 0
                val hasAdaptiveVideo = videoOnly > 0 && maxHeight >= 720
                return FormatStats(
                    total = formats.size,
                    videoOnly = videoOnly,
                    audioOnly = audioOnly,
                    maxHeight = maxHeight,
                    hasAdaptiveVideo = hasAdaptiveVideo,
                )
            }

            private fun parseHeight(resolution: String?): Int? {
                val trimmed = resolution ?: return null
                return trimmed.substringBefore("p", trimmed).toIntOrNull()
            }
        }
    }

    private fun shouldTryMoreCandidates(stats: FormatStats?): Boolean {
        if (stats == null) return true
        return shouldContinueAnalyzeCandidateSearch(
            totalFormats = stats.total,
            videoOnlyFormats = stats.videoOnly,
            maxHeight = stats.maxHeight,
        )
    }

    private fun resolveFailedAnalyzeAttempt(vararg attempts: AnalyzeAttempt): AnalyzeAttempt {
        return attempts
            .lastOrNull { !it.errorMessage.isNullOrBlank() }
            ?: attempts.last()
    }
}

internal const val DEFAULT_ANALYZE_SOCKET_TIMEOUT_SECONDS = 5
internal const val EXTENDED_ANALYZE_SOCKET_TIMEOUT_SECONDS = 15
internal const val DEFAULT_ANALYZE_PROCESS_TIMEOUT_MILLIS = 20_000L
internal const val EXTENDED_ANALYZE_PROCESS_TIMEOUT_MILLIS = 40_000L

internal fun analyzeProcessTimeoutMillis(socketTimeoutSeconds: Int): Long {
    return if (socketTimeoutSeconds >= EXTENDED_ANALYZE_SOCKET_TIMEOUT_SECONDS) {
        EXTENDED_ANALYZE_PROCESS_TIMEOUT_MILLIS
    } else {
        DEFAULT_ANALYZE_PROCESS_TIMEOUT_MILLIS
    }
}

internal fun shouldContinueAnalyzeCandidateSearch(
    totalFormats: Int,
    videoOnlyFormats: Int,
    maxHeight: Int,
): Boolean {
    if (videoOnlyFormats > 0 && maxHeight >= 720) return false
    if (totalFormats >= 20 && maxHeight >= 720) return false
    return true
}

internal fun shouldRetryAnalyzeWithExtendedTimeout(vararg failureMessages: String?): Boolean {
    return failureMessages
        .mapNotNull { it?.takeIf(String::isNotBlank) }
        .any(::isTransientAnalyzeFailure)
}

internal fun isTransientAnalyzeFailure(message: String): Boolean {
    val lower = message.lowercase()
    return lower.contains("read operation timed out") ||
        lower.contains("read timed out") ||
        lower.contains("timed out") ||
        lower.contains("i/o timeout") ||
        lower.contains("connection timed out") ||
        lower.contains("temporary failure in name resolution") ||
        lower.contains("no address associated with hostname") ||
        lower.contains("failed to resolve") ||
        lower.contains("network is unreachable") ||
        lower.contains("connection reset") ||
        lower.contains("connection aborted") ||
        lower.contains("transport endpoint is not connected")
}

internal fun parseFormatBitrateKbps(item: JsonObject): Int? {
    return item["tbr"]?.jsonPrimitive?.doubleOrNull?.toInt()
        ?: item["abr"]?.jsonPrimitive?.doubleOrNull?.toInt()
}

internal fun shouldIgnoreFormat(item: JsonObject): Boolean {
    val formatId = item["format_id"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase().orEmpty()
    val note = item["format_note"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase().orEmpty()
    val extension = item["ext"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase().orEmpty()
    val videoCodec = item["vcodec"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase().orEmpty()
    val audioCodec = item["acodec"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase().orEmpty()
    if (formatId.startsWith("sb")) return true
    if (note.contains("storyboard")) return true
    if (extension == "mhtml" || extension == "mht") return true

    val hasDeclaredCodec = (videoCodec.isNotBlank() && videoCodec != "none") ||
        (audioCodec.isNotBlank() && audioCodec != "none")
    if (hasDeclaredCodec) return false

    val resolution = item["resolution"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase().orEmpty()
    val hasDimensions = item["height"]?.jsonPrimitive?.intOrNull != null ||
        item["width"]?.jsonPrimitive?.intOrNull != null ||
        resolution.endsWith("p") ||
        resolution.contains('x')
    val looksLikeAudioFormat = extension in KNOWN_AUDIO_ONLY_FORMAT_EXTENSIONS || note.contains("audio")
    val hasBitrateHint = parseFormatBitrateKbps(item) != null ||
        item["asr"]?.jsonPrimitive?.intOrNull != null
    return !hasDimensions && !looksLikeAudioFormat && !hasBitrateHint
}

private val KNOWN_AUDIO_ONLY_FORMAT_EXTENSIONS = setOf(
    "m4a",
    "mp3",
    "aac",
    "opus",
    "ogg",
    "oga",
    "flac",
    "wav",
    "amr",
    "mka",
    "weba",
)

internal fun buildAnalyzeArgsForRequest(
    url: String,
    extractorArgs: String?,
    cookiesPath: String?,
    userAgent: String?,
    capturedInfoJsonPath: String?,
    loadInfoJsonPath: String? = null,
    tempDirPath: String,
    socketTimeoutSeconds: Int = DEFAULT_ANALYZE_SOCKET_TIMEOUT_SECONDS,
    useLineJsonMode: Boolean,
    requestMode: AnalyzeRequestMode = AnalyzeRequestMode.STANDARD,
): List<String> {
    return buildList {
        add(if (useLineJsonMode) "-j" else "-J")
        add("--skip-download")
        add("--no-warnings")
        add("--ignore-config")
        add("--ignore-errors")
        add("--no-clean-info-json")
        if (!capturedInfoJsonPath.isNullOrBlank()) {
            add("--print-to-file")
            add("video:%()j")
            add(capturedInfoJsonPath)
        }
        if (!loadInfoJsonPath.isNullOrBlank() && File(loadInfoJsonPath).exists()) {
            add("--load-info-json")
            add(loadInfoJsonPath)
        }
        add("-R")
        add("1")
        add("--compat-options")
        add("manifest-filesize-approx")
        add("--socket-timeout")
        add(socketTimeoutSeconds.toString())
        add("-P")
        add(tempDirPath)
        when (requestMode) {
            AnalyzeRequestMode.STANDARD -> {
                if (shouldForceNoPlaylistAnalyze(url)) {
                    add("--no-playlist")
                }
            }
            AnalyzeRequestMode.YOUTUBE_PLAYLIST_FAST -> {
                add("--flat-playlist")
                add("--lazy-playlist")
            }
        }
        if (!cookiesPath.isNullOrBlank() && File(cookiesPath).exists()) {
            add("--cookies")
            add(cookiesPath)
        }
        if (!userAgent.isNullOrBlank()) {
            add("--add-header")
            add("User-Agent:$userAgent")
        }
        if (!extractorArgs.isNullOrBlank()) {
            add("--extractor-args")
            add(extractorArgs)
        }
        add(url)
    }
}

internal enum class AnalyzeRequestMode {
    STANDARD,
    YOUTUBE_PLAYLIST_FAST,
}

internal fun looksLikeYoutubeUrl(url: String): Boolean {
    val normalized = url.lowercase()
    return normalized.contains("youtube.com") || normalized.contains("youtu.be")
}

internal fun isExplicitYoutubeVideoUrl(url: String): Boolean {
    if (!looksLikeYoutubeUrl(url)) return false

    val normalized = url.trim()
    return runCatching {
        val uri = URI(normalized)
        val host = uri.host?.lowercase().orEmpty()
        val path = uri.path.orEmpty().lowercase()
        val query = uri.rawQuery.orEmpty().lowercase()
        val hasVideoQueryParam = query.split('&').any { part ->
            part.startsWith("v=") && part.length > 2
        }

        when {
            hasVideoQueryParam -> true
            host == "youtu.be" || host.endsWith(".youtu.be") -> path.trim('/').isNotBlank()
            path.startsWith("/shorts/") -> true
            path.startsWith("/live/") -> true
            path.startsWith("/embed/") -> true
            else -> false
        }
    }.getOrElse {
        val fallback = normalized.lowercase()
        fallback.contains("watch?v=") ||
            fallback.contains("youtu.be/") ||
            fallback.contains("/shorts/") ||
            fallback.contains("/live/") ||
            fallback.contains("/embed/")
    }
}

internal fun isLikelyYoutubePlaylistUrl(url: String): Boolean {
    if (!looksLikeYoutubeUrl(url) || isExplicitYoutubeVideoUrl(url)) return false

    val normalized = url.trim()
    return runCatching {
        val uri = URI(normalized)
        val path = uri.path.orEmpty().trimEnd('/').lowercase()
        val queryParts = uri.rawQuery.orEmpty()
            .split('&')
            .mapNotNull { part ->
                val key = part.substringBefore('=').trim().lowercase()
                if (key.isBlank()) null else key
            }
            .toSet()

        path == "/playlist" || "list" in queryParts
    }.getOrElse {
        val fallback = normalized.lowercase()
        fallback.contains("/playlist") || fallback.contains("list=")
    }
}

internal fun shouldUseFastPlaylistAnalyze(url: String): Boolean {
    val normalized = url.trim().lowercase()
    if (normalized.isBlank()) return false
    if (normalized.contains("soundcloud.com")) return false
    if (looksLikeYoutubeUrl(url)) return isLikelyYoutubePlaylistUrl(url)
    return PLAYLIST_ANALYZE_HINTS.any { hint -> normalized.contains(hint) }
}

internal fun shouldForceNoPlaylistAnalyze(url: String): Boolean {
    return isExplicitYoutubeVideoUrl(url)
}

internal fun extractYoutubePlaylistId(url: String): String? {
    val listSegment = url.substringAfter("list=", missingDelimiterValue = "").trim()
    if (listSegment.isBlank()) return null
    return listSegment.substringBefore('&').ifBlank { null }
}

private val PLAYLIST_ANALYZE_HINTS = listOf(
    "list=",
    "playlist=",
    "album=",
    "collection=",
    "/playlist",
    "/album/",
    "/albums/",
    "/mix/",
    "/set/",
    "/sets/",
    "/collection/",
    "/collections/",
    "/featured/",
)


