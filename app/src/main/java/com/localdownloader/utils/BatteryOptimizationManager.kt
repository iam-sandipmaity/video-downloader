package com.localdownloader.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.localdownloader.domain.models.BatterySaverMode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class BatterySnapshot(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val isSystemPowerSaveMode: Boolean,
    val isIgnoringBatteryOptimizations: Boolean,
)

@Singleton
class BatteryOptimizationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val powerManager: PowerManager?
        get() = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    /**
     * Checks if the app is exempted from Android OS Doze mode and App Standby optimizations.
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = powerManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching { pm.isIgnoringBatteryOptimizations(context.packageName) }.getOrDefault(false)
        } else {
            true
        }
    }

    /**
     * Checks if the Android system-wide Battery Saver mode is currently turned on.
     */
    fun isSystemPowerSaveMode(): Boolean {
        val pm = powerManager ?: return false
        return runCatching { pm.isPowerSaveMode }.getOrDefault(false)
    }

    /**
     * Obtains the current battery level percentage (0-100) and charging status.
     */
    fun getBatterySnapshot(): BatterySnapshot {
        val batteryIntent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val percent = if (level >= 0 && scale > 0) {
            ((level.toFloat() / scale.toFloat()) * 100f).toInt().coerceIn(0, 100)
        } else {
            100
        }

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        return BatterySnapshot(
            batteryPercent = percent,
            isCharging = isCharging,
            isSystemPowerSaveMode = isSystemPowerSaveMode(),
            isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations(),
        )
    }

    /**
     * Determines whether in-app Battery Saver / Eco limits are currently active.
     */
    fun isBatterySaverActive(
        mode: BatterySaverMode,
        lowBatteryThresholdPercent: Int = 15,
    ): Boolean {
        return when (mode) {
            BatterySaverMode.OFF -> false
            BatterySaverMode.ALWAYS_ON -> true
            BatterySaverMode.AUTO -> {
                val snapshot = getBatterySnapshot()
                snapshot.isSystemPowerSaveMode || (!snapshot.isCharging && snapshot.batteryPercent <= lowBatteryThresholdPercent)
            }
        }
    }

    /**
     * Resolves effective fragment download threads considering the current power state.
     * When Battery Saver is active, threads are throttled to 1 or 2.
     */
    fun resolveEffectiveConcurrentFragments(
        configuredThreads: Int,
        mode: BatterySaverMode,
        lowBatteryThresholdPercent: Int = 15,
    ): Int {
        val safeConfigured = configuredThreads.coerceIn(1, 16)
        return if (isBatterySaverActive(mode, lowBatteryThresholdPercent)) {
            minOf(2, safeConfigured)
        } else {
            safeConfigured
        }
    }

    /**
     * Resolves effective max concurrent downloads considering the current power state.
     * When Battery Saver is active, queue slots are throttled to 1.
     */
    fun resolveEffectiveConcurrentSlots(
        configuredSlots: Int,
        mode: BatterySaverMode,
        lowBatteryThresholdPercent: Int = 15,
    ): Int {
        val safeConfigured = configuredSlots.coerceIn(1, 4)
        return if (isBatterySaverActive(mode, lowBatteryThresholdPercent)) {
            1
        } else {
            safeConfigured
        }
    }

    /**
     * Returns an intent to prompt the user to ignore battery optimizations (direct dialog when restricted).
     */
    fun createIgnoreBatteryOptimizationsIntent(): Intent {
        val packageName = context.packageName
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !isIgnoringBatteryOptimizations()) {
            val directRequestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (directRequestIntent.resolveActivity(context.packageManager) != null) {
                return directRequestIntent
            }
        }

        return createAppBatterySettingsIntent()
    }

    /**
     * Returns an intent to open the system App Info / Battery screen so the user can change or revoke battery restrictions.
     */
    fun createAppBatterySettingsIntent(): Intent {
        val packageName = context.packageName
        val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (appDetailsIntent.resolveActivity(context.packageManager) != null) {
            return appDetailsIntent
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val listIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (listIntent.resolveActivity(context.packageManager) != null) {
                return listIntent
            }
        }

        return Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
