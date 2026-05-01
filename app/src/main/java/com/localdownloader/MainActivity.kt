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
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.localdownloader.ui.DownloaderApp
import com.localdownloader.ui.model.ExternalOpenRequest
import com.localdownloader.ui.theme.LocalDownloaderTheme
import com.localdownloader.utils.FileUtils
import com.localdownloader.utils.Logger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var fileUtils: FileUtils

    private var darkTheme by mutableStateOf(false)
    private var externalOpenRequest by mutableStateOf<ExternalOpenRequest?>(null)
    private var sharedUrlRequest by mutableStateOf<String?>(null)
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
        externalOpenRequest = buildExternalOpenRequest(intent)
        sharedUrlRequest = buildSharedUrlRequest(intent)
        requestNotificationPermissionIfNeeded()
        requestStoragePermissionIfNeeded()
        setContent {
            LocalDownloaderTheme(darkTheme = darkTheme) {
                DownloaderApp(
                    externalOpenRequest = externalOpenRequest,
                    onExternalOpenHandled = { externalOpenRequest = null },
                    sharedUrlRequest = sharedUrlRequest,
                    onSharedUrlHandled = { sharedUrlRequest = null },
                    onDarkThemeChanged = { enabled ->
                        darkTheme = enabled
                        logger.i("MainActivity", "Dark theme changed to $enabled")
                    },
                    onDarkThemeUpdated = { enabled ->
                        darkTheme = enabled
                        if (darkTheme) logger.i("MainActivity", "Applied persisted dark theme")
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
    }

    override fun onStart() {
        super.onStart()
        logger.i("MainActivity", "onStart")
    }

    override fun onResume() {
        super.onResume()
        logger.i("MainActivity", "onResume")
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
}
