package com.localdownloader.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.localdownloader.AppLaunchRouter
import com.localdownloader.media.isLikelyPlayableMediaPath
import java.io.File

object AppNotifications {
    const val CHANNEL_ACTIVE_DOWNLOADS = "downloads.active"
    const val CHANNEL_COMPLETED_DOWNLOADS = "downloads.completed"
    const val CHANNEL_DOWNLOAD_ERRORS = "downloads.errors"
    const val CHANNEL_CANCELED_DOWNLOADS = "downloads.canceled"
    const val CHANNEL_PROMOTIONS = "general.promotions"
    const val CHANNEL_AUDIO_PLAYBACK = "audio.playback"

    private const val CHANNEL_GROUP_DOWNLOADS = "group.downloads"
    private const val CHANNEL_GROUP_GENERAL = "group.general"

    private const val NOTIFICATION_GROUP_ACTIVE = "notifications.downloads.active"
    private const val NOTIFICATION_GROUP_COMPLETED = "notifications.downloads.completed"
    private const val NOTIFICATION_GROUP_ERRORS = "notifications.downloads.errors"
    private const val NOTIFICATION_GROUP_CANCELED = "notifications.downloads.canceled"

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        manager.createNotificationChannelGroup(
            NotificationChannelGroup(CHANNEL_GROUP_DOWNLOADS, "Downloads"),
        )
        manager.createNotificationChannelGroup(
            NotificationChannelGroup(CHANNEL_GROUP_GENERAL, "General"),
        )

        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ACTIVE_DOWNLOADS,
                "Active downloads",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Live progress for each active download."
                group = CHANNEL_GROUP_DOWNLOADS
                setShowBadge(false)
                enableVibration(false)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_COMPLETED_DOWNLOADS,
                "Completed downloads",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "A notification for each finished download."
                group = CHANNEL_GROUP_DOWNLOADS
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_DOWNLOAD_ERRORS,
                "Download errors",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Problems that need your attention."
                group = CHANNEL_GROUP_DOWNLOADS
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CANCELED_DOWNLOADS,
                "Canceled downloads",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Download cancellation updates."
                group = CHANNEL_GROUP_DOWNLOADS
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_PROMOTIONS,
                "Promotions and updates",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Optional promotional and product update messages."
                group = CHANNEL_GROUP_GENERAL
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_AUDIO_PLAYBACK,
                "Audio playback",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Background playback controls for downloaded audio."
                group = CHANNEL_GROUP_GENERAL
                setShowBadge(false)
            },
        )
    }

    fun canPostUserNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun progressNotificationId(taskId: String): Int {
        return stableNotificationId(taskId = taskId, bucket = 20_000)
    }

    fun terminalNotificationId(taskId: String): Int {
        return stableNotificationId(taskId = taskId, bucket = 220_000)
    }

    fun audioPlaybackNotificationId(): Int = 910_001

    private fun stableNotificationId(taskId: String, bucket: Int): Int {
        return bucket + ((taskId.hashCode() and Int.MAX_VALUE) % 90_000_000)
    }

    fun buildActiveDownloadNotification(
        context: Context,
        taskId: String,
        title: String,
        progress: Int,
        downloadedStr: String? = null,
        totalStr: String? = null,
        speed: String? = null,
        eta: String? = null,
    ): Notification {
        ensureChannels(context)
        val safeProgress = progress.coerceIn(0, 100)
        return NotificationCompat.Builder(context, CHANNEL_ACTIVE_DOWNLOADS)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title.ifBlank { "Downloading media" })
            .setContentText(
                buildProgressLine(
                    progress = safeProgress,
                    downloadedStr = downloadedStr,
                    totalStr = totalStr,
                    speed = speed,
                    eta = eta,
                ),
            )
            .setSubText(if (safeProgress > 0) "$safeProgress% complete" else "Preparing download")
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, safeProgress, safeProgress <= 0)
            .setContentIntent(
                buildRoutePendingIntent(
                    context = context,
                    route = AppLaunchRouter.ROUTE_DOWNLOAD_QUEUE,
                    taskId = taskId,
                    requestCode = requestCodeFor(taskId, AppLaunchRouter.ROUTE_DOWNLOAD_QUEUE),
                ),
            )
            .setGroup(NOTIFICATION_GROUP_ACTIVE)
            .build()
    }

    fun showDownloadCompleted(
        context: Context,
        taskId: String,
        title: String,
        outputPath: String?,
        sizeLabel: String?,
    ) {
        if (!canPostUserNotifications(context)) return
        ensureChannels(context)

        val notificationId = terminalNotificationId(taskId)
        val contentIntent = buildRoutePendingIntent(
            context = context,
            route = AppLaunchRouter.ROUTE_DOWNLOADS,
            taskId = taskId,
            requestCode = requestCodeFor(taskId, AppLaunchRouter.ROUTE_DOWNLOADS),
        )
        val message = buildCompletionLine(outputPath = outputPath, sizeLabel = sizeLabel)
        val builder = NotificationCompat.Builder(context, CHANNEL_COMPLETED_DOWNLOADS)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title.ifBlank { "Download complete" })
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSubText("Completed")
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setGroup(NOTIFICATION_GROUP_COMPLETED)

        if (isPlayableMediaPath(outputPath)) {
            builder.addAction(
                android.R.drawable.ic_media_play,
                "Open",
                buildRoutePendingIntent(
                    context = context,
                    route = AppLaunchRouter.ROUTE_PLAYER,
                    taskId = taskId,
                    requestCode = requestCodeFor(taskId, AppLaunchRouter.ROUTE_PLAYER),
                ),
            )
        }

        notify(context, notificationId, builder.build())
    }

    fun showDownloadFailed(
        context: Context,
        taskId: String,
        title: String,
        errorMessage: String,
    ) {
        if (!canPostUserNotifications(context)) return
        ensureChannels(context)

        val message = errorMessage.trim().ifBlank { "Download failed." }
        notify(
            context = context,
            notificationId = terminalNotificationId(taskId),
            notification = NotificationCompat.Builder(context, CHANNEL_DOWNLOAD_ERRORS)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(title.ifBlank { "Download failed" })
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setSubText("Error")
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(
                    buildRoutePendingIntent(
                        context = context,
                        route = AppLaunchRouter.ROUTE_DOWNLOAD_QUEUE,
                        taskId = taskId,
                        requestCode = requestCodeFor(taskId, "error"),
                    ),
                )
                .setGroup(NOTIFICATION_GROUP_ERRORS)
                .build(),
        )
    }

    fun showDownloadCanceled(
        context: Context,
        taskId: String,
        title: String,
    ) {
        if (!canPostUserNotifications(context)) return
        ensureChannels(context)

        notify(
            context = context,
            notificationId = terminalNotificationId(taskId),
            notification = NotificationCompat.Builder(context, CHANNEL_CANCELED_DOWNLOADS)
                .setSmallIcon(android.R.drawable.ic_menu_close_clear_cancel)
                .setContentTitle(title.ifBlank { "Download canceled" })
                .setContentText("Canceled by you. Tap to review the queue.")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Canceled by you. Tap to review the queue."),
                )
                .setSubText("Canceled")
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(
                    buildRoutePendingIntent(
                        context = context,
                        route = AppLaunchRouter.ROUTE_DOWNLOAD_QUEUE,
                        taskId = taskId,
                        requestCode = requestCodeFor(taskId, "canceled"),
                    ),
                )
                .setGroup(NOTIFICATION_GROUP_CANCELED)
                .build(),
        )
    }

    private fun notify(
        context: Context,
        notificationId: Int,
        notification: Notification,
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    private fun buildRoutePendingIntent(
        context: Context,
        route: String,
        taskId: String?,
        requestCode: Int,
    ): PendingIntent {
        return PendingIntent.getActivity(
            context,
            requestCode,
            AppLaunchRouter.buildIntent(context = context, route = route, taskId = taskId),
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentImmutableFlag(),
        )
    }

    private fun pendingIntentImmutableFlag(): Int {
        return PendingIntent.FLAG_IMMUTABLE
    }

    private fun requestCodeFor(taskId: String, route: String): Int {
        return (progressNotificationId(taskId) xor route.hashCode()) and Int.MAX_VALUE
    }

    private fun buildProgressLine(
        progress: Int,
        downloadedStr: String?,
        totalStr: String?,
        speed: String?,
        eta: String?,
    ): String {
        val parts = buildList {
            if (!downloadedStr.isNullOrBlank() && !totalStr.isNullOrBlank()) {
                add("$downloadedStr / $totalStr")
            } else if (progress > 0) {
                add("$progress%")
            }
            if (!speed.isNullOrBlank()) add(speed)
            if (!eta.isNullOrBlank()) add("ETA $eta")
        }
        return parts.joinToString(" | ").ifBlank { "Preparing download" }
    }

    private fun buildCompletionLine(
        outputPath: String?,
        sizeLabel: String?,
    ): String {
        val parts = buildList {
            sizeLabel?.takeIf { it.isNotBlank() }?.let(::add)
            outputPath?.takeIf { it.isNotBlank() }?.let { add("Saved as ${File(it).name}") }
            add("Tap to view it in the app")
        }
        return parts.joinToString(" | ")
    }

    private fun isPlayableMediaPath(path: String?): Boolean {
        return isLikelyPlayableMediaPath(path)
    }
}
