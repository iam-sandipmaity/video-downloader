package com.localdownloader.updates

import com.localdownloader.BuildConfig
import kotlinx.serialization.Serializable
import java.math.BigInteger

enum class YtDlpReleaseChannel(
    val id: String,
    val title: String,
    val description: String,
    val apiUrl: String,
) {
    STABLE(
        id = "stable",
        title = "Stable Version of yt-dlp",
        description = "Recommended release channel with the latest stable fixes.",
        apiUrl = "https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest",
    ),
    NIGHTLY(
        id = "nightly",
        title = "Nightly Version of yt-dlp",
        description = "More frequent builds with newer extractor fixes.",
        apiUrl = "https://api.github.com/repos/yt-dlp/yt-dlp-nightly-builds/releases/latest",
    ),
    MASTER(
        id = "master",
        title = "Master Version of yt-dlp",
        description = "Bleeding-edge builds from the master branch.",
        apiUrl = "https://api.github.com/repos/yt-dlp/yt-dlp-master-builds/releases/latest",
    ),
    ;

    companion object {
        fun fromId(raw: String?): YtDlpReleaseChannel {
            return entries.firstOrNull { it.id.equals(raw, ignoreCase = true) } ?: STABLE
        }
    }
}

data class UpdatePreferences(
    val includePrereleaseAppReleases: Boolean = false,
    val autoUpdateYtDlp: Boolean = BuildConfig.YTDLP_AUTO_UPDATE_DEFAULT,
    val ytDlpChannel: YtDlpReleaseChannel = YtDlpReleaseChannel.STABLE,
)

@Serializable
data class GitHubReleaseDto(
    val tag_name: String,
    val name: String? = null,
    val body: String? = null,
    val prerelease: Boolean = false,
    val published_at: String? = null,
    val html_url: String? = null,
    val assets: List<GitHubAssetDto> = emptyList(),
)

@Serializable
data class GitHubAssetDto(
    val name: String,
    val size: Long = 0L,
    val browser_download_url: String,
    val content_type: String? = null,
)

data class ComponentUpdateCheck(
    val currentVersion: String?,
    val latestVersion: String?,
    val updateAvailable: Boolean,
    val requiresInitialInstall: Boolean = false,
    val summary: String,
    val releaseNotes: String? = null,
    val releasePageUrl: String? = null,
    val downloadUrl: String? = null,
    val assetName: String? = null,
)

data class PreparedAppUpdate(
    val apkPath: String,
    val requiresInstallPermission: Boolean,
    val assetName: String,
)

data class RuntimeInstallResult(
    val updated: Boolean,
    val version: String?,
    val message: String,
)

internal fun normalizeReleaseVersion(raw: String?): String? {
    return raw?.trim()
        ?.removePrefix("v")
        ?.takeIf { it.isNotBlank() }
}

internal fun compareLooseVersions(left: String?, right: String?): Int {
    val leftTokens = versionTokens(left)
    val rightTokens = versionTokens(right)
    val maxSize = maxOf(leftTokens.size, rightTokens.size)
    for (index in 0 until maxSize) {
        val leftToken = leftTokens.getOrNull(index)
        val rightToken = rightTokens.getOrNull(index)
        if (leftToken == null && rightToken == null) return 0
        if (leftToken == null) {
            return if (rightToken?.isPreReleaseMarker == true) 1 else -1
        }
        if (rightToken == null) {
            return if (leftToken.isPreReleaseMarker) -1 else 1
        }
        val diff = leftToken.compareTo(rightToken)
        if (diff != 0) return diff
    }
    return 0
}

private fun versionTokens(raw: String?): List<VersionToken> {
    val normalized = normalizeReleaseVersion(raw).orEmpty()
    if (normalized.isBlank()) return emptyList()
    return VERSION_TOKEN_REGEX.findAll(normalized).map { match ->
        val value = match.value
        value.toBigIntegerOrNull()?.let { number ->
            VersionToken.Number(number)
        } ?: VersionToken.Text(value.lowercase())
    }.toList()
}

private sealed interface VersionToken {
    val isPreReleaseMarker: Boolean

    fun compareTo(other: VersionToken): Int

    data class Number(private val value: BigInteger) : VersionToken {
        override val isPreReleaseMarker: Boolean = false

        override fun compareTo(other: VersionToken): Int {
            return when (other) {
                is Number -> value.compareTo(other.value)
                is Text -> 1
            }
        }
    }

    data class Text(private val value: String) : VersionToken {
        override val isPreReleaseMarker: Boolean
            get() = value in PRE_RELEASE_MARKERS

        override fun compareTo(other: VersionToken): Int {
            return when (other) {
                is Number -> -1
                is Text -> markerRank(value).compareTo(markerRank(other.value))
                    .takeIf { it != 0 }
                    ?: value.compareTo(other.value)
            }
        }
    }
}

private fun markerRank(value: String): Int {
    return when (value) {
        "dev", "snapshot" -> 0
        "alpha", "a" -> 1
        "beta", "b" -> 2
        "rc" -> 3
        else -> 10
    }
}

private val VERSION_TOKEN_REGEX = Regex("[0-9]+|[A-Za-z]+")
private val PRE_RELEASE_MARKERS = setOf("dev", "snapshot", "alpha", "a", "beta", "b", "rc")
