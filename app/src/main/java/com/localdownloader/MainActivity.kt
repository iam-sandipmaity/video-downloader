package com.localdownloader

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.core.os.LocaleListCompat
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.localdownloader.domain.models.AccentPreset
import com.localdownloader.domain.models.AppSettings
import com.localdownloader.domain.models.ContrastMode
import com.localdownloader.domain.models.SYSTEM_LANGUAGE_TAG
import com.localdownloader.domain.models.ThemeMode
import com.localdownloader.data.SettingsStore
import com.localdownloader.notifications.AppNotifications
import com.localdownloader.ui.DownloaderApp
import com.localdownloader.ui.model.ExternalOpenRequest
import com.localdownloader.ui.theme.LocalDownloaderTheme
import com.localdownloader.utils.FileUtils
import com.localdownloader.utils.Logger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint(ComponentActivity::class)
class MainActivity : Hilt_MainActivity() {
    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var fileUtils: FileUtils

    @Inject
    lateinit var settingsStore: SettingsStore

    private var themeMode by mutableStateOf(ThemeMode.SYSTEM)
    private var accentPreset by mutableStateOf(AccentPreset.AMBER)
    private var contrastMode by mutableStateOf(ContrastMode.STANDARD)
    private var externalOpenRequest by mutableStateOf<ExternalOpenRequest?>(null)
    private var sharedUrlRequest by mutableStateOf<String?>(null)
    private var notificationOpenRequest by mutableStateOf<AppOpenRequest?>(null)
    private var pictureInPictureAllowed by mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        logger.i("MainActivity", "Notification permission result granted=$granted")
    }

    // WRITE_EXTERNAL_STORAGE is only required on Android 8 and 9 (API 26–28).
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        logger.i("MainActivity", "Storage permission result granted=$granted")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logger.i("MainActivity", "onCreate")
        AppNotifications.ensureChannels(this)
        val initialSettings = runCatching {
            runBlocking { settingsStore.observeSettings().first() }
        }.getOrDefault(AppSettings())
        initializeAppLanguage(initialSettings)
        themeMode = initialSettings.themeMode
        accentPreset = initialSettings.accentPreset
        contrastMode = initialSettings.contrastMode
        externalOpenRequest = buildExternalOpenRequest(intent)
        sharedUrlRequest = buildSharedUrlRequest(intent)
        notificationOpenRequest = AppLaunchRouter.fromIntent(intent)
        requestNotificationPermissionIfNeeded()
        requestStoragePermissionIfNeeded()
        setContent {
            LocalDownloaderTheme(
                themeMode = themeMode,
                accentPreset = accentPreset,
                contrastMode = contrastMode,
            ) {
                DownloaderApp(
                    externalOpenRequest = externalOpenRequest,
                    onExternalOpenHandled = { externalOpenRequest = null },
                    sharedUrlRequest = sharedUrlRequest,
                    onSharedUrlHandled = { sharedUrlRequest = null },
                    notificationOpenRequest = notificationOpenRequest,
                    onNotificationOpenHandled = { notificationOpenRequest = null },
                    onAppearanceUpdated = { mode, accent, contrast ->
                        themeMode = mode
                        accentPreset = accent
                        contrastMode = contrast
                    },
                    onLanguageUpdated = { languageTag ->
                        applyAppLanguage(languageTag)
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalOpenRequest = buildExternalOpenRequest(intent)
        sharedUrlRequest = buildSharedUrlRequest(intent)
        notificationOpenRequest = AppLaunchRouter.fromIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        logger.i("MainActivity", "onStart")
    }

    override fun onResume() {
        super.onResume()
        logger.i("MainActivity", "onResume")
        syncLanguageSettingFromPlatform()
    }

    override fun onPause() {
        logger.i("MainActivity", "onPause")
        super.onPause()
    }

    override fun onStop() {
        logger.i("MainActivity", "onStop")
        super.onStop()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPictureInPictureIfPossible()
    }

    fun updatePictureInPictureAllowed(enabled: Boolean) {
        pictureInPictureAllowed = enabled
    }

    fun enterPictureInPictureIfPossible() {
        if (!pictureInPictureAllowed || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        runCatching {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build(),
            )
        }.onFailure { error ->
            logger.e("MainActivity", "Unable to enter PiP", error)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            logger.i("MainActivity", "Notification permission not required on this API level")
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            logger.i("MainActivity", "Notification permission already granted")
            return
        }
        logger.i("MainActivity", "Requesting notification permission")
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun requestStoragePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ handles external storage without this permission.
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            logger.i("MainActivity", "Storage permission already granted")
            return
        }
        logger.i("MainActivity", "Requesting storage permission for Android 8/9")
        storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private fun buildExternalOpenRequest(intent: Intent?): ExternalOpenRequest? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        val data = intent.data ?: return null
        if (data.scheme.equals("http", ignoreCase = true) || data.scheme.equals("https", ignoreCase = true)) {
            return null
        }
        return runCatching {
            fileUtils.importSharedOpenRequest(data, intent.type)
        }.onFailure { error ->
            logger.e("MainActivity", "Failed to handle external open intent", error)
        }.getOrNull()
    }

    private fun buildSharedUrlRequest(intent: Intent?): String? {
        intent ?: return null
        return when (intent.action) {
            Intent.ACTION_SEND -> extractFirstHttpUrl(
                intent.getStringExtra(Intent.EXTRA_TEXT)
                    ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
                    ?: intent.dataString,
            )

            Intent.ACTION_VIEW -> {
                val dataString = intent.dataString
                if (dataString?.startsWith("http://", ignoreCase = true) == true ||
                    dataString?.startsWith("https://", ignoreCase = true) == true
                ) {
                    dataString
                } else {
                    null
                }
            }

            else -> null
        }
    }

    private fun extractFirstHttpUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val matcher = Patterns.WEB_URL.matcher(text)
        while (matcher.find()) {
            val candidate = matcher.group()?.trim()?.trimEnd('.', ',', ';', ')', ']') ?: continue
            if (candidate.startsWith("http://", ignoreCase = true) ||
                candidate.startsWith("https://", ignoreCase = true)
            ) {
                return candidate
            }
        }
        return null
    }

    private fun initializeAppLanguage(initialSettings: AppSettings) {
        val storedLanguageTag = sanitizeLanguageTag(initialSettings.languageTag)
        val platformLanguageTag = currentAppLanguageTag()
        val effectiveLanguageTag = when {
            platformLanguageTag != SYSTEM_LANGUAGE_TAG -> platformLanguageTag
            storedLanguageTag != SYSTEM_LANGUAGE_TAG -> storedLanguageTag
            else -> SYSTEM_LANGUAGE_TAG
        }
        applyAppLanguage(effectiveLanguageTag)
        if (effectiveLanguageTag != storedLanguageTag) {
            persistLanguageSetting(initialSettings, effectiveLanguageTag)
        }
    }

    private fun syncLanguageSettingFromPlatform() {
        val platformLanguageTag = currentAppLanguageTag()
        lifecycleScope.launch {
            val settings = runCatching { settingsStore.observeSettings().first() }
                .getOrDefault(AppSettings())
            val storedLanguageTag = sanitizeLanguageTag(settings.languageTag)
            if (platformLanguageTag != storedLanguageTag) {
                persistLanguageSetting(settings, platformLanguageTag)
            }
        }
    }

    private fun persistLanguageSetting(
        settings: AppSettings,
        languageTag: String,
    ) {
        lifecycleScope.launch {
            runCatching {
                settingsStore.updateSettings(
                    settings.copy(languageTag = sanitizeLanguageTag(languageTag)),
                )
            }.onFailure { error ->
                logger.e("MainActivity", "Unable to persist app language", error)
            }
        }
    }

    private fun applyAppLanguage(languageTag: String) {
        val normalizedTag = normalizeLocaleTag(languageTag)
        val currentTags = currentAppLanguageTags()
        if (currentTags == normalizedTag) {
            return
        }

        AppCompatDelegate.setApplicationLocales(
            if (normalizedTag.isBlank()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(normalizedTag)
            },
        )
    }

    private fun currentAppLanguageTags(): String {
        return normalizeLocaleTag(AppCompatDelegate.getApplicationLocales().toLanguageTags())
    }

    private fun currentAppLanguageTag(): String {
        val currentTags = currentAppLanguageTags()
        return if (currentTags.isBlank()) SYSTEM_LANGUAGE_TAG else currentTags
    }

    private fun sanitizeLanguageTag(languageTag: String): String {
        val normalized = normalizeLocaleTag(languageTag)
        return if (normalized.isBlank()) SYSTEM_LANGUAGE_TAG else normalized
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
}
