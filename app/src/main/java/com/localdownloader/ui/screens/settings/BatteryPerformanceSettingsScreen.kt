package com.localdownloader.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EnergySavingsLeaf
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.localdownloader.R
import com.localdownloader.domain.models.BatterySaverMode
import com.localdownloader.ui.components.PreferenceDivider
import com.localdownloader.ui.components.PreferenceGroup
import com.localdownloader.ui.components.PreferencePageScaffold
import com.localdownloader.ui.components.PreferenceRow
import com.localdownloader.ui.components.PreferenceSwitchRow
import com.localdownloader.utils.BatteryOptimizationManager
import com.localdownloader.viewmodel.FormatUiState

@Composable
fun BatteryPerformanceSettingsScreen(
    uiState: FormatUiState,
    batteryOptimizationManager: BatteryOptimizationManager,
    onBatterySaverModeChanged: (BatterySaverMode) -> Unit,
    onDefaultConcurrentFragmentsChanged: (Int) -> Unit,
    onMaxConcurrentDownloadsChanged: (Int) -> Unit,
    onDownloadOnlyWhileChargingChanged: (Boolean) -> Unit,
    onPauseDownloadsOnLowBatteryChanged: (Boolean) -> Unit,
    onLowBatteryThresholdPercentChanged: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isIgnoringOptimizations by remember {
        mutableStateOf(batteryOptimizationManager.isIgnoringBatteryOptimizations())
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isIgnoringOptimizations = batteryOptimizationManager.isIgnoringBatteryOptimizations()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var choiceDialog by remember { mutableStateOf<SettingChoiceDialogState?>(null) }

    val saverTitle = stringResource(R.string.battery_saver_mode_title)
    val threadsTitle = stringResource(R.string.battery_threads_title)
    val concurrentTitle = stringResource(R.string.download_defaults_concurrent_title)
    val lowThresholdTitle = stringResource(R.string.battery_low_threshold_title)

    val slotSubtitles = mapOf(
        1 to stringResource(R.string.download_defaults_slot_1),
        2 to stringResource(R.string.download_defaults_slot_2),
        3 to stringResource(R.string.download_defaults_slot_3),
        4 to stringResource(R.string.download_defaults_slot_4),
    )

    choiceDialog?.let { state ->
        SettingChoiceDialog(
            state = state,
            onDismiss = { choiceDialog = null },
        )
    }

    PreferencePageScaffold(
        title = stringResource(R.string.settings_battery_title),
        onBack = onBack,
        modifier = modifier,
    ) {
        // System OS Battery Optimization Card
        item {
            PreferenceGroup {
                if (isIgnoringOptimizations) {
                    PreferenceRow(
                        icon = Icons.Rounded.CheckCircle,
                        title = stringResource(R.string.battery_system_unrestricted_title),
                        value = stringResource(R.string.battery_system_status_optimized),
                        onClick = {
                            runCatching {
                                context.startActivity(batteryOptimizationManager.createAppBatterySettingsIntent())
                            }
                        },
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.size(40.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.BatteryAlert,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.battery_system_restricted_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = stringResource(R.string.battery_system_status_restricted),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.battery_system_restricted_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                runCatching {
                                    context.startActivity(batteryOptimizationManager.createIgnoreBatteryOptimizationsIntent())
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(text = stringResource(R.string.battery_system_restricted_action))
                        }
                    }
                }
            }
        }

        // In-App Battery Saver / Eco Mode
        item {
            PreferenceGroup {
                val currentMode = uiState.appSettings.batterySaverMode
                val autoTitle = stringResource(R.string.battery_saver_mode_auto)
                val autoDesc = stringResource(R.string.battery_saver_mode_auto_desc)
                val alwaysOnTitle = stringResource(R.string.battery_saver_mode_always_on)
                val alwaysOnDesc = stringResource(R.string.battery_saver_mode_always_on_desc)
                val offTitle = stringResource(R.string.battery_saver_mode_off)
                val offDesc = stringResource(R.string.battery_saver_mode_off_desc)

                val modeLabel = when (currentMode) {
                    BatterySaverMode.AUTO -> autoTitle
                    BatterySaverMode.ALWAYS_ON -> alwaysOnTitle
                    BatterySaverMode.OFF -> offTitle
                }
                PreferenceRow(
                    icon = Icons.Rounded.EnergySavingsLeaf,
                    title = saverTitle,
                    subtitle = modeLabel,
                    onClick = {
                        val modeChoices = listOf(
                            SettingChoiceOption(
                                title = autoTitle,
                                subtitle = autoDesc,
                                onSelect = { onBatterySaverModeChanged(BatterySaverMode.AUTO) },
                            ),
                            SettingChoiceOption(
                                title = alwaysOnTitle,
                                subtitle = alwaysOnDesc,
                                onSelect = { onBatterySaverModeChanged(BatterySaverMode.ALWAYS_ON) },
                            ),
                            SettingChoiceOption(
                                title = offTitle,
                                subtitle = offDesc,
                                onSelect = { onBatterySaverModeChanged(BatterySaverMode.OFF) },
                            ),
                        )
                        choiceDialog = SettingChoiceDialogState(
                            title = saverTitle,
                            selected = modeLabel,
                            options = modeChoices,
                        )
                    },
                )
            }
        }

        // Concurrency & Threads Tuning
        item {
            PreferenceGroup {
                val currentThreads = uiState.appSettings.defaultConcurrentFragments
                val threadOptions = listOf(1, 2, 4, 8, 16)
                val threadOptionLabels = mapOf(
                    1 to stringResource(R.string.battery_threads_1),
                    2 to stringResource(R.string.battery_threads_2),
                    4 to stringResource(R.string.battery_threads_4),
                    8 to stringResource(R.string.battery_threads_8),
                    16 to stringResource(R.string.battery_threads_16),
                )
                val threadOptionSubtitles = mapOf(
                    1 to stringResource(R.string.battery_threads_1_desc),
                    2 to stringResource(R.string.battery_threads_2_desc),
                    4 to stringResource(R.string.battery_threads_4_desc),
                    8 to stringResource(R.string.battery_threads_8_desc),
                    16 to stringResource(R.string.battery_threads_16_desc),
                )

                PreferenceRow(
                    icon = Icons.Rounded.Speed,
                    title = threadsTitle,
                    subtitle = stringResource(R.string.battery_threads_count, currentThreads),
                    onClick = {
                        val choices = threadOptions.map { count ->
                            SettingChoiceOption(
                                title = threadOptionLabels[count] ?: "$count threads",
                                subtitle = threadOptionSubtitles[count],
                                onSelect = { onDefaultConcurrentFragmentsChanged(count) },
                            )
                        }
                        choiceDialog = SettingChoiceDialogState(
                            title = threadsTitle,
                            selected = threadOptionLabels[currentThreads] ?: "$currentThreads threads",
                            options = choices,
                        )
                    },
                )
                PreferenceDivider()
                PreferenceRow(
                    icon = Icons.Rounded.Queue,
                    title = concurrentTitle,
                    subtitle = "${uiState.maxConcurrentDownloads} slots",
                    onClick = {
                        val slotChoices = (1..4).map { slotCount ->
                            SettingChoiceOption(
                                title = slotCount.toString(),
                                subtitle = slotSubtitles.getValue(slotCount),
                                onSelect = { onMaxConcurrentDownloadsChanged(slotCount) },
                            )
                        }
                        choiceDialog = SettingChoiceDialogState(
                            title = concurrentTitle,
                            selected = uiState.maxConcurrentDownloads.toString(),
                            options = slotChoices,
                        )
                    },
                )
            }
        }

        // Power Rules
        item {
            PreferenceGroup {
                PreferenceSwitchRow(
                    icon = Icons.Rounded.Power,
                    title = stringResource(R.string.battery_charging_only_title),
                    subtitle = stringResource(R.string.battery_charging_only_subtitle),
                    checked = uiState.appSettings.downloadOnlyWhileCharging,
                    onCheckedChange = onDownloadOnlyWhileChargingChanged,
                )
                PreferenceDivider()
                PreferenceSwitchRow(
                    icon = Icons.Rounded.BatteryAlert,
                    title = stringResource(R.string.battery_pause_low_title),
                    checked = uiState.appSettings.pauseDownloadsOnLowBattery,
                    onCheckedChange = onPauseDownloadsOnLowBatteryChanged,
                )
                if (uiState.appSettings.pauseDownloadsOnLowBattery) {
                    PreferenceDivider()
                    val thresholdOptions = listOf(10, 15, 20, 25)
                    val currentThreshold = uiState.appSettings.lowBatteryThresholdPercent
                    PreferenceRow(
                        icon = Icons.Rounded.Tune,
                        title = lowThresholdTitle,
                        subtitle = "$currentThreshold%",
                        onClick = {
                            val thresholdChoices = thresholdOptions.map { pct ->
                                SettingChoiceOption(
                                    title = "$pct%",
                                    subtitle = if (pct == 15) "Recommended" else null,
                                    onSelect = { onLowBatteryThresholdPercentChanged(pct) },
                                )
                            }
                            choiceDialog = SettingChoiceDialogState(
                                title = lowThresholdTitle,
                                selected = "$currentThreshold%",
                                options = thresholdChoices,
                            )
                        },
                    )
                }
            }
        }
    }
}
