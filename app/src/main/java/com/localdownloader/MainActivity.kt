package com.localdownloader

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.localdownloader.ui.DownloaderApp
import com.localdownloader.ui.theme.LocalDownloaderTheme
import com.localdownloader.utils.Logger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var logger: Logger

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

    private var darkTheme by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logger.i("MainActivity", "onCreate")
        requestNotificationPermissionIfNeeded()
        requestStoragePermissionIfNeeded()
        setContent {
            LocalDownloaderTheme(darkTheme = darkTheme) {
                DownloaderApp(
                    onDarkThemeChanged = { enabled ->
                        darkTheme = enabled
                        logger.i("MainActivity", "Dark theme changed to $enabled")
                        // Force recreation of activity for theme change
                        recreate()
                    },
                )
            }
        }
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
}
