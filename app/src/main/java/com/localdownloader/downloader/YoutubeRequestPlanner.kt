package com.localdownloader.downloader

object YoutubeRequestPlanner {
    data class Attempt(
        val label: String,
        val selector: String,
        val extractorArgs: String?,
    )

    fun analyzeCandidates(cookiesAvailable: Boolean): List<String?> {
        val candidates = buildList {
            // Prefer explicit YouTube client routing first; the generic default path can stall.
            add(buildExtractorArgs(clientSpec = "default,android", includePlayerSkip = true))
            add(buildExtractorArgs(clientSpec = "tv,android", includePlayerSkip = true))
            if (cookiesAvailable) {
                add(buildExtractorArgs(clientSpec = "default,mweb,web,android,tv", includePlayerSkip = true))
                add(buildExtractorArgs(clientSpec = "default,mweb", includePlayerSkip = true))
                add(buildExtractorArgs(clientSpec = "default,web", includePlayerSkip = true))
            }
            add(null)
        }
        return candidates.distinct()
    }

    fun recoveryCandidates(selectedExtractorArgs: String?, cookiesAvailable: Boolean): List<String?> {
        val normalizedSelectedArgs = normalizeExtractorArgs(selectedExtractorArgs)
        return analyzeCandidates(cookiesAvailable)
            .map(::normalizeExtractorArgs)
            .distinct()
            .filter { it != normalizedSelectedArgs }
    }

    fun authenticatedSameSelectorAttempts(
        requestedSelector: String,
        poToken: String?,
        preferredHint: String,
        dataSyncId: String? = null,
    ): List<Attempt> {
        return prioritizeByHint(
            attempts = buildAuthenticatedSameSelectorAttempts(
                requestedSelector = requestedSelector,
                poToken = poToken,
                dataSyncId = dataSyncId,
            ),
            preferredHint = preferredHint,
        )
    }

    fun authenticatedAttempts(
        requestedSelector: String,
        poToken: String?,
        preferredHint: String,
        dataSyncId: String? = null,
    ): List<Attempt> {
        val normalizedRequestedSelector = requestedSelector.trim().ifBlank { "bestvideo*+bestaudio/best" }
        val attempts = buildAuthenticatedSameSelectorAttempts(
            requestedSelector = normalizedRequestedSelector,
            poToken = poToken,
            dataSyncId = dataSyncId,
        ) + listOf(
            Attempt(
                label = "mweb authenticated best",
                selector = "bestvideo*+bestaudio/best",
                extractorArgs = buildExtractorArgs(
                    clientSpec = "default,mweb",
                    includePlayerSkip = true,
                    poToken = poToken,
                    dataSyncId = dataSyncId,
                ),
            ),
            Attempt(
                label = "web authenticated best",
                selector = "bestvideo*+bestaudio/best",
                extractorArgs = buildExtractorArgs(
                    clientSpec = "default,web",
                    includePlayerSkip = true,
                    poToken = poToken,
                    dataSyncId = dataSyncId,
                ),
            ),
            Attempt(
                label = "android authenticated best",
                selector = "bestvideo*+bestaudio/best",
                extractorArgs = buildExtractorArgs(
                    clientSpec = "default,android",
                    poToken = poToken,
                    dataSyncId = dataSyncId,
                ),
            ),
            Attempt(
                label = "combined authenticated best",
                selector = "bestvideo*+bestaudio/best",
                extractorArgs = buildExtractorArgs(
                    clientSpec = "default,mweb,web,android,tv",
                    includePlayerSkip = true,
                    poToken = poToken,
                    dataSyncId = dataSyncId,
                ),
            ),
            Attempt(
                label = "mweb authenticated muxed safe mode",
                selector = "22/18/best[height<=720][vcodec!=none][acodec!=none][ext=mp4]/best[height<=480][vcodec!=none][acodec!=none]/best",
                extractorArgs = buildExtractorArgs(
                    clientSpec = "default,mweb",
                    includePlayerSkip = true,
                    poToken = poToken,
                    dataSyncId = dataSyncId,
                ),
            ),
        )
        return prioritizeByHint(attempts, preferredHint)
    }

    fun progressiveRecoveryAttempts(
        poToken: String?,
        preferredHint: String,
        dataSyncId: String? = null,
    ): List<Attempt> {
        val attempts = listOf(
            Attempt(
                label = "combined progressive safe mode",
                selector = "22/18/best[ext=mp4][vcodec!=none][acodec!=none]/best[height<=480][vcodec!=none][acodec!=none]/best",
                extractorArgs = buildExtractorArgs(
                    clientSpec = "default,mweb,web,android,tv",
                    includePlayerSkip = true,
                    poToken = poToken,
                    dataSyncId = dataSyncId,
                ),
            ),
            Attempt(
                label = "mweb progressive safe mode",
                selector = "22/18/best[ext=mp4][vcodec!=none][acodec!=none]/best[height<=480][vcodec!=none][acodec!=none]/best",
                extractorArgs = buildExtractorArgs(
                    clientSpec = "default,mweb",
                    includePlayerSkip = true,
                    poToken = poToken,
                    dataSyncId = dataSyncId,
                ),
            ),
            Attempt(
                label = "web progressive safe mode",
                selector = "22/18/best[ext=mp4][vcodec!=none][acodec!=none]/best[height<=480][vcodec!=none][acodec!=none]/best",
                extractorArgs = buildExtractorArgs(
                    clientSpec = "default,web",
                    includePlayerSkip = true,
                    poToken = poToken,
                    dataSyncId = dataSyncId,
                ),
            ),
            Attempt(
                label = "android progressive safe mode",
                selector = "22/18/best[ext=mp4][vcodec!=none][acodec!=none]/best[height<=480][vcodec!=none][acodec!=none]/best",
                extractorArgs = buildExtractorArgs(
                    clientSpec = "default,android",
                    poToken = poToken,
                    dataSyncId = dataSyncId,
                ),
            ),
        )
        return prioritizeByHint(attempts, preferredHint)
    }

    fun preferredAuthenticatedExtractorArgs(
        poToken: String?,
        preferredHint: String,
        dataSyncId: String? = null,
    ): String? {
        if (poToken.isNullOrBlank() && dataSyncId.isNullOrBlank()) return null
        return buildExtractorArgs(
            clientSpec = preferredClientSpecForHint(preferredHint),
            includePlayerSkip = preferredHint.trim().lowercase() != "android.gvs",
            poToken = poToken,
            dataSyncId = dataSyncId,
        )
    }

    fun buildAdaptiveSelector(
        maxHeight: Int?,
        container: String?,
        videoOnly: Boolean,
    ): String {
        val heightFilter = maxHeight?.let { "[height<=$it]" }.orEmpty()
        val normalizedContainer = container?.trim()?.lowercase()
        val videoExtFilter = when (normalizedContainer) {
            "mp4", "mov" -> "[ext=mp4]"
            "webm" -> "[ext=webm]"
            else -> ""
        }
        val audioExtFilter = when (normalizedContainer) {
            "mp4", "mov" -> "[ext=m4a]"
            "webm" -> "[ext=webm]"
            else -> ""
        }

        return if (videoOnly) {
            "bestvideo$heightFilter$videoExtFilter/bestvideo$heightFilter/bestvideo/best$heightFilter/best"
        } else {
            "bestvideo$heightFilter$videoExtFilter+bestaudio$audioExtFilter/bestvideo$heightFilter+bestaudio/best$heightFilter/best"
        }
    }

    private fun buildExtractorArgs(
        clientSpec: String,
        includePlayerSkip: Boolean = false,
        poToken: String? = null,
        dataSyncId: String? = null,
    ): String {
        val parts = mutableListOf("player_client=$clientSpec")
        if (includePlayerSkip) {
            parts += "player_skip=webpage,configs"
        }
        if (!dataSyncId.isNullOrBlank()) {
            parts += "data_sync_id=${dataSyncId.trim()}"
        }

        val poTokenEntries = buildPoTokenEntries(
            clientSpec = clientSpec,
            poToken = poToken,
        )
        if (poTokenEntries.isNotEmpty()) {
            parts += "po_token=${poTokenEntries.joinToString(",")}"
        }

        return "youtube:${parts.joinToString(";")}"
    }

    private fun buildAuthenticatedSameSelectorAttempts(
        requestedSelector: String,
        poToken: String?,
        dataSyncId: String? = null,
    ): List<Attempt> {
        val normalizedRequestedSelector = requestedSelector.trim().ifBlank { "bestvideo*+bestaudio/best" }
        return listOf(
            Attempt(
                label = "combined authenticated requested format",
                selector = normalizedRequestedSelector,
                extractorArgs = buildExtractorArgs(
                    clientSpec = "default,mweb,web,android,tv",
                    includePlayerSkip = true,
                    poToken = poToken,
                    dataSyncId = dataSyncId,
                ),
            ),
            Attempt(
                label = "mweb authenticated requested format",
                selector = normalizedRequestedSelector,
                extractorArgs = buildExtractorArgs(
                    clientSpec = "default,mweb",
                    includePlayerSkip = true,
                    poToken = poToken,
                    dataSyncId = dataSyncId,
                ),
            ),
            Attempt(
                label = "web authenticated requested format",
                selector = normalizedRequestedSelector,
                extractorArgs = buildExtractorArgs(
                    clientSpec = "default,web",
                    includePlayerSkip = true,
                    poToken = poToken,
                    dataSyncId = dataSyncId,
                ),
            ),
            Attempt(
                label = "android authenticated requested format",
                selector = normalizedRequestedSelector,
                extractorArgs = buildExtractorArgs(
                    clientSpec = "default,android",
                    poToken = poToken,
                    dataSyncId = dataSyncId,
                ),
            ),
        )
    }

    private fun preferredClientSpecForHint(preferredHint: String): String {
        return when (preferredHint.trim().lowercase()) {
            "mweb.gvs" -> "default,mweb"
            "android.gvs" -> "default,android"
            else -> "default,web"
        }
    }

    private fun buildPoTokenEntries(
        clientSpec: String,
        poToken: String?,
    ): List<String> {
        val trimmedToken = poToken.orEmpty().trim()
        if (trimmedToken.isBlank()) return emptyList()

        val rawEntries = trimmedToken.split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
        val looksPreformatted = rawEntries.all { entry ->
            entry.contains('.') && entry.contains('+')
        }
        if (looksPreformatted) {
            return rawEntries
        }

        val poTokenPrefix = when {
            clientSpec.contains("mweb") -> "mweb.gvs+"
            clientSpec.contains("web") -> "web.gvs+"
            clientSpec.contains("android") -> "android.gvs+"
            else -> "web.gvs+"
        }
        return listOf("$poTokenPrefix$trimmedToken")
    }

    private fun prioritizeByHint(attempts: List<Attempt>, preferredHint: String): List<Attempt> {
        val normalizedHint = preferredHint.trim().lowercase()
        return attempts.sortedByDescending { attempt ->
            when {
                normalizedHint == "mweb.gvs" && attempt.extractorArgs?.contains("mweb") == true -> 1
                normalizedHint == "web.gvs" && attempt.extractorArgs?.contains("default,web") == true -> 1
                normalizedHint == "android.gvs" && attempt.extractorArgs?.contains("android") == true -> 1
                else -> 0
            }
        }
    }

    private fun normalizeExtractorArgs(extractorArgs: String?): String? {
        return extractorArgs?.trim()?.ifBlank { null }
    }
}
