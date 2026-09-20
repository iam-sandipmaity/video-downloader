package com.localdownloader

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localdownloader.data.SettingsStore
import com.localdownloader.domain.models.AccentPreset
import com.localdownloader.domain.models.AppSettings
import com.localdownloader.domain.models.ContrastMode
import com.localdownloader.domain.models.SYSTEM_LANGUAGE_TAG
import com.localdownloader.domain.models.ThemeMode
import com.localdownloader.ui.screens.QuickDownloadScreen
import com.localdownloader.ui.theme.LocalDownloaderTheme
import com.localdownloader.utils.Logger
import com.localdownloader.viewmodel.QuickDownloadViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint(ComponentActivity::class)
class QuickDownloadActivity : Hilt_QuickDownloadActivity() {

    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var settingsStore: SettingsStore

    private val viewModel: QuickDownloadViewModel by viewModels()

    private var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    private var accentPreset by mutableStateOf(AccentPreset.AMBER)
    private var contrastMode by mutableStateOf(ContrastMode.STANDARD)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logger.i("QuickDownloadActivity", "onCreate")

        val initialSettings = runCatching {
            runBlocking { settingsStore.observeSettings().first() }
        }.getOrDefault(AppSettings())
        initializeAppLanguage(initialSettings)
        themeMode = initialSettings.themeMode
        accentPreset = initialSettings.accentPreset
        contrastMode = initialSettings.contrastMode

        val sharedUrl = extractSharedUrl(intent)
        if (sharedUrl.isNullOrBlank()) {
            Toast.makeText(this, "No valid URL received to download", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel.initUrl(sharedUrl)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.isDownloadSuccess) {
                if (uiState.isDownloadSuccess) {
                    Toast.makeText(this@QuickDownloadActivity, getString(R.string.quick_download_queued), Toast.LENGTH_SHORT).show()
                    finish()
                }
            }

            LocalDownloaderTheme(
                themeMode = themeMode,
                accentPreset = accentPreset,
                contrastMode = contrastMode,
            ) {
                QuickDownloadScreen(
                    uiState = uiState,
                    onDismiss = { finish() },
                    onStreamTypeChanged = viewModel::onStreamTypeChanged,
                    onTitleChanged = viewModel::onTitleChanged,
                    onShowTitleEditDialog = viewModel::onShowTitleEditDialog,
                    onToggleQuickSettings = viewModel::onToggleQuickSettings,
                    onVideoQualitySelected = viewModel::onVideoQualitySelected,
                    onAudioQualitySelected = viewModel::onAudioQualitySelected,
                    onVideoFormatSelected = viewModel::onVideoFormatSelected,
                    onAudioFormatSelected = viewModel::onAudioFormatSelected,
                    onThreadsChanged = viewModel::onThreadsChanged,
                    onDownloadClicked = viewModel::download,
                    onRetryClicked = viewModel::retry,
                    onTogglePlaylistItem = viewModel::togglePlaylistItemSelection,
                    onSelectAllPlaylistItems = viewModel::toggleSelectAllPlaylistItems,
                    onDismissMeteredNetworkDialog = viewModel::dismissMeteredNetworkDialog,
                    onAllowCellularAndDownload = viewModel::allowCellularAndDownload,
                    onDownloadWhenWifiAvailable = viewModel::downloadWhenWifiAvailable,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val sharedUrl = extractSharedUrl(intent)
        if (!sharedUrl.isNullOrBlank()) {
            viewModel.initUrl(sharedUrl)
        }
    }

    private fun extractSharedUrl(intent: Intent?): String? {
        intent ?: return null
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                    ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
                    ?: intent.dataString
                extractFirstHttpUrl(text)
            }
            Intent.ACTION_VIEW -> {
                normalizeSharedUrlCandidate(intent.dataString)
            }
            else -> null
        }
    }

    private fun extractFirstHttpUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val matcher = Patterns.WEB_URL.matcher(text)
        while (matcher.find()) {
            val candidate = matcher.group()?.trim()?.trimEnd('.', ',', ';', ')', ']') ?: continue
            normalizeSharedUrlCandidate(candidate)?.let { return it }
        }
        return null
    }

    private fun normalizeSharedUrlCandidate(raw: String?): String? {
        val candidate = raw?.trim().orEmpty()
        if (candidate.isBlank()) return null
        return when {
            candidate.startsWith("https://", ignoreCase = true) -> candidate
            candidate.startsWith("http://", ignoreCase = true) ->
                candidate.replaceFirst(HTTP_SCHEME_REGEX, "https://")
            else -> null
        }
    }

    private fun initializeAppLanguage(initialSettings: AppSettings) {
        val storedLanguageTag = sanitizeLanguageTag(initialSettings.languageTag)
        applyAppLanguage(storedLanguageTag)
    }

    private fun applyAppLanguage(languageTag: String) {
        val normalizedTag = normalizeLocaleTag(languageTag)
        val targetTags = targetResourceLanguageTags(normalizedTag)

        AppCompatDelegate.setApplicationLocales(
            if (normalizedTag.isBlank()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(normalizedTag)
            },
        )

        applyResourcesLanguage(targetTags)
    }

    private fun sanitizeLanguageTag(languageTag: String): String {
        val normalized = normalizeLocaleTag(languageTag)
        return if (normalized.isBlank()) SYSTEM_LANGUAGE_TAG else normalized
    }

    private fun targetResourceLanguageTags(normalizedTag: String): String {
        return if (normalizedTag.isBlank()) {
            normalizeLocaleTag(LocaleListCompat.getAdjustedDefault().toLanguageTags())
        } else {
            normalizedTag
        }
    }

    private fun applyResourcesLanguage(targetTags: String) {
        val configuration = Configuration(resources.configuration)
        if (targetTags.isBlank()) return
        val locales = LocaleList.forLanguageTags(targetTags)
        LocaleList.setDefault(locales)
        configuration.setLocales(locales)
        resources.updateConfiguration(configuration, resources.displayMetrics)
        applicationContext.resources.updateConfiguration(configuration, applicationContext.resources.displayMetrics)
    }

    private fun normalizeLocaleTag(languageTag: String): String {
        val primaryTag = languageTag
            .split(',')
            .firstOrNull()
            ?.trim()
            .orEmpty()
        if (primaryTag.isBlank() || primaryTag == SYSTEM_LANGUAGE_TAG) {
            return ""
        }
        return LocaleListCompat.forLanguageTags(primaryTag)
            .get(0)
            ?.toLanguageTag()
            .orEmpty()
    }

    private companion object {
        private val HTTP_SCHEME_REGEX = Regex("^http://", RegexOption.IGNORE_CASE)
    }
}
